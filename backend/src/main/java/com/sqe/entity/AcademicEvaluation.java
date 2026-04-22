package com.sqe.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学业素质评价实体
 */
@Data
@TableName("academic_evaluation")
public class AcademicEvaluation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String semester;
    /* 学业表现 */
    private BigDecimal weightedAvgScore;
    private Integer courseCount;
    private Integer retakeCount;
    private Integer minorCount;
    private BigDecimal academicPerformanceScore;
    /* 学科竞赛与科研 */
    private Integer competitionCount;
    private String competitionHighestLevel;
    private Integer paperCount;
    private Integer paperFirstAuthor;
    private Integer researchProjectCount;
    private Integer researchHostCount;
    private Integer techPromotionCount;
    private BigDecimal researchScore;
    /* 扣分事项 */
    private Integer examCheatCount;
    private Integer classViolationCount;
    private BigDecimal academicDeduction;
    /* 汇总 */
    private BigDecimal totalScore;
    private String remark;
    private Long evaluatorId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String studentName;
    @TableField(exist = false)
    private String studentNo;
    @TableField(exist = false)
    private String evaluatorName;
}
