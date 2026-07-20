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

package com.nageoffer.ai.ragent.rag.core.retrieval.channel;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.nageoffer.ai.ragent.framework.convention.RetrievedChunk;
import com.nageoffer.ai.ragent.rag.config.SearchChannelProperties;
import com.nageoffer.ai.ragent.rag.core.intent.NodeScore;
import com.nageoffer.ai.ragent.rag.core.intent.NodeScoreFilters;
import com.nageoffer.ai.ragent.rag.core.keyword.KeywordRetrieverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 关键词检索通道
 * <p>
 * 基于全文检索引擎（ES）的 BM25 关键词召回，与向量通道互补：擅长精确词、编号、专有名词等
 * 仅当开启 ES 关键词检索（rag.keyword.type=es）时才注册，
 * 否则整个通道不存在，引擎自动退化为纯向量检索
 * <p>
 * 与其他通道并行执行，结果统一进 RRF 融合，通道间无先后与优先级之分
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "rag.keyword", name = "type", havingValue = "es")
public class KeywordSearchChannel implements SearchChannel {

    private final KeywordRetrieverService keywordRetriever;
    private final SearchChannelProperties properties;

    @Override
    public String getName() {
        return "KeywordSearch";
    }

    @Override
    public SearchChannelType getType() {
        return SearchChannelType.KEYWORD;
    }

    @Override
    public boolean isEnabled(SearchContext context) {
        SearchChannelProperties.Keyword keyword = properties.getChannels().getKeyword();
        if (!keyword.isEnabled()) {
            return false;
        }
        List<NodeScore> kbIntents = extractHighConfidenceKbIntents(context);
        if (CollUtil.isEmpty(kbIntents)) {
            log.info("跳过关键词检索：未识别到高置信 KB 意图，阈值 {}", keyword.getMinIntentScore());
            return false;
        }
        return true;
    }

    @Override
    public SearchChannelResult search(SearchContext context) {
        long startTime = System.currentTimeMillis();
        try {
            List<String> collections = resolveCollections(context);
            if (CollUtil.isEmpty(collections)) {
                log.info("关键词检索未解析到目标知识库，跳过");
                return emptyResult(startTime);
            }

            int topK = context.getBudget().recallBudget();
            List<RetrievedChunk> chunks = keywordRetriever.search(context.getMainQuestion(), collections, topK);

            long latency = System.currentTimeMillis() - startTime;
            log.info("关键词检索完成，知识库={}，检索到 {} 个 Chunk，耗时 {}ms", collections, chunks.size(), latency);

            return SearchChannelResult.builder()
                    .channelType(SearchChannelType.KEYWORD)
                    .channelName(getName())
                    .chunks(chunks)
                    .latencyMs(latency)
                    .build();
        } catch (Exception e) {
            log.error("关键词检索失败", e);
            return emptyResult(startTime);
        }
    }

    /**
     * 解析高置信 KB 意图对应的知识库 collection。
     */
    private List<String> resolveCollections(SearchContext context) {
        return extractHighConfidenceIntentCollections(context);
    }

    /**
     * 从意图识别结果提取 KB 意图对应的 collection 名称
     */
    private List<String> extractHighConfidenceIntentCollections(SearchContext context) {
        return extractHighConfidenceKbIntents(context).stream()
                .map(ns -> ns.getNode().getCollectionName())
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toList();
    }

    private List<NodeScore> extractHighConfidenceKbIntents(SearchContext context) {
        if (CollUtil.isEmpty(context.getIntents())) {
            return List.of();
        }
        List<NodeScore> allScores = context.getIntents().stream()
                .flatMap(si -> si.nodeScores().stream())
                .toList();
        double minScore = properties.getChannels().getKeyword().getMinIntentScore();
        return NodeScoreFilters.kb(allScores, minScore);
    }

    private SearchChannelResult emptyResult(long startTime) {
        return SearchChannelResult.builder()
                .channelType(SearchChannelType.KEYWORD)
                .channelName(getName())
                .chunks(List.of())
                .latencyMs(System.currentTimeMillis() - startTime)
                .build();
    }
}
