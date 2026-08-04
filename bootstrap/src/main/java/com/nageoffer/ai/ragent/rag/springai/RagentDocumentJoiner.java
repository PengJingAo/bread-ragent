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
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.join.DocumentJoiner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Spring AI Modular RAG 使用的倒数排名融合（RRF）适配器。 */
@Component
@RequiredArgsConstructor
public class RagentDocumentJoiner implements DocumentJoiner {

    private final SearchChannelProperties searchProperties;

    @Override
    public List<Document> join(Map<Query, List<List<Document>>> documentsForQuery) {
        Map<String, RankedDocument> merged = new LinkedHashMap<>();
        int rrfK = searchProperties.getFusion().getRrfK();

        documentsForQuery.values().stream()
                .flatMap(List::stream)
                .forEach(channelDocuments -> accumulate(channelDocuments, rrfK, merged));

        List<Document> ranked = merged.values().stream()
                .sorted(Comparator.comparingDouble(RankedDocument::score).reversed())
                .map(item -> item.document().mutate().score(item.score()).build())
                .toList();
        int candidateLimit = searchProperties.getFusion().getRerankCandidateLimit();
        return candidateLimit > 0 && ranked.size() > candidateLimit
                ? ranked.subList(0, candidateLimit)
                : ranked;
    }

    private void accumulate(List<Document> documents, int rrfK, Map<String, RankedDocument> merged) {
        List<Document> safeDocuments = documents == null ? new ArrayList<>() : documents;
        for (int index = 0; index < safeDocuments.size(); index++) {
            Document document = safeDocuments.get(index);
            String key = document.getId() == null ? document.getText() : document.getId();
            double contribution = weightOf(document) / (rrfK + index + 1.0D);
            merged.compute(key, (ignored, existing) -> existing == null
                    ? new RankedDocument(document, contribution)
                    : new RankedDocument(existing.document(), existing.score() + contribution));
        }
    }

    private double weightOf(Document document) {
        Object sourceType = document.getMetadata().get(RetrievedChunkDocumentMapper.SOURCE_TYPE);
        SearchChannelProperties.ChannelWeights weights = searchProperties.getFusion().getChannelWeights();
        if (sourceType == null) {
            return weights.getDefaultWeight();
        }
        return switch (String.valueOf(sourceType).toLowerCase()) {
            case "vector" -> weights.getVector();
            case "keyword", "bm25" -> weights.getKeyword();
            case "graph" -> weights.getGraph();
            case "web", "web_search", "web-search" -> weights.getWebSearch();
            default -> weights.getDefaultWeight();
        };
    }

    private record RankedDocument(Document document, double score) {
    }
}
