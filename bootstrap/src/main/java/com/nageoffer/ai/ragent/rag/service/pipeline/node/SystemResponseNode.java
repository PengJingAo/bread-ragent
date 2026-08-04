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

import cn.hutool.core.util.StrUtil;
import com.nageoffer.ai.ragent.framework.trace.RagTraceNode;
import com.nageoffer.ai.ragent.infra.chat.StreamCancellationHandle;
import com.nageoffer.ai.ragent.rag.core.intent.IntentResolver;
import com.nageoffer.ai.ragent.rag.dto.SubQuestionIntent;
import com.nageoffer.ai.ragent.rag.service.handler.StreamTaskManager;
import com.nageoffer.ai.ragent.rag.service.pipeline.ChatFlowNode;
import com.nageoffer.ai.ragent.rag.service.pipeline.ChatFlowResponseService;
import com.nageoffer.ai.ragent.rag.service.pipeline.ChatFlowState;
import com.nageoffer.ai.ragent.rag.service.pipeline.NodeResult;
import com.nageoffer.ai.ragent.rag.service.pipeline.StreamChatContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** 处理不需要检索的系统意图。 */
@Component
@RequiredArgsConstructor
public class SystemResponseNode implements ChatFlowNode {

    private final IntentResolver intentResolver;
    private final ChatFlowResponseService responseService;
    private final StreamTaskManager taskManager;

    @Override
    public int order() {
        return 500;
    }

    @Override
    @RagTraceNode(name = "system-response-flow", type = "FLOW_NODE")
    public NodeResult execute(ChatFlowState state) {
        StreamChatContext context = state.getContext();
        List<SubQuestionIntent> subIntents = context.getSubIntents();
        boolean systemOnly = subIntents.stream()
                .allMatch(intent -> intentResolver.isSystemOnly(intent.nodeScores()));
        if (!systemOnly) {
            return NodeResult.CONTINUE;
        }

        String customPrompt = subIntents.stream()
                .flatMap(intent -> intent.nodeScores().stream())
                .map(score -> score.getNode().getPromptTemplate())
                .filter(StrUtil::isNotBlank)
                .findFirst()
                .orElse(null);
        StreamCancellationHandle handle = responseService.streamSystemResponse(
                context.getRewriteResult().rewrittenQuestion(), context.getHistory(), customPrompt,
                context.getCallback());
        taskManager.bindHandle(context.getTaskId(), handle);
        return NodeResult.COMPLETED;
    }
}
