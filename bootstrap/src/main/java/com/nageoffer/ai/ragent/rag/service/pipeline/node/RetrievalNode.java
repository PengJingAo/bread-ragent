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
import com.nageoffer.ai.ragent.rag.core.retrieval.RetrievalEngine;
import com.nageoffer.ai.ragent.rag.service.pipeline.ChatFlowNode;
import com.nageoffer.ai.ragent.rag.service.pipeline.ChatFlowState;
import com.nageoffer.ai.ragent.rag.service.pipeline.NodeResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 执行原有知识库与 MCP 联合检索。 */
@Component
@RequiredArgsConstructor
public class RetrievalNode implements ChatFlowNode {

    private final RetrievalEngine retrievalEngine;

    @Override
    public int order() {
        return 600;
    }

    @Override
    @RagTraceNode(name = "retrieval-flow", type = "FLOW_NODE")
    public NodeResult execute(ChatFlowState state) {
        state.setRetrievalContext(retrievalEngine.retrieve(state.getContext().getSubIntents()));
        return NodeResult.CONTINUE;
    }
}
