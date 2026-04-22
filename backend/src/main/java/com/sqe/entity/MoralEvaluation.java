package com.sqe.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 品德素质评价实体
 */
@Data
@TableName("moral_evaluation")
public class MoralEvaluation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String semester;
    /* 德育实践 */
    private Integer moralActivityCount;
    private BigDecimal moralActivityScore;
    /* 品德表现 */
    private BigDecimal politicalThoughtScore;
    private BigDecimal integrityScore;
    private BigDecimal learningAttitudeScore;
    private BigDecimal disciplineScore;
    private BigDecimal collectiveScore;
    private BigDecimal civilityScore;
    /* 荣誉与奖励 */
    private Integer personalHonorCount;
    private Integer collectiveHonorCount;
    private BigDecimal honorScore;
    private String militaryTrainingLevel;
    /* 社会工作 */
    private String cadreDuty;
    private Integer cadreDuration;
    private BigDecimal socialWorkScore;
    /* 突出事例 */
    private Integer outstandingEventCount;
    private Integer bloodDonationCount;
    private BigDecimal outstandingScore;
    /* 扣分事项 */
    private Integer criticismCount;
    private String disciplinePunishment;
    private Integer absentActivityCount;
    private BigDecimal deductionScore;
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
