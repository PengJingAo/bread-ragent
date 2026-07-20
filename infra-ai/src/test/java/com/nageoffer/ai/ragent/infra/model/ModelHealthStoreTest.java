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

package com.nageoffer.ai.ragent.infra.model;

import com.nageoffer.ai.ragent.infra.config.AIModelProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModelHealthStoreTest {

    private static final String MODEL_ID = "test-model";

    @Test
    @DisplayName("旧成功结果不应覆盖更新的失败熔断状态")
    void shouldIgnoreStaleSuccessAfterNewerFailure() {
        // Arrange
        ModelHealthStore healthStore = newHealthStore(1, 30000L);
        ModelHealthStore.CallPermit oldPermit = healthStore.acquireCall(MODEL_ID);
        ModelHealthStore.CallPermit newerPermit = healthStore.acquireCall(MODEL_ID);

        // Act
        healthStore.markFailure(newerPermit);
        healthStore.markSuccess(oldPermit);

        // Assert
        assertThat(healthStore.isUnavailable(MODEL_ID))
                .as("旧请求的成功结果不能把较新的失败熔断状态重置为健康")
                .isTrue();
    }

    @Test
    @DisplayName("旧失败结果不应覆盖更新的成功恢复状态")
    void shouldIgnoreStaleFailureAfterNewerSuccess() {
        // Arrange
        ModelHealthStore healthStore = newHealthStore(1, 30000L);
        ModelHealthStore.CallPermit oldPermit = healthStore.acquireCall(MODEL_ID);
        ModelHealthStore.CallPermit newerPermit = healthStore.acquireCall(MODEL_ID);

        // Act
        healthStore.markSuccess(newerPermit);
        healthStore.markFailure(oldPermit);

        // Assert
        assertThat(healthStore.isUnavailable(MODEL_ID))
                .as("旧请求的失败结果不能把较新的成功恢复状态重新熔断")
                .isFalse();
    }

    @Test
    @DisplayName("半开状态同一时间只允许一个试探请求")
    void shouldAllowOnlyOneHalfOpenProbe() {
        // Arrange
        ModelHealthStore healthStore = newHealthStore(1, 0L);
        ModelHealthStore.CallPermit failurePermit = healthStore.acquireCall(MODEL_ID);
        healthStore.markFailure(failurePermit);

        // Act
        ModelHealthStore.CallPermit firstProbe = healthStore.acquireCall(MODEL_ID);
        ModelHealthStore.CallPermit secondProbe = healthStore.acquireCall(MODEL_ID);

        // Assert
        assertThat(firstProbe.allowed())
                .as("熔断冷却结束后，第一个请求应获得半开试探资格")
                .isTrue();
        assertThat(secondProbe.allowed())
                .as("已有半开试探请求时，第二个并发请求应被拒绝")
                .isFalse();
    }

    private static ModelHealthStore newHealthStore(int failureThreshold, long openDurationMs) {
        AIModelProperties properties = new AIModelProperties();
        properties.getSelection().setFailureThreshold(failureThreshold);
        properties.getSelection().setOpenDurationMs(openDurationMs);
        return new ModelHealthStore(properties);
    }
}
