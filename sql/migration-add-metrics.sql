-- 聚类评价指标扩展迁移
-- 新增 Davies-Bouldin Index 和 Calinski-Harabasz Index 字段

USE student_quality_evaluation;

ALTER TABLE cluster_result
    ADD COLUMN IF NOT EXISTS davies_bouldin_index DECIMAL(8,4) COMMENT 'Davies-Bouldin指数(越低越好)',
    ADD COLUMN IF NOT EXISTS calinski_harabasz_index DECIMAL(8,4) COMMENT 'Calinski-Harabasz指数(越高越好)';
