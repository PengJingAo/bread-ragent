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
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RetrievedChunkDocumentMapperTest {

    private final RetrievedChunkDocumentMapper mapper = new RetrievedChunkDocumentMapper();

    @Test
    void 项目检索结果往返转换不丢失字段() {
        RetrievedChunk original = RetrievedChunk.builder()
                .id("chunk-1")
                .text("content")
                .score(0.75F)
                .docId("doc-1")
                .docName("说明书")
                .chunkIndex(3)
                .build();

        Document document = mapper.toDocument(original);
        RetrievedChunk restored = mapper.fromDocument(document);

        assertEquals(original, restored);
        assertEquals("chunk-1", document.getMetadata().get(RetrievedChunkDocumentMapper.CHUNK_ID));
    }
}
