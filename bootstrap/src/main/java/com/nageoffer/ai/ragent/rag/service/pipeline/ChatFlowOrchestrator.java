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

package com.nageoffer.ai.ragent.rag.service.pipeline;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/** 节点化对话编排器，只负责顺序与短路，不承载业务判断。 */
@Service
public class ChatFlowOrchestrator {

    private final List<ChatFlowNode> nodes;

    public ChatFlowOrchestrator(List<ChatFlowNode> nodes) {
        this.nodes = nodes.stream().sorted(Comparator.comparingInt(ChatFlowNode::order)).toList();
    }

    public NodeResult execute(StreamChatContext context) {
        ChatFlowState state = new ChatFlowState(context);
        for (ChatFlowNode node : nodes) {
            NodeResult result = node.execute(state);
            if (result != NodeResult.CONTINUE) {
                return result;
            }
        }
        return NodeResult.COMPLETED;
    }
}
