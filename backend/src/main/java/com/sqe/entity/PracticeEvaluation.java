package com.sqe.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 劳动教育测评实体
 */
@Data
@TableName("practice_evaluation")
public class PracticeEvaluation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String academicYear;
    /* 劳动教育表现 */
    private BigDecimal laborPerformance;
    /* 奖励分 */
    private BigDecimal laborBonus;
    /* 扣分与汇总 */
    private BigDecimal laborDeduction;
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
