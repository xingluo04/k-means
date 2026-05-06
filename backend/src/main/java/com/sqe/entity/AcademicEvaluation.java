package com.sqe.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 智育测评实体
 */
@Data
@TableName("academic_evaluation")
public class AcademicEvaluation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String academicYear;
    /* 学业成绩 */
    private BigDecimal weightedAvgScore;
    /* 奖励分 */
    private BigDecimal academicBonus;
    private BigDecimal academicOther;
    private BigDecimal academicBonusSubtotal;
    /* 扣分与汇总 */
    private BigDecimal academicDeduction;
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
