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
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagentDocumentJoinerTest {

    @Test
    void 多路重复文档通过Rrf融合并排到首位() {
        SearchChannelProperties properties = new SearchChannelProperties();
        properties.getFusion().setRrfK(20);
        RagentDocumentJoiner joiner = new RagentDocumentJoiner(properties);

        Document common = document("common");
        Map<Query, List<List<Document>>> input = new LinkedHashMap<>();
        input.put(new Query("question"), List.of(
                List.of(common, document("vector-only")),
                List.of(document("keyword-only"), common)));

        List<Document> result = joiner.join(input);

        assertEquals(List.of("common", "keyword-only", "vector-only"),
                result.stream().map(Document::getId).toList());
        assertTrue(result.get(0).getScore() > result.get(1).getScore());
    }

    private Document document(String id) {
        return Document.builder().id(id).text(id).build();
    }
}
