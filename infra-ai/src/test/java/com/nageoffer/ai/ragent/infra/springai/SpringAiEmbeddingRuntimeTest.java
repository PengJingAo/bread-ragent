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

import com.nageoffer.ai.ragent.infra.config.AIModelProperties;
import com.nageoffer.ai.ragent.infra.config.RagentAiRuntimeProperties;
import com.nageoffer.ai.ragent.infra.model.ModelTarget;
import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.OpenAiEmbeddingModel;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringAiEmbeddingRuntimeTest {

    @Test
    void 保持输入顺序并转换浮点向量() {
        SpringAiModelFactory factory = mock(SpringAiModelFactory.class);
        OpenAiEmbeddingModel model = mock(OpenAiEmbeddingModel.class);
        when(factory.embeddingModel(any())).thenReturn(model);
        when(model.embed(List.of("a", "b"))).thenReturn(List.of(new float[]{1F, 0F}, new float[]{0F, 1F}));
        SpringAiEmbeddingRuntime runtime = new SpringAiEmbeddingRuntime(
                new RagentAiRuntimeProperties(), factory, Runnable::run);

        List<List<Float>> result = runtime.embed(List.of("a", "b"), target(2));

        assertEquals(List.of(List.of(1F, 0F), List.of(0F, 1F)), result);
    }

    @Test
    void 向量维度变化时拒绝结果() {
        SpringAiModelFactory factory = mock(SpringAiModelFactory.class);
        OpenAiEmbeddingModel model = mock(OpenAiEmbeddingModel.class);
        when(factory.embeddingModel(any())).thenReturn(model);
        when(model.embed(List.of("a"))).thenReturn(List.of(new float[]{1F}));
        SpringAiEmbeddingRuntime runtime = new SpringAiEmbeddingRuntime(
                new RagentAiRuntimeProperties(), factory, Runnable::run);

        assertThrows(IllegalStateException.class, () -> runtime.embed(List.of("a"), target(2)));
    }

    private ModelTarget target(int dimension) {
        AIModelProperties.ModelCandidate candidate = new AIModelProperties.ModelCandidate();
        candidate.setProvider("test");
        candidate.setModel("embedding-model");
        candidate.setDimension(dimension);
        return new ModelTarget("embedding-model", candidate, new AIModelProperties.ProviderConfig(), 1000L);
    }
}
