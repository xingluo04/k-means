package com.sqe.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 艺术素质评价实体
 */
@Data
@TableName("art_evaluation")
public class ArtEvaluation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String semester;
    /* 美育课程与活动 */
    private Integer artCourseCount;
    private BigDecimal artCourseAvgScore;
    private Integer artGroupMember;
    private Integer artActivityCount;
    private BigDecimal artActivityScore;
    /* 文艺竞赛 */
    private Integer artCompetitionCount;
    private String artCompetitionCategory;
    private String artHighestLevel;
    private BigDecimal artCompetitionScore;
    /* 扣分事项 */
    private Integer absentArtCount;
    private BigDecimal artDeduction;
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
