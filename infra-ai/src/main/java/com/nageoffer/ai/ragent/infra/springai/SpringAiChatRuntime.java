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

import cn.hutool.core.util.StrUtil;
import com.nageoffer.ai.ragent.framework.convention.ChatMessage;
import com.nageoffer.ai.ragent.framework.convention.ChatRequest;
import com.nageoffer.ai.ragent.framework.trace.RagStreamTraceSupport;
import com.nageoffer.ai.ragent.infra.chat.StreamCallback;
import com.nageoffer.ai.ragent.infra.chat.StreamCancellationHandle;
import com.nageoffer.ai.ragent.infra.config.RagentAiRuntimeProperties;
import com.nageoffer.ai.ragent.infra.config.RagentAiRuntimeProperties.RuntimeMode;
import com.nageoffer.ai.ragent.infra.model.ModelTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Spring AI ChatModel 与项目现有同步/流式协议之间的适配器。
 *
 * <p>该类不会改变模型选择与降级策略；RoutingLLMService 仍然负责这些业务规则。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpringAiChatRuntime {

    private static final List<String> REASONING_METADATA_KEYS = List.of(
            "reasoning_content", "reasoningContent", "reasoning");

    private final RagentAiRuntimeProperties runtimeProperties;
    private final SpringAiModelFactory modelFactory;
    private final RagStreamTraceSupport streamTraceSupport;
    private final Executor modelStreamExecutor;

    public RuntimeMode mode(String provider) {
        return runtimeProperties.resolve(provider);
    }

    public String chat(ChatRequest request, ModelTarget target) {
        ChatResponse response = modelFactory.chatModel(target).call(toPrompt(request, target));
        String content = extractContent(response);
        if (StrUtil.isBlank(content)) {
            throw new IllegalStateException("Spring AI 模型响应内容为空: " + target.id());
        }
        return content;
    }

    public StreamCancellationHandle streamChat(
            String provider, ChatRequest request, StreamCallback callback, ModelTarget target) {
        OpenAiChatModel model = modelFactory.chatModel(target);
        AtomicBoolean cancelled = new AtomicBoolean(false);
        AtomicBoolean terminated = new AtomicBoolean(false);
        AtomicReference<reactor.core.Disposable> subscription = new AtomicReference<>();
        RagStreamTraceSupport.StreamSpan span =
                streamTraceSupport.beginStreamNode(provider + "-spring-ai-stream-chat", "LLM_PROVIDER");
        try {
            reactor.core.Disposable disposable = model.stream(toPrompt(request, target)).subscribe(
                    response -> forward(response, callback, cancelled),
                    error -> terminateWithError(callback, error, cancelled, terminated, span),
                    () -> terminateNormally(callback, cancelled, terminated, span)
            );
            subscription.set(disposable);
        } finally {
            span.detach();
        }
        return () -> {
            if (!cancelled.compareAndSet(false, true)) {
                return;
            }
            reactor.core.Disposable disposable = subscription.get();
            if (disposable != null) {
                disposable.dispose();
            }
            if (terminated.compareAndSet(false, true)) {
                span.finishCancelledIfRunning();
            }
        };
    }

    /** Shadow 模式只记录差异，不把 Spring AI 结果发送给用户。 */
    public void shadowChat(ChatRequest request, ModelTarget target, String legacyResult) {
        CompletableFuture.runAsync(() -> {
            try {
                String springAiResult = chat(request, target);
                log.info(
                        "Spring AI shadow 对比完成, modelId={}, exactMatch={}, legacyLength={}, springAiLength={}",
                        target.id(), Objects.equals(legacyResult, springAiResult), safeLength(legacyResult),
                        safeLength(springAiResult));
            } catch (Exception ex) {
                log.warn("Spring AI shadow 调用失败，不影响 Legacy 返回, modelId={}", target.id(), ex);
            }
        }, modelStreamExecutor);
    }

    /** 流式 Shadow 只执行旁路完整调用，旧流仍是唯一用户输出。 */
    public void shadowStream(ChatRequest request, ModelTarget target) {
        CompletableFuture.runAsync(() -> {
            try {
                String result = chat(request, target);
                log.info("Spring AI stream shadow 调用完成, modelId={}, resultLength={}", target.id(), safeLength(result));
            } catch (Exception ex) {
                log.warn("Spring AI stream shadow 调用失败，不影响 Legacy 流, modelId={}", target.id(), ex);
            }
        }, modelStreamExecutor);
    }

    private Prompt toPrompt(ChatRequest request, ModelTarget target) {
        List<Message> messages = new ArrayList<>();
        if (request.getMessages() != null) {
            for (ChatMessage message : request.getMessages()) {
                if (message == null || message.getRole() == null) {
                    continue;
                }
                String content = StrUtil.nullToEmpty(message.getContent());
                messages.add(switch (message.getRole()) {
                    case SYSTEM -> new SystemMessage(content);
                    case USER -> new UserMessage(content);
                    case ASSISTANT -> new AssistantMessage(content);
                });
            }
        }

        OpenAiChatOptions.Builder options = OpenAiChatOptions.builder().model(target.candidate().getModel());
        if (request.getTemperature() != null) {
            options.temperature(request.getTemperature());
        }
        if (request.getTopP() != null) {
            options.topP(request.getTopP());
        }
        if (request.getMaxTokens() != null) {
            options.maxTokens(request.getMaxTokens());
        }
        Map<String, Object> extraBody = new HashMap<>();
        if (request.getTopK() != null) {
            extraBody.put("top_k", request.getTopK());
        }
        if (Boolean.TRUE.equals(request.getThinking())) {
            extraBody.put("enable_thinking", true);
        }
        if (!extraBody.isEmpty()) {
            options.extraBody(extraBody);
        }
        return new Prompt(messages, options.build());
    }

    private void forward(ChatResponse response, StreamCallback callback, AtomicBoolean cancelled) {
        if (cancelled.get() || response == null || response.getResult() == null) {
            return;
        }
        AssistantMessage output = response.getResult().getOutput();
        if (output == null) {
            return;
        }
        String reasoning = extractReasoning(output.getMetadata());
        if (StrUtil.isNotBlank(reasoning)) {
            callback.onThinking(reasoning);
        }
        if (StrUtil.isNotBlank(output.getText())) {
            callback.onContent(output.getText());
        }
    }

    private String extractContent(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return null;
        }
        return response.getResult().getOutput().getText();
    }

    private String extractReasoning(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        for (String key : REASONING_METADATA_KEYS) {
            Object value = metadata.get(key);
            if (value != null && StrUtil.isNotBlank(value.toString())) {
                return value.toString();
            }
        }
        return null;
    }

    private void terminateNormally(
            StreamCallback callback,
            AtomicBoolean cancelled,
            AtomicBoolean terminated,
            RagStreamTraceSupport.StreamSpan span) {
        if (!cancelled.get() && terminated.compareAndSet(false, true)) {
            try {
                callback.onComplete();
            } finally {
                span.finishSuccess();
            }
        }
    }

    private void terminateWithError(
            StreamCallback callback,
            Throwable error,
            AtomicBoolean cancelled,
            AtomicBoolean terminated,
            RagStreamTraceSupport.StreamSpan span) {
        if (!cancelled.get() && terminated.compareAndSet(false, true)) {
            try {
                callback.onError(error);
            } finally {
                span.finishError(error);
            }
        }
    }

    private int safeLength(String value) {
        return value == null ? 0 : value.length();
    }
}
