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

package com.nageoffer.ai.ragent.infra.springai;

import com.nageoffer.ai.ragent.framework.convention.ChatMessage;
import com.nageoffer.ai.ragent.framework.convention.ChatRequest;
import com.nageoffer.ai.ragent.framework.trace.RagStreamTraceSupport;
import com.nageoffer.ai.ragent.infra.chat.StreamCallback;
import com.nageoffer.ai.ragent.infra.chat.StreamCancellationHandle;
import com.nageoffer.ai.ragent.infra.config.AIModelProperties;
import com.nageoffer.ai.ragent.infra.config.RagentAiRuntimeProperties;
import com.nageoffer.ai.ragent.infra.model.ModelTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.Executor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpringAiChatRuntimeTest {

    private SpringAiModelFactory modelFactory;
    private RagStreamTraceSupport traceSupport;
    private RagStreamTraceSupport.StreamSpan span;
    private OpenAiChatModel chatModel;
    private SpringAiChatRuntime runtime;

    @BeforeEach
    void setUp() {
        modelFactory = mock(SpringAiModelFactory.class);
        traceSupport = mock(RagStreamTraceSupport.class);
        span = mock(RagStreamTraceSupport.StreamSpan.class);
        chatModel = mock(OpenAiChatModel.class);
        when(modelFactory.chatModel(any())).thenReturn(chatModel);
        when(traceSupport.beginStreamNode(anyString(), anyString())).thenReturn(span);
        Executor directExecutor = Runnable::run;
        runtime = new SpringAiChatRuntime(
                new RagentAiRuntimeProperties(), modelFactory, traceSupport, directExecutor);
    }

    @Test
    void 流式内容和完成事件只转发一次() {
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.just(response("A"), response("B")));
        StreamCallback callback = mock(StreamCallback.class);

        runtime.streamChat("test", request(), callback, target());

        verify(callback).onContent("A");
        verify(callback).onContent("B");
        verify(callback, times(1)).onComplete();
        verify(callback, never()).onError(any());
        verify(span, times(1)).finishSuccess();
        verify(span, times(1)).detach();
    }

    @Test
    void 重复取消只终结一次且不发送完成事件() {
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.never());
        StreamCallback callback = mock(StreamCallback.class);

        StreamCancellationHandle handle = runtime.streamChat("test", request(), callback, target());
        handle.cancel();
        handle.cancel();

        verify(callback, never()).onComplete();
        verify(callback, never()).onError(any());
        verify(span, times(1)).finishCancelledIfRunning();
    }

    @Test
    void 上游错误只发送一次错误终态() {
        IllegalStateException failure = new IllegalStateException("failed");
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.error(failure));
        StreamCallback callback = mock(StreamCallback.class);

        runtime.streamChat("test", request(), callback, target());

        verify(callback, times(1)).onError(failure);
        verify(callback, never()).onComplete();
        verify(span, times(1)).finishError(failure);
    }

    private ChatResponse response(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    private ChatRequest request() {
        return ChatRequest.builder().messages(List.of(ChatMessage.user("question"))).build();
    }

    private ModelTarget target() {
        AIModelProperties.ProviderConfig provider = new AIModelProperties.ProviderConfig();
        provider.setUrl("https://example.com");
        provider.setApiKey("test-key");
        provider.setEndpoints(java.util.Map.of("chat", "/v1/chat/completions"));
        AIModelProperties.ModelCandidate candidate = new AIModelProperties.ModelCandidate();
        candidate.setProvider("test");
        candidate.setModel("test-model");
        return new ModelTarget("test-model", candidate, provider, 1000L);
    }
}
