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

import com.nageoffer.ai.ragent.framework.trace.RagTraceNode;
import com.nageoffer.ai.ragent.rag.core.guidance.GuidanceDecision;
import com.nageoffer.ai.ragent.rag.core.guidance.IntentGuidanceService;
import com.nageoffer.ai.ragent.rag.service.pipeline.ChatFlowNode;
import com.nageoffer.ai.ragent.rag.service.pipeline.ChatFlowState;
import com.nageoffer.ai.ragent.rag.service.pipeline.NodeResult;
import com.nageoffer.ai.ragent.rag.service.pipeline.StreamChatContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 对歧义问题保持现有澄清输出与短路语义。 */
@Component
@RequiredArgsConstructor
public class GuidanceNode implements ChatFlowNode {

    private final IntentGuidanceService guidanceService;

    @Override
    public int order() {
        return 400;
    }

    @Override
    @RagTraceNode(name = "guidance-flow", type = "FLOW_NODE")
    public NodeResult execute(ChatFlowState state) {
        StreamChatContext context = state.getContext();
        GuidanceDecision decision = guidanceService.detectAmbiguity(
                context.getRewriteResult().rewrittenQuestion(), context.getSubIntents());
        if (!decision.isPrompt()) {
            return NodeResult.CONTINUE;
        }
        context.getCallback().onContent(decision.getPrompt());
        context.getCallback().onComplete();
        return NodeResult.COMPLETED;
    }
}
