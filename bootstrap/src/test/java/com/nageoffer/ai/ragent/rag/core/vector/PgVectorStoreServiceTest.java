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

package com.nageoffer.ai.ragent.rag.core.vector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nageoffer.ai.ragent.core.chunk.VectorChunk;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;

import java.sql.PreparedStatement;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PgVectorStoreServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void 写入向量时同时填充物理Collection列和元数据() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        PgVectorStoreService service = new PgVectorStoreService(jdbcTemplate, new ObjectMapper());
        VectorChunk chunk = VectorChunk.builder()
                .chunkId("chunk-1")
                .index(0)
                .content("商品说明")
                .embedding(new float[]{0.1F, 0.2F})
                .build();

        doAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (!sql.contains("collection_name")) {
                throw new AssertionError("INSERT 必须显式写入 collection_name");
            }
            ParameterizedPreparedStatementSetter<VectorChunk> setter = invocation.getArgument(3);
            setter.setValues(statement, chunk);
            return new int[][]{{1}};
        }).when(jdbcTemplate).batchUpdate(anyString(), anyList(), anyInt(), any());

        service.indexDocumentChunks("kb-product", "doc-1", List.of(chunk));

        verify(statement).setString(1, "chunk-1");
        verify(statement).setString(2, "kb-product");
        verify(statement).setString(3, "商品说明");
        verify(statement).setString(4,
                "{\"collection_name\":\"kb-product\",\"doc_id\":\"doc-1\",\"chunk_index\":0}");
        verify(statement).setString(5, "[0.1,0.2]");
    }
}
