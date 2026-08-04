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

package com.nageoffer.ai.ragent.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring AI 渐进式迁移开关。
 *
 * <p>全局开关控制默认运行时，provider-runtime 可以对单个模型提供商覆盖，便于灰度和即时回退。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ragent.ai")
public class RagentAiRuntimeProperties {

    /** 全局默认运行时。 */
    private RuntimeMode runtime = RuntimeMode.LEGACY;

    /** 提供商级运行时覆盖，key 与 ModelProvider.id 保持一致。 */
    private Map<String, RuntimeMode> providerRuntime = new HashMap<>();

    /**
     * 返回指定提供商的有效运行模式。
     *
     * @param provider 提供商标识
     * @return 提供商覆盖值或全局默认值
     */
    public RuntimeMode resolve(String provider) {
        if (provider == null) {
            return runtime;
        }
        return providerRuntime.getOrDefault(provider.toLowerCase(), runtime);
    }

    /** AI 基础设施运行模式。 */
    public enum RuntimeMode {
        LEGACY,
        SPRING_AI,
        SHADOW
    }
}
