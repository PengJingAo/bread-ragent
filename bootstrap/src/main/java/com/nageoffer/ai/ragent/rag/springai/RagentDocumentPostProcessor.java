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

import com.nageoffer.ai.ragent.framework.convention.RetrievedChunk;
import com.nageoffer.ai.ragent.infra.rerank.RerankService;
import com.nageoffer.ai.ragent.rag.config.SearchChannelProperties;
import com.nageoffer.ai.ragent.rag.config.RAGConfigProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.stereotype.Component;

import java.util.List;

/** 将现有 Rerank 服务适配为 Spring AI 文档后处理扩展点。 */
@Component
@RequiredArgsConstructor
public class RagentDocumentPostProcessor implements DocumentPostProcessor {

    public static final String TOP_K_CONTEXT_KEY = "ragent.topK";

    private final RerankService rerankService;
    private final RAGConfigProperties ragConfigProperties;
    private final SearchChannelProperties searchProperties;
    private final RetrievedChunkDocumentMapper documentMapper;

    @Override
    public List<Document> process(Query query, List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        if (!Boolean.TRUE.equals(ragConfigProperties.getRerankEnabled())) {
            return documents;
        }
        List<RetrievedChunk> chunks = documents.stream().map(documentMapper::fromDocument).toList();
        List<RetrievedChunk> reranked = rerankService.rerank(query.text(), chunks, resolveTopK(query));
        return reranked.stream().map(documentMapper::toDocument).toList();
    }

    private int resolveTopK(Query query) {
        Object value = query.context().get(TOP_K_CONTEXT_KEY);
        return value instanceof Number number ? number.intValue() : searchProperties.getDefaultTopK();
    }
}
