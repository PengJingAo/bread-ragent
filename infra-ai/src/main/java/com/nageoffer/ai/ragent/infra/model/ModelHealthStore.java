/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nageoffer.ai.ragent.infra.model;

import com.nageoffer.ai.ragent.infra.config.AIModelProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 模型健康状态存储器
 * <p>
 * 用于管理和跟踪各个 AI 模型的健康状况，实现断路器模式。
 * 调用方在请求模型前通过 {@link #acquireCall(String)} 获取调用凭证，
 * 调用成功后通过 {@link #markSuccess(CallPermit)} 恢复健康状态，
 * 调用失败后通过 {@link #markFailure(CallPermit)} 累计失败并触发熔断。
 * </p>
 */
@Component
@RequiredArgsConstructor
public class ModelHealthStore {

    /**
     * AI 模型配置，提供熔断阈值和熔断持续时间等参数。
     */
    private final AIModelProperties properties;

    /**
     * 按模型 ID 保存健康状态。
     * <p>
     * ConcurrentHashMap 适合多线程并发读写，避免多个请求同时更新模型状态时出现线程安全问题。
     * key 是模型 ID，value 是该模型当前的健康状态。
     * </p>
     */
    private final Map<String, ModelHealth> healthById = new ConcurrentHashMap<>();

    /**
     * 模型调用凭证。
     * <p>
     * 每次允许调用时都会生成一个递增序号。调用完成后必须带着同一个凭证回写结果，
     * 这样可以判断成功或失败是否已经落后于更新的调用结果，避免旧请求乱序返回后覆盖新状态。
     * </p>
     *
     * @param modelId 模型 ID
     * @param sequence 本次调用在该模型内的递增序号
     * @param allowed 是否允许本次调用
     */
    public record CallPermit(String modelId, long sequence, boolean allowed) {

        private static CallPermit denied(String modelId) {
            return new CallPermit(modelId, 0L, false);
        }

        private static CallPermit allowed(String modelId, long sequence) {
            return new CallPermit(modelId, sequence, true);
        }
    }

    /**
     * 判断模型当前是否不可用，主要用于模型候选列表筛选。
     *
     * @param id 模型 ID
     * @return true 表示当前应跳过该模型，false 表示可以作为候选模型
     */
    public boolean isUnavailable(String id) {
        ModelHealth health = healthById.get(id);
        if (health == null) {
            // 没有健康记录说明模型还没有失败过，默认认为可用。
            return false;
        }
        if (health.state == State.OPEN && health.openUntil > System.currentTimeMillis()) {
            // OPEN 表示熔断中，且当前时间还没到恢复时间，因此不可用。
            return true;
        }
        // HALF_OPEN 阶段只允许一个试探请求，已有试探请求时其他请求需要跳过。
        return health.state == State.HALF_OPEN && health.halfOpenInFlight;
    }

    /**
     * 获取本次调用凭证。
     * <p>
     * 该方法会根据模型当前的熔断状态做状态流转：
     * CLOSED 直接允许调用；OPEN 到期后转为 HALF_OPEN 并允许一次试探调用；
     * HALF_OPEN 只允许一个正在进行的试探调用。
     * </p>
     *
     * @param id 模型 ID
     * @return 调用凭证，allowed 为 true 表示允许本次调用
     */
    public CallPermit acquireCall(String id) {
        if (id == null) {
            return CallPermit.denied(null);
        }
        long now = System.currentTimeMillis();
        // compute 的返回值只能更新 Map，因此用 AtomicReference 把本次调用凭证带出 lambda。
        AtomicReference<CallPermit> permitRef = new AtomicReference<>(CallPermit.denied(id));
        // compute 会针对同一个 key 原子化更新状态，避免并发请求把熔断状态改乱。
        //哪里有试探请求？⬇
        //allowCall返回正确了，后续模型就能进行请求，这次请求就是一个试探请求，小心并发问题！！
        //todo：如何保证并发安全性，有可能同时两个请求到来，进行了两次试探请求，或者是前一次试探请求还没执行结束，
        // 没有来得及markFailure，第二次请求继续试探（实际上不需要），这算并发问题吗？

        //concurrentHashMap.compute()来防止并发
        healthById.compute(id, (k, v) -> {
            if (v == null) {
                // 第一次遇到该模型时创建默认健康状态，默认状态为 CLOSED。
                v = new ModelHealth();
            }
            if (v.state == State.OPEN) {
                if (v.openUntil > now) {
                    // 熔断时间还没结束，不允许调用，保持 OPEN 状态。
                    return v;
                }
                // 熔断时间结束后进入 HALF_OPEN，放行一个请求用于试探模型是否恢复。
                v.state = State.HALF_OPEN;
                v.halfOpenInFlight = true;
                permitRef.set(CallPermit.allowed(id, v.nextSequence()));
                return v;
            }
            if (v.state == State.HALF_OPEN) {
                if (v.halfOpenInFlight) {
                    // 已经有试探请求在执行，其他并发请求不再放行。
                    return v;
                }
                // HALF_OPEN 状态没有试探请求时，允许当前请求作为试探请求。
                v.halfOpenInFlight = true;
                permitRef.set(CallPermit.allowed(id, v.nextSequence()));
                return v;
            }
            // CLOSED 是正常状态，直接允许调用。
            permitRef.set(CallPermit.allowed(id, v.nextSequence()));
            return v;
        });
        return permitRef.get();
    }

    /**
     * 判断本次调用是否允许执行。
     * <p>
     * 兼容旧调用方式。新代码优先使用 {@link #acquireCall(String)} 获取凭证，
     * 并在完成后通过凭证回写调用结果。
     * 注解的作用是关闭“这个 boolean 方法总是被取反使用”的静态检查提示。
     * </p>
     *
     * @param id 模型 ID
     * @return true 表示允许本次调用，false 表示应跳过该模型
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean allowCall(String id) {
        return acquireCall(id).allowed();
    }

    /**
     * 标记模型调用成功。
     * <p>
     * 一旦调用成功，说明模型恢复健康，清空失败次数并回到 CLOSED 状态。
     * </p>
     *
     * @param id 模型 ID
     */
    public void markSuccess(String id) {
        if (id == null) {
            return;
        }
        healthById.compute(id, (k, v) -> {
            if (v == null) {
                // 没有历史记录时创建一个默认健康状态即可。
                return new ModelHealth();
            }
            v.state = State.CLOSED;
            v.consecutiveFailures = 0;
            v.openUntil = 0L;
            v.halfOpenInFlight = false;
            return v;
        });
    }

    /**
     * 根据调用凭证标记模型调用成功。
     * <p>
     * 如果该成功结果的调用序号早于或等于最新失败序号，说明它是乱序返回的旧结果，
     * 不能再把已经更新的失败状态重置为健康。
     * </p>
     *
     * @param permit 调用前获取的凭证
     */
    public void markSuccess(CallPermit permit) {
        if (permit == null || !permit.allowed()) {
            return;
        }
        healthById.compute(permit.modelId(), (k, v) -> {
            if (v == null) {
                return new ModelHealth();
            }
            if (permit.sequence() <= v.latestFailureSequence) {
                return v;
            }
            v.latestSuccessSequence = Math.max(v.latestSuccessSequence, permit.sequence());
            v.state = State.CLOSED;
            v.consecutiveFailures = 0;
            v.openUntil = 0L;
            v.halfOpenInFlight = false;
            return v;
        });
    }

    /**
     * 标记模型调用失败。
     * <p>
     * CLOSED 状态下失败会累计连续失败次数，达到阈值后进入 OPEN 熔断状态；
     * HALF_OPEN 状态下试探失败会立即重新进入 OPEN 状态。
     * </p>
     *
     * @param id 模型 ID
     */
    public void markFailure(String id) {
        if (id == null) {
            return;
        }
        long now = System.currentTimeMillis();
        healthById.compute(id, (k, v) -> {
            if (v == null) {
                // 第一次失败时创建健康状态，然后继续记录失败次数。
                v = new ModelHealth();
            }
            if (v.state == State.HALF_OPEN) {
                // 试探请求失败，说明模型还没恢复，重新进入 OPEN 熔断状态。
                v.state = State.OPEN;
                v.openUntil = now + properties.getSelection().getOpenDurationMs();
                v.consecutiveFailures = 0;
                v.halfOpenInFlight = false;
                return v;
            }
            v.consecutiveFailures++;
            if (v.consecutiveFailures >= properties.getSelection().getFailureThreshold()) {
                // 连续失败达到阈值，打开熔断器，并设置下一次允许试探的时间。
                v.state = State.OPEN;
                v.openUntil = now + properties.getSelection().getOpenDurationMs();
                v.consecutiveFailures = 0;
            }
            return v;
        });
    }

    /**
     * 根据调用凭证标记模型调用失败。
     * <p>
     * 如果该失败结果的调用序号早于或等于最新成功序号，说明它是乱序返回的旧结果，
     * 不能再把已经恢复健康的状态重新打开熔断。
     * consecutiveFailures作用是标记在正常状态CLOSED下的失败次数，如果达到阈值，就会进入熔断状态，并将consecutiveFailures清零，
     * 还设置有恢复时间
     * </p>
     *
     * @param permit 调用前获取的凭证
     */
    public void markFailure(CallPermit permit) {
        if (permit == null || !permit.allowed()) {
            return;
        }
        long now = System.currentTimeMillis();
        healthById.compute(permit.modelId(), (k, v) -> {
            if (v == null) {
                v = new ModelHealth();
            }
            if (permit.sequence() <= v.latestSuccessSequence) {
                return v;
            }
            v.latestFailureSequence = Math.max(v.latestFailureSequence, permit.sequence());
            if (v.state == State.HALF_OPEN) {
                // 试探请求失败，说明模型还没恢复，重新进入 OPEN 熔断状态。
                v.state = State.OPEN;
                v.openUntil = now + properties.getSelection().getOpenDurationMs();
                v.consecutiveFailures = 0;
                v.halfOpenInFlight = false;
                return v;
            }
            v.consecutiveFailures++;
            if (v.consecutiveFailures >= properties.getSelection().getFailureThreshold()) {
                // 连续失败达到阈值，打开熔断器，并设置下一次允许试探的时间。
                v.state = State.OPEN;
                v.openUntil = now + properties.getSelection().getOpenDurationMs();
                v.consecutiveFailures = 0;
            }
            return v;
        });
    }

    /**
     * 单个模型的健康状态快照。
     */
    private static class ModelHealth {
        /**
         * 连续失败次数，达到配置阈值后会触发熔断。
         */
        private int consecutiveFailures;

        /**
         * OPEN 状态持续到的时间戳，单位为毫秒。
         */
        private long openUntil;

        /**
         * HALF_OPEN 状态下是否已有试探请求正在执行。
         */
        private boolean halfOpenInFlight;

        /**
         * 当前熔断状态。
         */
        private State state;

        /**
         * 下一次允许调用时要分配的序号。
         */
        private long callSequence;

        /**
         * 最近一次成功回写的调用序号。
         */
        private long latestSuccessSequence;

        /**
         * 最近一次失败回写的调用序号。
         */
        private long latestFailureSequence;

        private ModelHealth() {
            this.consecutiveFailures = 0;
            this.openUntil = 0L;
            this.halfOpenInFlight = false;
            this.state = State.CLOSED;
            this.callSequence = 0L;
            this.latestSuccessSequence = 0L;
            this.latestFailureSequence = 0L;
        }

        private long nextSequence() {
            return ++callSequence;
        }
    }

    /**
     * 熔断器状态。
     */
    private enum State {
        /**
         * 关闭状态：模型正常可调用。
         */
        CLOSED,

        /**
         * 打开状态：模型已熔断，暂时不可调用。
         */
        OPEN,

        /**
         * 半开状态：熔断冷却结束，允许少量请求试探模型是否恢复。
         */
        HALF_OPEN
    }
}
