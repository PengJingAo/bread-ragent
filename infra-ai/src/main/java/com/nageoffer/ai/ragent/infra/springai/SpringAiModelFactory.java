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

import com.nageoffer.ai.ragent.infra.enums.ModelCapability;
import com.nageoffer.ai.ragent.infra.model.ModelTarget;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 根据现有 ModelTarget 动态创建 Spring AI 模型。
 *
 * <p>缓存键包含所有连接与模型参数，配置变化后会自然生成新实例，不会复用旧凭据。
 */
@Component
@RequiredArgsConstructor
public class SpringAiModelFactory {

    private static final String LOCAL_PLACEHOLDER_API_KEY = "local-no-api-key";

    private final ObservationRegistry observationRegistry;
    private final Map<ModelKey, OpenAiChatModel> chatModels = new ConcurrentHashMap<>();
    private final Map<ModelKey, OpenAiEmbeddingModel> embeddingModels = new ConcurrentHashMap<>();

    public OpenAiChatModel chatModel(ModelTarget target) {
        SpringAiEndpoint endpoint = SpringAiEndpoint.from(target, ModelCapability.CHAT);
        ModelKey key = ModelKey.from(target, endpoint);
        return chatModels.computeIfAbsent(key, ignored -> buildChatModel(target, endpoint));
    }

    public OpenAiEmbeddingModel embeddingModel(ModelTarget target) {
        SpringAiEndpoint endpoint = SpringAiEndpoint.from(target, ModelCapability.EMBEDDING);
        ModelKey key = ModelKey.from(target, endpoint);
        return embeddingModels.computeIfAbsent(key, ignored -> buildEmbeddingModel(target, endpoint));
    }

    private OpenAiChatModel buildChatModel(ModelTarget target, SpringAiEndpoint endpoint) {
        OpenAiApi api = buildApi(target, endpoint, true);
        OpenAiChatOptions defaults = OpenAiChatOptions.builder()
                .model(target.candidate().getModel())
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(defaults)
                .observationRegistry(observationRegistry)
                .build();
    }

    private OpenAiEmbeddingModel buildEmbeddingModel(ModelTarget target, SpringAiEndpoint endpoint) {
        OpenAiApi api = buildApi(target, endpoint, false);
        OpenAiEmbeddingOptions defaults = OpenAiEmbeddingOptions.builder()
                .model(target.candidate().getModel())
                .dimensions(target.candidate().getDimension())
                .encodingFormat("float")
                .build();
        return new OpenAiEmbeddingModel(api, MetadataMode.NONE, defaults);
    }

    private OpenAiApi buildApi(ModelTarget target, SpringAiEndpoint endpoint, boolean chat) {
        String configuredApiKey = target.provider() == null ? null : target.provider().getApiKey();
        String apiKey = StringUtils.hasText(configuredApiKey) ? configuredApiKey : LOCAL_PLACEHOLDER_API_KEY;
        OpenAiApi.Builder builder = OpenAiApi.builder().baseUrl(endpoint.baseUrl()).apiKey(apiKey);
        configureTimeout(builder, target.timeoutMs());
        if (chat) {
            builder.completionsPath(endpoint.path());
        } else {
            builder.embeddingsPath(endpoint.path());
        }
        return builder.build();
    }

    /**
     * 同步调用沿用档位超时预算，流式调用只限制连接建立；首包超时仍由现有路由层统一控制，
     * 避免把长答案的总生成时间误当成首包超时。
     */
    private void configureTimeout(OpenAiApi.Builder builder, Long timeoutMs) {
        if (timeoutMs == null || timeoutMs <= 0) {
            return;
        }
        Duration timeout = Duration.ofMillis(timeoutMs);
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);
        builder.restClientBuilder(RestClient.builder().requestFactory(requestFactory));

        JdkClientHttpConnector connector = new JdkClientHttpConnector(httpClient);
        builder.webClientBuilder(WebClient.builder().clientConnector(connector));
    }

    private record ModelKey(String id, String provider, String model, String baseUrl, String path, String apiKey,
                            Integer dimension, Long timeoutMs) {

        private static ModelKey from(ModelTarget target, SpringAiEndpoint endpoint) {
            String apiKey = target.provider() == null ? null : target.provider().getApiKey();
            return new ModelKey(
                    target.id(),
                    target.candidate().getProvider(),
                    target.candidate().getModel(),
                    endpoint.baseUrl(),
                    endpoint.path(),
                    apiKey,
                    target.candidate().getDimension(),
                    target.timeoutMs()
            );
        }
    }
}
