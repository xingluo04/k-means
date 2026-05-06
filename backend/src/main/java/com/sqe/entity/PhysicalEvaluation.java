package com.sqe.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 体育测评实体
 */
@Data
@TableName("physical_evaluation")
public class PhysicalEvaluation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String academicYear;
    /* 体育表现 */
    private BigDecimal physicalPerformance;
    /* 奖励分 */
    private BigDecimal physicalBonus;
    /* 扣分与汇总 */
    private BigDecimal physicalDeduction;
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
