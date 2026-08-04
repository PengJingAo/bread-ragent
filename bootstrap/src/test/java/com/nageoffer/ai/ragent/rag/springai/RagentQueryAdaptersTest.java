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

import com.nageoffer.ai.ragent.framework.convention.ChatMessage;
import com.nageoffer.ai.ragent.rag.core.rewrite.QueryRewriteService;
import com.nageoffer.ai.ragent.rag.core.rewrite.RewriteResult;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.rag.Query;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RagentQueryAdaptersTest {

    @Test
    void 查询转换时保留会话历史语义() {
        QueryRewriteService service = mock(QueryRewriteService.class);
        when(service.rewriteWithSplit(eq("它怎么配置？"), eq(List.of(ChatMessage.user("Spring AI")))))
                .thenReturn(new RewriteResult("Spring AI 如何配置？", List.of("Spring AI 如何配置？")));
        RagentQueryTransformer transformer = new RagentQueryTransformer(service);
        Query query = Query.builder()
                .text("它怎么配置？")
                .history(List.of(new UserMessage("Spring AI")))
                .build();

        Query transformed = transformer.transform(query);

        assertEquals("Spring AI 如何配置？", transformed.text());
        assertEquals(query.history(), transformed.history());
    }

    @Test
    void 子问题拆分转换为多个SpringAiQuery() {
        QueryRewriteService service = mock(QueryRewriteService.class);
        when(service.rewriteWithSplit("比较 A 和 B"))
                .thenReturn(new RewriteResult("比较 A 和 B", List.of("A 是什么", "B 是什么")));
        RagentMultiQueryExpander expander = new RagentMultiQueryExpander(service);

        List<Query> queries = expander.expand(new Query("比较 A 和 B"));

        assertEquals(List.of("A 是什么", "B 是什么"), queries.stream().map(Query::text).toList());
        verify(service).rewriteWithSplit("比较 A 和 B");
    }
}
