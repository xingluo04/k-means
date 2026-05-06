package com.sqe.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqe.entity.PracticeEvaluation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PracticeEvaluationMapper extends BaseMapper<PracticeEvaluation> {

    @Select("<script>" +
            "SELECT pe.*, su.real_name as student_name, si.student_no, eu.real_name as evaluator_name " +
            "FROM practice_evaluation pe " +
            "LEFT JOIN student_info si ON pe.student_id = si.id " +
            "LEFT JOIN sys_user su ON si.user_id = su.id " +
            "LEFT JOIN sys_user eu ON pe.evaluator_id = eu.id " +
            "<where>" +
            "<if test='studentId != null'> AND pe.student_id = #{studentId}</if>" +
            "<if test='academicYear != null and academicYear != \"\"'> AND pe.academic_year = #{academicYear}</if>" +
            "</where>" +
            " ORDER BY pe.id DESC" +
            "</script>")
    IPage<PracticeEvaluation> selectEvalPage(Page<PracticeEvaluation> page, @Param("studentId") Long studentId, @Param("academicYear") String academicYear);
}
