package com.sqe.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sqe.entity.*;
import com.sqe.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据看板服务
 */
@Service
public class DashboardService {

    @Autowired
    private SysUserMapper userMapper;
    @Autowired
    private StudentInfoMapper studentInfoMapper;
    @Autowired
    private ClassInfoMapper classInfoMapper;
    @Autowired
    private ComprehensiveEvaluationMapper comprehensiveMapper;
    @Autowired
    private ClusterResultMapper clusterResultMapper;

    public Map<String, Object> getOverview() {
        Map<String, Object> data = new HashMap<>();
        data.put("studentCount", userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, "student")));
        data.put("teacherCount", userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, "teacher")));
        data.put("classCount", classInfoMapper.selectCount(null));
        data.put("evaluationCount", comprehensiveMapper.selectCount(null));
        return data;
    }

    public Map<String, Object> getDimensionAvg(String academicYear) {
        List<ComprehensiveEvaluation> evals = comprehensiveMapper.selectList(
                new LambdaQueryWrapper<ComprehensiveEvaluation>()
                        .eq(ComprehensiveEvaluation::getAcademicYear, academicYear));
        Map<String, Object> data = new HashMap<>();
        if (evals.isEmpty()) return data;

        double avgMoral = evals.stream().mapToDouble(e -> e.getMoralScore().doubleValue()).average().orElse(0);
        double avgAcademic = evals.stream().mapToDouble(e -> e.getAcademicScore().doubleValue()).average().orElse(0);
        double avgPhysical = evals.stream().mapToDouble(e -> e.getPhysicalScore().doubleValue()).average().orElse(0);
        double avgArt = evals.stream().mapToDouble(e -> e.getArtScore().doubleValue()).average().orElse(0);
        double avgPractice = evals.stream().mapToDouble(e -> e.getPracticeScore().doubleValue()).average().orElse(0);

        data.put("dimensions", Arrays.asList("德育发展", "智育发展", "体育发展", "美育发展", "劳动教育发展"));
        data.put("values", Arrays.asList(
                Math.round(avgMoral * 100.0) / 100.0,
                Math.round(avgAcademic * 100.0) / 100.0,
                Math.round(avgPhysical * 100.0) / 100.0,
                Math.round(avgArt * 100.0) / 100.0,
                Math.round(avgPractice * 100.0) / 100.0));
        return data;
    }

    public List<Map<String, Object>> getClusterDistribution(String academicYear) {
        List<ClusterResult> results = clusterResultMapper.selectList(
                new LambdaQueryWrapper<ClusterResult>()
                        .eq(ClusterResult::getAcademicYear, academicYear)
                        .orderByAsc(ClusterResult::getClusterLabel));
        return results.stream().map(r -> {
            Map<String, Object> item = new HashMap<>();
            item.put("name", r.getClusterName());
            item.put("value", r.getStudentCount());
            return item;
        }).collect(Collectors.toList());
    }

    public Map<String, Object> getScoreDistribution(String academicYear) {
        List<ComprehensiveEvaluation> evals = comprehensiveMapper.selectList(
                new LambdaQueryWrapper<ComprehensiveEvaluation>()
                        .eq(ComprehensiveEvaluation::getAcademicYear, academicYear));
        Map<String, Object> data = new HashMap<>();
        int[] ranges = new int[5];
        String[] labels = {"60分以下", "60-69分", "70-79分", "80-89分", "90分以上"};
        for (ComprehensiveEvaluation e : evals) {
            double score = e.getTotalScore().doubleValue();
            if (score < 60) ranges[0]++;
            else if (score < 70) ranges[1]++;
            else if (score < 80) ranges[2]++;
            else if (score < 90) ranges[3]++;
            else ranges[4]++;
        }
        data.put("labels", Arrays.asList(labels));
        data.put("values", Arrays.asList(ranges[0], ranges[1], ranges[2], ranges[3], ranges[4]));
        return data;
    }
}
