package com.sqe.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 综合素质评价汇总实体
 */
@Data
@TableName("comprehensive_evaluation")
public class ComprehensiveEvaluation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String semester;
    private BigDecimal moralScore;
    private BigDecimal academicScore;
    private BigDecimal physicalScore;
    private BigDecimal artScore;
    private BigDecimal practiceScore;
    private BigDecimal totalScore;
    private Integer clusterLabel;
    private String clusterName;
    private String suggestion;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String studentName;
    @TableField(exist = false)
    private String studentNo;
    @TableField(exist = false)
    private String className;
}
