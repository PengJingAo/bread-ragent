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
import com.nageoffer.ai.ragent.rag.service.pipeline.ChatFlowNode;
import com.nageoffer.ai.ragent.rag.service.pipeline.ChatFlowState;
import com.nageoffer.ai.ragent.rag.service.pipeline.NodeResult;
import org.springframework.stereotype.Component;

/** 保持旧链路的空检索提示和结束语义。 */
@Component
public class EmptyRetrievalNode implements ChatFlowNode {

    @Override
    public int order() {
        return 700;
    }

    @Override
    @RagTraceNode(name = "empty-retrieval-flow", type = "FLOW_NODE")
    public NodeResult execute(ChatFlowState state) {
        if (!state.getRetrievalContext().isEmpty()) {
            return NodeResult.CONTINUE;
        }
        state.getContext().getCallback().onContent("未检索到与问题相关的文档内容。");
        state.getContext().getCallback().onComplete();
        return NodeResult.COMPLETED;
    }
}
