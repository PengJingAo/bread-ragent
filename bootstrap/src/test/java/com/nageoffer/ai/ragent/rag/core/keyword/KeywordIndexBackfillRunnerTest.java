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

package com.nageoffer.ai.ragent.rag.core.keyword;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nageoffer.ai.ragent.core.chunk.model.EmbeddedChunk;
import com.nageoffer.ai.ragent.rag.config.KeywordProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KeywordIndexBackfillRunnerTest {

    @Test
    @SuppressWarnings("unchecked")
    void 使用物理库名解析向量并按文档分组且缺失DocId回退ChunkId() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        KeywordIndexService keywordIndexService = mock(KeywordIndexService.class);
        KeywordProperties properties = properties(true, 3);
        ResultSet first = row("chunk-1", "kb-physical", "正文一",
                "{\"collection_name\":\"kb-stale\",\"doc_id\":\"doc-a\",\"chunk_index\":1,\"section_context\":\"章节 A\"}",
                "[0.1, 0.2]");
        ResultSet second = row("chunk-2", "kb-physical", "正文二",
                "{\"doc_id\":\"doc-a\",\"chunk_index\":2}", "[0.3,0.4]");
        ResultSet fallback = row("chunk-3", "kb-other", "正文三",
                "{\"chunk_index\":0}", "[-0.5,6e-1]");
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(3L);
        AtomicInteger batch = new AtomicInteger();
        doAnswer(invocation -> {
            if (batch.getAndIncrement() > 0) {
                return List.of();
            }
            RowMapper<Object> mapper = invocation.getArgument(1);
            return List.of(mapper.mapRow(first, 0), mapper.mapRow(second, 1), mapper.mapRow(fallback, 2));
        }).when(jdbcTemplate).query(anyString(), any(RowMapper.class), any(Object[].class));

        runner(jdbcTemplate, properties, keywordIndexService).run(mock(ApplicationArguments.class));

        ArgumentCaptor<String> collectionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> docCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<EmbeddedChunk>> chunksCaptor = ArgumentCaptor.forClass(List.class);
        verify(keywordIndexService, times(2)).indexDocumentChunks(
                collectionCaptor.capture(), docCaptor.capture(), chunksCaptor.capture());
        assertEquals(List.of("kb-physical", "kb-other"), collectionCaptor.getAllValues());
        assertEquals(List.of("doc-a", "chunk-3"), docCaptor.getAllValues());
        assertEquals(2, chunksCaptor.getAllValues().get(0).size(), "同一批次内相同文档的块应合并写入");
        EmbeddedChunk migrated = chunksCaptor.getAllValues().get(0).get(0);
        assertEquals("章节 A" + System.lineSeparator() + "正文一", migrated.embeddingText());
        assertArrayEquals(new float[]{0.1F, 0.2F}, migrated.embedding());
        assertArrayEquals(new float[]{-0.5F, 0.6F}, chunksCaptor.getAllValues().get(1).get(0).embedding());
        verify(jdbcTemplate, times(2)).query(anyString(), any(RowMapper.class), any(Object[].class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void 非法向量在FailOnError开启时中断启动() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        KeywordIndexService keywordIndexService = mock(KeywordIndexService.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(1L);
        ResultSet invalid = row("chunk-invalid", "kb-a", "正文", "{\"doc_id\":\"doc-a\"}", "[bad]");
        doAnswer(invocation -> {
            RowMapper<Object> mapper = invocation.getArgument(1);
            return List.of(mapper.mapRow(invalid, 0));
        }).when(jdbcTemplate).query(anyString(), any(RowMapper.class), any(Object[].class));

        assertThrows(IllegalStateException.class,
                () -> runner(jdbcTemplate, properties(true, 10), keywordIndexService)
                        .run(mock(ApplicationArguments.class)));
        verify(keywordIndexService, never()).indexDocumentChunks(anyString(), anyString(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void 非法向量在FailOnError关闭时记录后继续启动() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        KeywordIndexService keywordIndexService = mock(KeywordIndexService.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(1L);
        ResultSet invalid = row("chunk-invalid", "kb-a", "正文", "{}", "[]");
        doAnswer(invocation -> {
            RowMapper<Object> mapper = invocation.getArgument(1);
            return List.of(mapper.mapRow(invalid, 0));
        }).when(jdbcTemplate).query(anyString(), any(RowMapper.class), any(Object[].class));

        assertDoesNotThrow(() -> runner(jdbcTemplate, properties(false, 10), keywordIndexService)
                .run(mock(ApplicationArguments.class)));
        verify(keywordIndexService, never()).indexDocumentChunks(anyString(), anyString(), any());
    }

    private KeywordIndexBackfillRunner runner(JdbcTemplate jdbcTemplate, KeywordProperties properties,
                                               KeywordIndexService keywordIndexService) {
        return new KeywordIndexBackfillRunner(
                jdbcTemplate,
                mock(ElasticsearchClient.class),
                new ObjectMapper(),
                properties,
                keywordIndexService);
    }

    private KeywordProperties properties(boolean failOnError, int batchSize) {
        KeywordProperties properties = new KeywordProperties();
        properties.getBackfill().setRecreateIndex(false);
        properties.getBackfill().setFailOnError(failOnError);
        properties.getBackfill().setBatchSize(batchSize);
        return properties;
    }

    private ResultSet row(String id, String collectionName, String content,
                          String metadata, String embedding) throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("id")).thenReturn(id);
        when(resultSet.getString("collection_name")).thenReturn(collectionName);
        when(resultSet.getString("content")).thenReturn(content);
        when(resultSet.getString("metadata")).thenReturn(metadata);
        when(resultSet.getString("embedding")).thenReturn(embedding);
        return resultSet;
    }
}
