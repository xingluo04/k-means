package com.sqe.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqe.entity.AcademicEvaluation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AcademicEvaluationMapper extends BaseMapper<AcademicEvaluation> {

    @Select("<script>" +
            "SELECT ae.*, su.real_name as student_name, si.student_no, eu.real_name as evaluator_name " +
            "FROM academic_evaluation ae " +
            "LEFT JOIN student_info si ON ae.student_id = si.id " +
            "LEFT JOIN sys_user su ON si.user_id = su.id " +
            "LEFT JOIN sys_user eu ON ae.evaluator_id = eu.id " +
            "<where>" +
            "<if test='studentId != null'> AND ae.student_id = #{studentId}</if>" +
            "<if test='academicYear != null and academicYear != \"\"'> AND ae.academic_year = #{academicYear}</if>" +
            "</where>" +
            " ORDER BY ae.id DESC" +
            "</script>")
    IPage<AcademicEvaluation> selectEvalPage(Page<AcademicEvaluation> page, @Param("studentId") Long studentId, @Param("academicYear") String academicYear);
}
