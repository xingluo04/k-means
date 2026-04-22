package com.sqe.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqe.entity.MoralEvaluation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MoralEvaluationMapper extends BaseMapper<MoralEvaluation> {

    @Select("<script>" +
            "SELECT me.*, su.real_name as student_name, si.student_no, eu.real_name as evaluator_name " +
            "FROM moral_evaluation me " +
            "LEFT JOIN student_info si ON me.student_id = si.id " +
            "LEFT JOIN sys_user su ON si.user_id = su.id " +
            "LEFT JOIN sys_user eu ON me.evaluator_id = eu.id " +
            "<where>" +
            "<if test='studentId != null'> AND me.student_id = #{studentId}</if>" +
            "<if test='semester != null and semester != \"\"'> AND me.semester = #{semester}</if>" +
            "</where>" +
            " ORDER BY me.id DESC" +
            "</script>")
    IPage<MoralEvaluation> selectEvalPage(Page<MoralEvaluation> page, @Param("studentId") Long studentId, @Param("semester") String semester);
}
