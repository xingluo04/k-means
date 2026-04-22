package com.sqe.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 学生信息实体
 */
@Data
@TableName("student_info")
public class StudentInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String studentNo;
    private Long classId;
    private Long parentId;
    private Integer enrollmentYear;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /* 非数据库字段 */
    @TableField(exist = false)
    private String realName;
    @TableField(exist = false)
    private String className;
    @TableField(exist = false)
    private String parentName;
    @TableField(exist = false)
    private String phone;
    @TableField(exist = false)
    private String email;
    @TableField(exist = false)
    private Integer gender;
}
