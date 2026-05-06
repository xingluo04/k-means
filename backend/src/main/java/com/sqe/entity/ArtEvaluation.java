package com.sqe.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 美育测评实体
 */
@Data
@TableName("art_evaluation")
public class ArtEvaluation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String academicYear;
    /* 美育表现 */
    private BigDecimal artPerformance;
    /* 奖励分 */
    private BigDecimal artBonus;
    /* 扣分与汇总 */
    private BigDecimal artDeduction;
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
