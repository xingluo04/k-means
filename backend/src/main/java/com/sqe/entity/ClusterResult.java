package com.sqe.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 聚类分析结果实体
 */
@Data
@TableName("cluster_result")
public class ClusterResult {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String academicYear;
    private Integer clusterCount;
    private BigDecimal silhouetteScore;
    private BigDecimal daviesBouldinIndex;
    private BigDecimal calinskiHarabaszIndex;
    private Integer clusterLabel;
    private String clusterName;
    private Integer studentCount;
    private BigDecimal avgMoral;
    private BigDecimal avgAcademic;
    private BigDecimal avgPhysical;
    private BigDecimal avgArt;
    private BigDecimal avgPractice;
    private String description;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
