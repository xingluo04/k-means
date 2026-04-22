package com.sqe.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 实践素质评价实体
 */
@Data
@TableName("practice_evaluation")
public class PracticeEvaluation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String semester;
    /* 日常劳动 */
    private Integer publicLaborCount;
    private Integer dormHygienePass;
    private Integer dormHygieneFail;
    private Integer civilizedDorm;
    private BigDecimal dailyLaborScore;
    /* 劳动教育课程 */
    private Integer laborCourseCount;
    private BigDecimal laborCourseScore;
    /* 社会实践与志愿服务 */
    private BigDecimal volunteerHours;
    private Integer socialPracticeCount;
    private Integer studyStyleActivityCount;
    private BigDecimal volunteerScore;
    /* 实践竞赛 */
    private Integer practiceCompetitionCount;
    private String practiceHighestLevel;
    private BigDecimal practiceCompetitionScore;
    /* 扣分事项 */
    private Integer wasteCount;
    private Integer dirtyDormCount;
    private BigDecimal practiceDeduction;
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
