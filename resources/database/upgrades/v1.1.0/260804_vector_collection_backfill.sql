-- v1.1.0 260804 历史向量 Collection 物理列回填
-- collection_name 物理列是查询、删除和关键词回灌的权威字段。
-- 仅修正 metadata 中仍保存有效 collection_name 的历史行；不删除 metadata，不改动向量内容。

UPDATE t_knowledge_vector
SET collection_name = BTRIM(metadata ->> 'collection_name')
WHERE NULLIF(BTRIM(metadata ->> 'collection_name'), '') IS NOT NULL
  AND collection_name IS DISTINCT FROM BTRIM(metadata ->> 'collection_name');
