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

package com.nageoffer.ai.ragent.rag.service.pipeline.node;

import com.nageoffer.ai.ragent.framework.convention.SourceRef;
import com.nageoffer.ai.ragent.framework.trace.RagTraceNode;
import com.nageoffer.ai.ragent.rag.core.intent.IntentResolver;
import com.nageoffer.ai.ragent.rag.core.source.CitationContextEnricher;
import com.nageoffer.ai.ragent.rag.core.source.GroundingChunksAssembler;
import com.nageoffer.ai.ragent.rag.core.source.SourcesAssembler;
import com.nageoffer.ai.ragent.rag.dto.RetrievalContext;
import com.nageoffer.ai.ragent.rag.service.pipeline.ChatFlowNode;
import com.nageoffer.ai.ragent.rag.service.pipeline.ChatFlowState;
import com.nageoffer.ai.ragent.rag.service.pipeline.NodeResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** 组装来源、引用、grounding 与模型所需意图分组。 */
@Component
@RequiredArgsConstructor
public class AssembleContextNode implements ChatFlowNode {

    private final IntentResolver intentResolver;
    private final SourcesAssembler sourcesAssembler;
    private final GroundingChunksAssembler groundingChunksAssembler;
    private final CitationContextEnricher citationContextEnricher;

    @Override
    public int order() {
        return 800;
    }

    @Override
    @RagTraceNode(name = "assemble-context-flow", type = "FLOW_NODE")
    public NodeResult execute(ChatFlowState state) {
        RetrievalContext retrievalContext = state.getRetrievalContext();
        List<SourceRef> sources = sourcesAssembler.assemble(retrievalContext.getIntentChunks());
        state.getContext().getCallback().onSources(sources);
        retrievalContext.setKbContext(citationContextEnricher.enrich(retrievalContext.getKbContext(), sources));
        state.getContext().getCallback().onGroundingChunks(
                groundingChunksAssembler.assemble(retrievalContext.getIntentChunks()));
        state.setMergedIntentGroup(intentResolver.mergeIntentGroup(state.getContext().getSubIntents()));
        return NodeResult.CONTINUE;
    }
}
