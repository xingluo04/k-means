package com.sqe.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqe.entity.ComprehensiveEvaluation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ComprehensiveEvaluationMapper extends BaseMapper<ComprehensiveEvaluation> {

    @Select("<script>" +
            "SELECT ce.*, su.real_name as student_name, si.student_no, ci.class_name " +
            "FROM comprehensive_evaluation ce " +
            "LEFT JOIN student_info si ON ce.student_id = si.id " +
            "LEFT JOIN sys_user su ON si.user_id = su.id " +
            "LEFT JOIN class_info ci ON si.class_id = ci.id " +
            "<where>" +
            "<if test='studentId != null'> AND ce.student_id = #{studentId}</if>" +
            "<if test='academicYear != null and academicYear != \"\"'> AND ce.academic_year = #{academicYear}</if>" +
            "<if test='clusterLabel != null'> AND ce.cluster_label = #{clusterLabel}</if>" +
            "<if test='classId != null'> AND si.class_id = #{classId}</if>" +
            "<if test='keyword != null and keyword != \"\"'> AND (su.real_name LIKE CONCAT('%',#{keyword},'%') OR si.student_no LIKE CONCAT('%',#{keyword},'%'))</if>" +
            "</where>" +
            " ORDER BY ce.total_score DESC" +
            "</script>")
    IPage<ComprehensiveEvaluation> selectEvalPage(Page<ComprehensiveEvaluation> page,
                                                   @Param("studentId") Long studentId,
                                                   @Param("academicYear") String academicYear,
                                                   @Param("clusterLabel") Integer clusterLabel,
                                                   @Param("classId") Long classId,
                                                   @Param("keyword") String keyword);

    @Select("SELECT ce.*, su.real_name as student_name, si.student_no, ci.class_name " +
            "FROM comprehensive_evaluation ce " +
            "LEFT JOIN student_info si ON ce.student_id = si.id " +
            "LEFT JOIN sys_user su ON si.user_id = su.id " +
            "LEFT JOIN class_info ci ON si.class_id = ci.id " +
            "WHERE ce.student_id = #{studentId} AND ce.academic_year = #{academicYear}")
    ComprehensiveEvaluation selectDetail(@Param("studentId") Long studentId,
                                         @Param("academicYear") String academicYear);
}
