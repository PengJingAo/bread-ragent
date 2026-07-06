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

import cn.hutool.core.util.StrUtil;
import com.nageoffer.ai.ragent.infra.config.AIModelProperties;
import com.nageoffer.ai.ragent.infra.enums.ModelProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 模型选择器
 * <p>
 * 负责根据配置和当前需求（如普通对话、深度思考、Embedding、Rerank 等）选择合适的模型候选列表。
 * 该类只负责“选出候选模型并排序”，真正的调用、失败重试和熔断回写由 {@link ModelRoutingExecutor}
 * 和 {@link ModelHealthStore} 负责。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelSelector {

    /**
     * AI 模型配置，来源于 application 配置中 ai 前缀下的模型组、候选模型和 provider 配置。
     */
    private final AIModelProperties properties;

    /**
     * 模型健康状态仓库，用于过滤已经处于熔断或半开试探中的模型。
     */
    private final ModelHealthStore healthStore;

    /**
     * 选择聊天模型候选列表。
     * <p>
     * 普通聊天优先使用 defaultModel；深度思考模式优先使用 deepThinkingModel，
     * 并且会过滤掉不支持思考能力的候选模型。
     * </p>
     *
     * @param deepThinking 是否启用深度思考模式
     * @return 按优先级排序后的可用聊天模型目标列表
     */
    public List<ModelTarget> selectChatCandidates(boolean deepThinking) {
        AIModelProperties.ModelGroup group = properties.getChat();
        if (group == null) {
            // 没有配置聊天模型组时，返回空候选列表，由调用方决定如何报错或降级。
            return List.of();
        }

        String firstChoiceModelId = resolveFirstChoiceModel(group, deepThinking);
        return selectCandidates(group, firstChoiceModelId, deepThinking);
    }

    /**
     * 选择向量嵌入模型候选列表。
     *
     * @return 按配置排序并过滤健康状态后的 Embedding 模型目标列表
     */
    public List<ModelTarget> selectEmbeddingCandidates() {
        return selectCandidates(properties.getEmbedding());
    }

    /**
     * 选择重排模型候选列表。
     *
     * @return 按配置排序并过滤健康状态后的 Rerank 模型目标列表
     */
    public List<ModelTarget> selectRerankCandidates() {
        return selectCandidates(properties.getRerank());
    }

    /**
     * 选择视觉大模型候选列表。
     *
     * @return 按配置排序并过滤健康状态后的 VLM 模型目标列表
     */
    public List<ModelTarget> selectVlmCandidates() {
        return selectCandidates(properties.getVlm());
    }

    /**
     * 解析当前场景下的首选模型 ID。
     * <p>
     * 深度思考模式下，如果配置了 deepThinkingModel，就优先把它排在前面；
     * 否则回退到 defaultModel。
     * </p>
     *
     * @param group 模型组配置
     * @param deepThinking 是否启用深度思考模式
     * @return 首选模型 ID
     */
    private String resolveFirstChoiceModel(AIModelProperties.ModelGroup group, boolean deepThinking) {
        if (deepThinking) {
            String deepModel = group.getDeepThinkingModel();
            if (StrUtil.isNotBlank(deepModel)) {
                return deepModel;
            }
        }
        return group.getDefaultModel();
    }

    /**
     * 选择非聊天类模型候选。
     * <p>
     * Embedding 和 Rerank 当前没有深度思考模式，因此默认使用模型组的 defaultModel 作为首选模型。
     * </p>
     *
     * @param group 模型组配置
     * @return 可用模型目标列表
     */
    private List<ModelTarget> selectCandidates(AIModelProperties.ModelGroup group) {
        if (group == null) {
            return List.of();
        }
        return selectCandidates(group, group.getDefaultModel(), false);
    }

    /**
     * 根据模型组配置选择候选模型。
     * <p>
     * 这里分两步执行：先对配置中的候选模型做启用状态、能力和优先级排序；
     * 再把候选配置转换为真正可调用的 {@link ModelTarget}。
     * </p>
     *
     * @param group 模型组配置
     * @param firstChoiceModelId 首选模型 ID
     * @param deepThinking 是否启用深度思考模式
     * @return 可用模型目标列表
     */
    private List<ModelTarget> selectCandidates(AIModelProperties.ModelGroup group, String firstChoiceModelId, boolean deepThinking) {
        if (group == null || group.getCandidates() == null) {
            return List.of();
        }

        List<AIModelProperties.ModelCandidate> orderedCandidates =
                filterAndSortCandidates(group.getCandidates(), firstChoiceModelId, deepThinking);

        return buildAvailableTargets(orderedCandidates);
    }

    /**
     * 过滤并排序候选模型列表
     * <p>
     * 排序规则：
     * 1. 首选模型排在最前面；
     * 2. priority 数值越小越靠前；
     * 3. priority 相同时，按 id 字符串排序，保证结果稳定。
     * </p>
     *
     * @param candidates 配置中的候选模型列表
     * @param firstChoiceModelId 首选模型 ID
     * @param deepThinking 是否启用深度思考模式
     * @return 过滤并排序后的候选模型配置
     */
    private List<AIModelProperties.ModelCandidate> filterAndSortCandidates(List<AIModelProperties.ModelCandidate> candidates,
                                                                           String firstChoiceModelId,
                                                                           boolean deepThinking) {
        List<AIModelProperties.ModelCandidate> enabled = candidates.stream()
                // 过滤 null 配置，并排除显式 enabled=false 的模型；enabled 未配置时默认视为可用。
                .filter(c -> c != null && !Boolean.FALSE.equals(c.getEnabled()))
                // 深度思考模式下，只保留 supportsThinking=true 的模型。
                .filter(c -> !deepThinking || Boolean.TRUE.equals(c.getSupportsThinking()))
                .sorted(Comparator
                        // Objects.equals 为 true 表示命中首选模型，取反后 false 会排在 true 前面。
                        .comparing((AIModelProperties.ModelCandidate c) ->
                                !Objects.equals(resolveId(c), firstChoiceModelId))
                        // priority 越小优先级越高；null 放到最后。
                        .thenComparing(AIModelProperties.ModelCandidate::getPriority,
                                Comparator.nullsLast(Integer::compareTo))
                        // 最后按 id 排序，避免同优先级候选的顺序不稳定。
                        .thenComparing(AIModelProperties.ModelCandidate::getId,
                                Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());

        if (deepThinking && enabled.isEmpty()) {
            log.warn("深度思考模式没有可用候选模型");
        }

        return enabled;
    }

    /**
     * 将候选模型配置转换为可调用的模型目标。
     * <p>
     * 转换过程中会读取 provider 配置，并过滤掉当前健康状态不可用的模型。
     * </p>
     *
     * @param candidates 已排序的候选模型配置
     * @return 可用于后续调用的模型目标列表
     */
    private List<ModelTarget> buildAvailableTargets(List<AIModelProperties.ModelCandidate> candidates) {
        Map<String, AIModelProperties.ProviderConfig> providers = properties.getProviders();

        return candidates.stream()
                .map(candidate -> buildModelTarget(candidate, providers))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 构建单个模型目标。
     * <p>
     * 如果模型处于熔断或半开试探中，直接返回 null 表示本轮不参与候选；
     * 如果非 NOOP provider 缺少 provider 配置，也返回 null，避免后续调用时才失败。
     * </p>
     *
     * @param candidate 候选模型配置
     * @param providers provider 配置映射
     * @return 可调用的模型目标；返回 null 表示该候选不可用
     */
    private ModelTarget buildModelTarget(AIModelProperties.ModelCandidate candidate, Map<String, AIModelProperties.ProviderConfig> providers) {
        String modelId = resolveId(candidate);

        if (healthStore.isUnavailable(modelId)) {
            // 模型健康状态不可用时，先从候选列表中剔除。
            return null;
        }

        AIModelProperties.ProviderConfig provider = providers.get(candidate.getProvider());
        if (provider == null && !ModelProvider.NOOP.matches(candidate.getProvider())) {
            // NOOP 是特殊 provider，可以没有外部 provider 配置；其他 provider 缺配置时不可调用。
            log.warn("Provider配置缺失: provider={}, modelId={}", candidate.getProvider(), modelId);
            return null;
        }

        return new ModelTarget(modelId, candidate, provider);
    }

    /**
     * 解析模型唯一 ID。
     * <p>
     * 如果候选模型显式配置了 id，则直接使用该 id；
     * 否则使用 provider::model 生成一个稳定 ID，保证健康状态和调用凭证有统一的 key。
     * </p>
     *
     * @param candidate 候选模型配置
     * @return 模型唯一 ID
     */
    private String resolveId(AIModelProperties.ModelCandidate candidate) {
        if (StrUtil.isNotBlank(candidate.getId())) {
            return candidate.getId();
        }
        return String.format("%s::%s",
                Objects.toString(candidate.getProvider(), "unknown"),
                Objects.toString(candidate.getModel(), "unknown"));
    }
}
