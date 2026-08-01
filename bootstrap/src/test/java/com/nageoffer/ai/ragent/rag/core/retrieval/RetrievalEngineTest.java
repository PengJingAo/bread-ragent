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

package com.nageoffer.ai.ragent.rag.core.retrieval;

import com.nageoffer.ai.ragent.rag.config.SearchChannelProperties;
import com.nageoffer.ai.ragent.rag.core.intent.IntentNode;
import com.nageoffer.ai.ragent.rag.core.intent.NodeScore;
import com.nageoffer.ai.ragent.rag.core.mcp.McpParameterExtractor;
import com.nageoffer.ai.ragent.rag.core.mcp.McpToolRegistry;
import com.nageoffer.ai.ragent.rag.core.prompt.ContextFormatter;
import com.nageoffer.ai.ragent.rag.core.prompt.PromptTemplateLoader;
import com.nageoffer.ai.ragent.rag.dto.RetrievalContext;
import com.nageoffer.ai.ragent.rag.dto.SubQuestionIntent;
import com.nageoffer.ai.ragent.rag.enums.IntentKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RetrievalEngineTest {

    private SearchChannelProperties searchProperties;
    private MultiChannelRetrievalEngine multiChannelRetrievalEngine;
    private RetrievalEngine retrievalEngine;

    @BeforeEach
    void setUp() {
        searchProperties = mock(SearchChannelProperties.class, Answers.RETURNS_DEEP_STUBS);
        multiChannelRetrievalEngine = mock(MultiChannelRetrievalEngine.class);
        when(searchProperties.getDefaultTopK()).thenReturn(5);
        when(searchProperties.resolveRecallBudget(5)).thenReturn(10);
        when(searchProperties.getFusion().getRerankCandidateLimit()).thenReturn(20);

        retrievalEngine = new RetrievalEngine(
                searchProperties,
                mock(ContextFormatter.class),
                mock(PromptTemplateLoader.class),
                mock(McpParameterExtractor.class),
                mock(McpToolRegistry.class),
                multiChannelRetrievalEngine,
                Runnable::run,
                Runnable::run);
    }

    @Test
    void systemIntentDoesNotTriggerKnowledgeRetrieval() {
        IntentNode systemNode = IntentNode.builder()
                .id("system-feedback")
                .kind(IntentKind.SYSTEM)
                .build();
        SubQuestionIntent intent = new SubQuestionIntent(
                "我有一个功能建议",
                List.of(NodeScore.builder().node(systemNode).score(1.0).build()));

        RetrievalContext result = retrievalEngine.retrieve(List.of(intent));

        assertThat(result.hasKb()).isFalse();
        assertThat(result.getIntentChunks()).isEmpty();
        verifyNoInteractions(multiChannelRetrievalEngine);
    }
}
