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

import com.nageoffer.ai.ragent.infra.config.RagentAiRuntimeProperties;
import com.nageoffer.ai.ragent.infra.config.RagentAiRuntimeProperties.RuntimeMode;
import com.nageoffer.ai.ragent.infra.model.ModelTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Spring AI EmbeddingModel 与项目现有浮点列表协议之间的适配器。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpringAiEmbeddingRuntime {

    private final RagentAiRuntimeProperties runtimeProperties;
    private final SpringAiModelFactory modelFactory;
    private final Executor modelStreamExecutor;

    public RuntimeMode mode(String provider) {
        return runtimeProperties.resolve(provider);
    }

    public List<List<Float>> embed(List<String> texts, ModelTarget target) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }
        List<float[]> vectors = modelFactory.embeddingModel(target).embed(texts);
        if (vectors.size() != texts.size()) {
            throw new IllegalStateException(
                    "Spring AI Embedding 数量与输入不一致: expected=" + texts.size() + ", actual=" + vectors.size());
        }
        List<List<Float>> result = new ArrayList<>(vectors.size());
        for (float[] vector : vectors) {
            validateDimension(vector, target);
            List<Float> converted = new ArrayList<>(vector.length);
            for (float value : vector) {
                converted.add(value);
            }
            result.add(converted);
        }
        return result;
    }

    /** Shadow 模式比较维度与余弦相似度，不记录向量内容。 */
    public void shadowEmbed(List<String> texts, ModelTarget target, List<List<Float>> legacyResult) {
        List<String> inputSnapshot = List.copyOf(texts);
        CompletableFuture.runAsync(() -> {
            try {
                List<List<Float>> springAiResult = embed(inputSnapshot, target);
                SimilaritySummary similarity = similaritySummary(legacyResult, springAiResult);
                log.info(
                        "Spring AI embedding shadow 对比完成, modelId={}, inputSize={}, legacySize={}, springAiSize={}, dimensionMatch={}, minCosine={}, avgCosine={}",
                        target.id(), inputSnapshot.size(), legacyResult.size(), springAiResult.size(),
                        dimensionsMatch(legacyResult, springAiResult), similarity.min(), similarity.average());
            } catch (Exception ex) {
                log.warn("Spring AI embedding shadow 调用失败，不影响 Legacy 结果, modelId={}", target.id(), ex);
            }
        }, modelStreamExecutor);
    }

    private void validateDimension(float[] vector, ModelTarget target) {
        Integer expected = target.candidate().getDimension();
        if (vector == null || (expected != null && expected > 0 && vector.length != expected)) {
            int actual = vector == null ? 0 : vector.length;
            throw new IllegalStateException(
                    "Spring AI Embedding 维度不匹配: modelId=" + target.id() + ", expected=" + expected + ", actual=" + actual);
        }
    }

    private boolean dimensionsMatch(List<List<Float>> left, List<List<Float>> right) {
        if (left.size() != right.size()) {
            return false;
        }
        for (int i = 0; i < left.size(); i++) {
            if (left.get(i) == null || right.get(i) == null || left.get(i).size() != right.get(i).size()) {
                return false;
            }
        }
        return true;
    }

    private SimilaritySummary similaritySummary(List<List<Float>> left, List<List<Float>> right) {
        if (!dimensionsMatch(left, right) || left.isEmpty()) {
            return SimilaritySummary.unavailable();
        }
        double min = Double.POSITIVE_INFINITY;
        double sum = 0D;
        for (int i = 0; i < left.size(); i++) {
            double cosine = cosine(left.get(i), right.get(i));
            min = Math.min(min, cosine);
            sum += cosine;
        }
        return new SimilaritySummary(min, sum / left.size());
    }

    private double cosine(List<Float> left, List<Float> right) {
        double dot = 0D;
        double leftNorm = 0D;
        double rightNorm = 0D;
        for (int i = 0; i < left.size(); i++) {
            double leftValue = left.get(i);
            double rightValue = right.get(i);
            dot += leftValue * rightValue;
            leftNorm += leftValue * leftValue;
            rightNorm += rightValue * rightValue;
        }
        if (leftNorm == 0D || rightNorm == 0D) {
            return 0D;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private record SimilaritySummary(Double min, Double average) {

        private static SimilaritySummary unavailable() {
            return new SimilaritySummary(null, null);
        }
    }
}
