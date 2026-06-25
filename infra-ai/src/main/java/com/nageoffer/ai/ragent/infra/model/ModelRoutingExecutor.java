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

import com.nageoffer.ai.ragent.framework.errorcode.BaseErrorCode;
import com.nageoffer.ai.ragent.framework.exception.RemoteException;
import com.nageoffer.ai.ragent.infra.enums.ModelCapability;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

/**
 * 模型路由执行器
 * 负责在多个模型候选者之间进行调度执行，并提供故障转移（Fallback）和健康检查机制
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelRoutingExecutor {

    private final ModelHealthStore healthStore;



    /**
     * 只负责通用流程  遍历模型集合，选择模型，找客户端，判断是否健康，执行逻辑，失败切换
     * 其中的ModelCaller是由调用方传进来的 Lambda决定具体执行什么调用逻辑
     * @param capability
     * @param targets
     * @param clientResolver
     * @param caller
     * @return
     * @param <C>
     * @param <T>
     */
    public <C, T> T executeWithFallback(
            ModelCapability capability,
            List<ModelTarget> targets,
            Function<ModelTarget, C> clientResolver,
            ModelCaller<C, T> caller) {
        String label = capability.getDisplayName();
        if (targets == null || targets.isEmpty()) {
            throw new RemoteException("No " + label + " model candidates available");
        }

        Throwable last = null;
        for (ModelTarget target : targets) {
            //找到目标模型对应的客户端对象
            //具体实现逻辑，需要调用方Lambda实现，只是确定一个函数规范，传入T类型数据，返回C类型数据（此时T已经确定是ModelTarget）
            C client = clientResolver.apply(target);
            if (client == null) {
                log.warn("{} provider client missing: provider={}, modelId={}", label, target.candidate().getProvider(), target.id());
                continue;
            }
            //判断是否可以使用
            ModelHealthStore.CallPermit permit = healthStore.acquireCall(target.id());
            if (!permit.allowed()) {
                continue;
            }

            try {
                T response = caller.call(client, target);
                // 带凭证标记成功，避免旧请求乱序返回后覆盖更新的健康状态。
                healthStore.markSuccess(permit);
                return response;
            } catch (Exception e) {
                last = e;
                // 带凭证标记失败，避免旧失败覆盖更新的成功状态。
                //todo：弄懂如果模型调用失败，如何将其设置为暂时不可用模型的？如何恢复？
                healthStore.markFailure(permit);
                log.warn("{} model failed, fallback to next. modelId={}, provider={}", label, target.id(), target.candidate().getProvider(), e);
            }
        }
        //遍历所有候选模型都无法使用，抛出异常
        throw new RemoteException(
                "All " + label + " model candidates failed: " + (last == null ? "unknown" : last.getMessage()),
                last,
                BaseErrorCode.REMOTE_ERROR
        );
    }
}
