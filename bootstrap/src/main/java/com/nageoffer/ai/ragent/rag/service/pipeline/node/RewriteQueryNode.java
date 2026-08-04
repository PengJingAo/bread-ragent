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
import com.nageoffer.ai.ragent.rag.core.rewrite.QueryRewriteService;
import com.nageoffer.ai.ragent.rag.service.pipeline.ChatFlowNode;
import com.nageoffer.ai.ragent.rag.service.pipeline.ChatFlowState;
import com.nageoffer.ai.ragent.rag.service.pipeline.NodeResult;
import com.nageoffer.ai.ragent.rag.service.pipeline.StreamChatContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 复用现有问题改写与子问题拆分能力。 */
@Component
@RequiredArgsConstructor
public class RewriteQueryNode implements ChatFlowNode {

    private final QueryRewriteService queryRewriteService;

    @Override
    public int order() {
        return 200;
    }

    @Override
    @RagTraceNode(name = "rewrite-query-flow", type = "FLOW_NODE")
    public NodeResult execute(ChatFlowState state) {
        StreamChatContext context = state.getContext();
        context.setRewriteResult(queryRewriteService.rewriteWithSplit(
                context.getQuestion(), context.getHistory()));
        return NodeResult.CONTINUE;
    }
}
