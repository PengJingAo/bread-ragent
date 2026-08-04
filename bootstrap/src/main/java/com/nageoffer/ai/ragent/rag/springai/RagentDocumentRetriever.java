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

package com.nageoffer.ai.ragent.rag.springai;

import com.nageoffer.ai.ragent.rag.config.SearchChannelProperties;
import com.nageoffer.ai.ragent.rag.core.retrieval.MultiChannelRetrievalEngine;
import com.nageoffer.ai.ragent.rag.core.retrieval.RetrievalBudget;
import com.nageoffer.ai.ragent.rag.dto.SubQuestionIntent;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 将现有多通道检索引擎适配为 Spring AI DocumentRetriever。
 *
 * <p>当前检索引擎内部仍完整保留并行召回、RRF、Rerank 与裁剪。</p>
 */
@Component
@RequiredArgsConstructor
public class RagentDocumentRetriever implements DocumentRetriever {

    public static final String SUB_INTENTS_CONTEXT_KEY = "ragent.subIntents";
    public static final String RETRIEVAL_BUDGET_CONTEXT_KEY = "ragent.retrievalBudget";

    private final MultiChannelRetrievalEngine retrievalEngine;
    private final SearchChannelProperties searchProperties;
    private final RetrievedChunkDocumentMapper documentMapper;

    @Override
    public List<Document> retrieve(Query query) {
        List<SubQuestionIntent> subIntents = resolveSubIntents(query);
        RetrievalBudget budget = resolveBudget(query);
        return retrievalEngine.retrieveKnowledgeChannels(subIntents, budget).stream()
                .map(documentMapper::toDocument)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<SubQuestionIntent> resolveSubIntents(Query query) {
        Object value = query.context().get(SUB_INTENTS_CONTEXT_KEY);
        if (value instanceof List<?> list && list.stream().allMatch(SubQuestionIntent.class::isInstance)) {
            return (List<SubQuestionIntent>) list;
        }
        return List.of(new SubQuestionIntent(query.text(), List.of()));
    }

    private RetrievalBudget resolveBudget(Query query) {
        Object value = query.context().get(RETRIEVAL_BUDGET_CONTEXT_KEY);
        if (value instanceof RetrievalBudget budget) {
            return budget;
        }
        int topK = searchProperties.getDefaultTopK();
        int recallBudget = searchProperties.resolveRecallBudget(topK);
        int candidateLimit = searchProperties.getFusion().getRerankCandidateLimit();
        return new RetrievalBudget(recallBudget, candidateLimit, topK);
    }
}
