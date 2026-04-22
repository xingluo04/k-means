package com.sqe.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 体育素质评价实体
 */
@Data
@TableName("physical_evaluation")
public class PhysicalEvaluation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String semester;
    /* 体质健康 */
    private BigDecimal fitnessTestScore;
    private Integer isExempt;
    private BigDecimal fitnessScore;
    /* 体育课与日常锻炼 */
    private BigDecimal peCourseScore;
    private Integer sportsTeamMember;
    private Integer sportsActivityCount;
    private Integer dailyExerciseCount;
    private BigDecimal exerciseScore;
    /* 体育竞赛 */
    private Integer sportsCompetitionCount;
    private String sportsHighestLevel;
    private String sportsBestRank;
    private Integer brokeRecord;
    private BigDecimal sportsCompetitionScore;
    /* 扣分事项 */
    private Integer absentSportsCount;
    private BigDecimal physicalDeduction;
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
