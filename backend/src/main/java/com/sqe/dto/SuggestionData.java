package com.sqe.dto;

import lombok.Data;
import java.util.List;

/**
 * 结构化个性化发展建议 DTO — 序列化为 JSON 存入 suggestion 字段
 */
@Data
public class SuggestionData {
    private String summary;
    private String clusterName;
    private List<DimAdvice> strengths;
    private List<DimAdvice> weaknesses;
    private List<String> actionItems;
    private TrendInfo trend;
    private PromotionPath promotion;
    private Typicality typicality;
    private List<DimRank> percentileRanks;
    private String severityLevel;

    @Data
    public static class DimAdvice {
        private String dimKey;      // moral/academic/physical/art/practice
        private String dimLabel;    // 德育/智育/体育/美育/劳动教育
        private double score;
        private double percentile;  // 年级百分比排名(0-100)
        private int rank;           // 绝对排名(1-based)
        private int total;          // 总人数
        private String level;       // critical/warning/normal/good/excellent
        private String levelLabel;  // 等级中文标签
        private String suggestion;  // 针对该维度的具体建议
    }

    @Data
    public static class TrendInfo {
        private String prevYear;           // 上一学年
        private double totalChange;        // 综合总分变化
        private List<DimChange> dimChanges; // 各维度变化
        private String clusterChangedFrom; // 原聚类类别(若有变化)
        private String clusterChangedTo;   // 当前聚类类别
        private String highlight;          // 趋势亮点描述
    }

    @Data
    public static class DimChange {
        private String dimKey;
        private String dimLabel;
        private double change;      // 变化值，正为上升
        private boolean improved;   // 是否提升
    }

    @Data
    public static class PromotionPath {
        private String targetClusterName;  // 目标聚类名称
        private List<DimGap> gaps;         // 需提升的维度差距
        private String description;        // 晋升路径一句话描述
    }

    @Data
    public static class DimGap {
        private String dimKey;
        private String dimLabel;
        private double currentScore;
        private double targetScore;
        private double gap;
    }

    @Data
    public static class Typicality {
        private String label;       // 典型代表/一般成员/特殊个体
        private double ratio;       // distRatio
        private String description;
        private String outlierDim;  // 偏离最远的维度(仅特殊个体有值)
        private String outlierDirection; // above/below
    }

    @Data
    public static class DimRank {
        private String dimKey;
        private String dimLabel;
        private double percentile; // 0-100
        private int rank;
        private int total;
    }
}
