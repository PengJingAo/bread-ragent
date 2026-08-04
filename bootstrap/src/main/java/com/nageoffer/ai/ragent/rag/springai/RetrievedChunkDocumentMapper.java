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
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 项目检索结果与 Spring AI Document 的无损适配器。
 *
 * <p>领域对象仍是唯一业务模型，Spring AI 类型只停留在适配层。</p>
 */
@Component
public class RetrievedChunkDocumentMapper {

    public static final String CHUNK_ID = "chunkId";
    public static final String DOC_ID = "docId";
    public static final String DOC_NAME = "docName";
    public static final String CHUNK_INDEX = "chunkIndex";
    public static final String SOURCE_TYPE = "sourceType";
    public static final String URL = "url";
    public static final String INTENT_ID = "intentId";
    public static final String KNOWLEDGE_BASE_ID = "knowledgeBaseId";

    public Document toDocument(RetrievedChunk chunk) {
        Map<String, Object> metadata = new HashMap<>();
        putIfNotNull(metadata, CHUNK_ID, chunk.getId());
        putIfNotNull(metadata, DOC_ID, chunk.getDocId());
        putIfNotNull(metadata, DOC_NAME, chunk.getDocName());
        putIfNotNull(metadata, CHUNK_INDEX, chunk.getChunkIndex());

        return Document.builder()
                .id(chunk.getId())
                .text(chunk.getText())
                .metadata(metadata)
                .score(chunk.getScore() == null ? null : chunk.getScore().doubleValue())
                .build();
    }

    public RetrievedChunk fromDocument(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        return RetrievedChunk.builder()
                .id(stringValue(metadata.getOrDefault(CHUNK_ID, document.getId())))
                .text(document.getText())
                .score(document.getScore() == null ? null : document.getScore().floatValue())
                .docId(stringValue(metadata.get(DOC_ID)))
                .docName(stringValue(metadata.get(DOC_NAME)))
                .chunkIndex(integerValue(metadata.get(CHUNK_INDEX)))
                .build();
    }

    private void putIfNotNull(Map<String, Object> metadata, String key, Object value) {
        if (value != null) {
            metadata.put(key, value);
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Integer integerValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return value == null ? null : Integer.valueOf(String.valueOf(value));
    }
}
