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

package com.nageoffer.ai.ragent.rag.springai;

import com.nageoffer.ai.ragent.rag.core.rewrite.QueryRewriteService;
import com.nageoffer.ai.ragent.rag.core.rewrite.RewriteResult;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.rag.Query;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 将项目现有的子问题拆分结果转换为 Spring AI Query 列表。
 *
 * <p>Spring AI 1.1 的 MultiQueryExpander 是具体实现而非接口，因此这里提供等价适配组件，
 * 后续自定义 Advisor 可直接组合使用。</p>
 */
@Component
@RequiredArgsConstructor
public class RagentMultiQueryExpander {

    private final QueryRewriteService queryRewriteService;

    public List<Query> expand(Query query) {
        RewriteResult result = queryRewriteService.rewriteWithSplit(query.text());
        List<String> subQuestions = result.subQuestions();
        if (subQuestions == null || subQuestions.isEmpty()) {
            return List.of(query.mutate().text(result.rewrittenQuestion()).build());
        }
        return subQuestions.stream().map(text -> query.mutate().text(text).build()).toList();
    }
}
