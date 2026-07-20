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

import cn.hutool.core.collection.CollUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nageoffer.ai.ragent.core.chunk.VectorChunk;
import com.nageoffer.ai.ragent.rag.config.KeywordProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 关键词索引历史数据回灌任务。
 * <p>
 * ES 关键词索引是可派生索引，主数据仍以 PG 向量表为准；该任务仅在显式开启
 * rag.keyword.type=es 且 rag.keyword.backfill.enabled=true 时启动，用于测评前把历史 chunk 补写到 ES。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "rag.keyword", name = "type", havingValue = "es")
@ConditionalOnProperty(prefix = "rag.keyword.backfill", name = "enabled", havingValue = "true")
public class KeywordIndexBackfillRunner implements ApplicationRunner {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ElasticsearchClient esClient;
    private final ObjectMapper objectMapper;
    private final KeywordProperties keywordProperties;
    private final KeywordIndexService keywordIndexService;

    @Override
    public void run(ApplicationArguments args) {
        KeywordProperties.Backfill backfill = keywordProperties.getBackfill();
        try {
            doBackfill(backfill);
        } catch (Exception e) {
            if (backfill.isFailOnError()) {
                throw new IllegalStateException("关键词索引回灌失败", e);
            }
            log.error("关键词索引回灌失败，已按配置继续启动", e);
        }
    }

    private void doBackfill(KeywordProperties.Backfill backfill) throws Exception {
        if (backfill.isRecreateIndex()) {
            recreateSharedIndex();
        }

        int batchSize = Math.max(1, backfill.getBatchSize());
        long total = countBackfillableRows();
        long offset = 0;
        long indexed = 0;
        log.info("关键词索引回灌开始, index={}, pgRows={}, batchSize={}", keywordProperties.sharedIndex(), total, batchSize);

        while (true) {
            List<BackfillRow> rows = loadRows(batchSize, offset);
            if (CollUtil.isEmpty(rows)) {
                break;
            }
            indexed += indexRows(rows);
            offset += rows.size();
            log.info("关键词索引回灌进度, indexed={}, pgRows={}", indexed, total);
        }

        log.info("关键词索引回灌完成, index={}, indexed={}, pgRows={}", keywordProperties.sharedIndex(), indexed, total);
    }

    private void recreateSharedIndex() throws Exception {
        String index = keywordProperties.sharedIndex();
        esClient.indices().delete(d -> d.index(index).ignoreUnavailable(true).allowNoIndices(true));
        log.info("ES 关键词共享索引已删除，准备全量重建, index={}", index);
    }

    private long countBackfillableRows() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_knowledge_vector WHERE jsonb_exists(metadata, 'collection_name')",
                Long.class);
        return count == null ? 0L : count;
    }

    private List<BackfillRow> loadRows(int batchSize, long offset) {
        return jdbcTemplate.query(
                "SELECT id, content, metadata::text AS metadata FROM t_knowledge_vector " +
                        "WHERE jsonb_exists(metadata, 'collection_name') ORDER BY id LIMIT ? OFFSET ?",
                (rs, rowNum) -> toBackfillRow(rs),
                batchSize,
                offset);
    }

    private BackfillRow toBackfillRow(ResultSet rs) throws SQLException {
        String metadataJson = rs.getString("metadata");
        Map<String, Object> metadata = parseMetadata(metadataJson);
        String collectionName = stringValue(metadata.get("collection_name"));
        String docId = stringValue(metadata.get("doc_id"));
        Integer chunkIndex = intValue(metadata.get("chunk_index"));

        VectorChunk chunk = VectorChunk.builder()
                .chunkId(rs.getString("id"))
                .content(rs.getString("content"))
                .index(chunkIndex)
                .metadata(metadata)
                .blockType(firstText(metadata, "block_type", "blockType"))
                .outlinePath(stringListValue(firstValue(metadata, "outline_path", "outlinePath")))
                .sectionContext(firstText(metadata, "section_context", "sectionContext"))
                .sourceBlockIds(stringListValue(firstValue(metadata, "source_block_ids", "sourceBlockIds")))
                .build();

        return new BackfillRow(collectionName, docId, chunk);
    }

    private Map<String, Object> parseMetadata(String metadataJson) {
        if (!StringUtils.hasText(metadataJson)) {
            return new LinkedHashMap<>();
        }
        try {
            return new LinkedHashMap<>(objectMapper.readValue(metadataJson, MAP_TYPE));
        } catch (Exception e) {
            throw new IllegalArgumentException("PG 向量元数据解析失败", e);
        }
    }

    private long indexRows(List<BackfillRow> rows) {
        Map<DocumentKey, List<VectorChunk>> chunksByDocument = new LinkedHashMap<>();
        for (BackfillRow row : rows) {
            if (!StringUtils.hasText(row.collectionName())) {
                log.warn("跳过缺少 collection_name 的历史 chunk, chunkId={}", row.chunk().getChunkId());
                continue;
            }
            String docId = StringUtils.hasText(row.docId()) ? row.docId() : row.chunk().getChunkId();
            DocumentKey key = new DocumentKey(row.collectionName(), docId);
            chunksByDocument.computeIfAbsent(key, ignored -> new ArrayList<>()).add(row.chunk());
        }

        long indexed = 0;
        for (Map.Entry<DocumentKey, List<VectorChunk>> entry : chunksByDocument.entrySet()) {
            DocumentKey key = entry.getKey();
            List<VectorChunk> chunks = entry.getValue();
            keywordIndexService.indexDocumentChunks(key.collectionName(), key.docId(), chunks);
            indexed += chunks.size();
        }
        return indexed;
    }

    private String firstText(Map<String, Object> metadata, String... keys) {
        Object value = firstValue(metadata, keys);
        return stringValue(value);
    }

    private Object firstValue(Map<String, Object> metadata, String... keys) {
        for (String key : keys) {
            if (metadata.containsKey(key)) {
                return metadata.get(key);
            }
        }
        return null;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Integer intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = stringValue(value);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<String> stringListValue(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return new ArrayList<>();
    }

    private record BackfillRow(String collectionName, String docId, VectorChunk chunk) {
    }

    private record DocumentKey(String collectionName, String docId) {
    }
}
