package com.sqe.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqe.entity.StudentInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface StudentInfoMapper extends BaseMapper<StudentInfo> {

    @Select("<script>" +
            "SELECT si.*, su.real_name, su.phone, su.email, su.gender, " +
            "ci.class_name, pu.real_name as parent_name " +
            "FROM student_info si " +
            "LEFT JOIN sys_user su ON si.user_id = su.id " +
            "LEFT JOIN class_info ci ON si.class_id = ci.id " +
            "LEFT JOIN sys_user pu ON si.parent_id = pu.id " +
            "<where>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            " AND (su.real_name LIKE CONCAT('%',#{keyword},'%') OR si.student_no LIKE CONCAT('%',#{keyword},'%'))" +
            "</if>" +
            "<if test='classId != null'> AND si.class_id = #{classId}</if>" +
            "</where>" +
            " ORDER BY si.id" +
            "</script>")
    IPage<StudentInfo> selectStudentPage(Page<StudentInfo> page, @Param("keyword") String keyword, @Param("classId") Long classId);
}
