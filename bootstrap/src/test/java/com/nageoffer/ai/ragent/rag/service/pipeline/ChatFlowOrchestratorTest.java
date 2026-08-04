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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatFlowOrchestratorTest {

    @Test
    void 节点按显式顺序执行() {
        List<Integer> executions = new ArrayList<>();
        ChatFlowOrchestrator orchestrator = new ChatFlowOrchestrator(List.of(
                node(300, NodeResult.COMPLETED, executions),
                node(100, NodeResult.CONTINUE, executions),
                node(200, NodeResult.CONTINUE, executions)));

        NodeResult result = orchestrator.execute(StreamChatContext.builder().question("q").build());

        assertEquals(NodeResult.COMPLETED, result);
        assertEquals(List.of(100, 200, 300), executions);
    }

    @Test
    void 完成或等待输入会短路后续节点() {
        List<Integer> executions = new ArrayList<>();
        ChatFlowOrchestrator orchestrator = new ChatFlowOrchestrator(List.of(
                node(100, NodeResult.WAITING_INPUT, executions),
                node(200, NodeResult.CONTINUE, executions)));

        NodeResult result = orchestrator.execute(StreamChatContext.builder().question("q").build());

        assertEquals(NodeResult.WAITING_INPUT, result);
        assertEquals(List.of(100), executions);
    }

    private ChatFlowNode node(int order, NodeResult result, List<Integer> executions) {
        return new ChatFlowNode() {
            @Override
            public int order() {
                return order;
            }

            @Override
            public NodeResult execute(ChatFlowState state) {
                executions.add(order);
                return result;
            }
        };
    }
}
