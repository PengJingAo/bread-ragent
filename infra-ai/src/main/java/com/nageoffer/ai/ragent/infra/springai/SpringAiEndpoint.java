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
import com.nageoffer.ai.ragent.infra.http.ModelUrlResolver;
import com.nageoffer.ai.ragent.infra.model.ModelTarget;

import java.net.URI;

/** Spring AI OpenAI 客户端所需的基础地址与端点路径。 */
record SpringAiEndpoint(String baseUrl, String path) {

    static SpringAiEndpoint from(ModelTarget target, ModelCapability capability) {
        String resolvedUrl = ModelUrlResolver.resolveUrl(target.provider(), target.candidate(), capability);
        URI uri = URI.create(resolvedUrl);
        String authority = uri.getRawAuthority();
        if (uri.getScheme() == null || authority == null) {
            throw new IllegalArgumentException("模型地址必须是绝对 URL: " + resolvedUrl);
        }
        String baseUrl = uri.getScheme() + "://" + authority;
        String path = uri.getRawPath();
        if (uri.getRawQuery() != null) {
            path += "?" + uri.getRawQuery();
        }
        return new SpringAiEndpoint(baseUrl, path);
    }
}
