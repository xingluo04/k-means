package com.sqe.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 德育测评实体
 */
@Data
@TableName("moral_evaluation")
public class MoralEvaluation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String academicYear;
    /* 德育实践 */
    private BigDecimal moralPracticeBase;
    /* 品德表现 */
    private BigDecimal politicalThought;
    private BigDecimal integrity;
    private BigDecimal learningAttitude;
    private BigDecimal discipline;
    private BigDecimal collective;
    private BigDecimal civility;
    private BigDecimal moralCharacterSubtotal;
    /* 奖励分 */
    private BigDecimal moralHonor;
    private BigDecimal moralSocialWork;
    private BigDecimal moralOutstanding;
    private BigDecimal moralBonusSubtotal;
    /* 扣分与汇总 */
    private BigDecimal moralDeduction;
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
