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
import lombok.RequiredArgsConstructor;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/** 将现有查询改写能力暴露为 Spring AI Modular RAG 扩展点。 */
@Component
@RequiredArgsConstructor
public class RagentQueryTransformer implements QueryTransformer {

    private final QueryRewriteService queryRewriteService;

    @Override
    public Query transform(Query query) {
        RewriteResult result = queryRewriteService.rewriteWithSplit(query.text(), toProjectHistory(query));
        return query.mutate().text(result.rewrittenQuestion()).build();
    }

    private List<ChatMessage> toProjectHistory(Query query) {
        return query.history().stream()
                .map(message -> switch (message.getMessageType()) {
                    case SYSTEM -> ChatMessage.system(message.getText());
                    case USER -> ChatMessage.user(message.getText());
                    case ASSISTANT -> ChatMessage.assistant(message.getText());
                    case TOOL -> null;
                })
                .filter(Objects::nonNull)
                .toList();
    }
}
