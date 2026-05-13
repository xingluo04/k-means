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

        double avgMoral = evals.stream().mapToDouble(e -> e.getMoralScore() != null ? e.getMoralScore().doubleValue() : 0).average().orElse(0);
        double avgAcademic = evals.stream().mapToDouble(e -> e.getAcademicScore() != null ? e.getAcademicScore().doubleValue() : 0).average().orElse(0);
        double avgPhysical = evals.stream().mapToDouble(e -> e.getPhysicalScore() != null ? e.getPhysicalScore().doubleValue() : 0).average().orElse(0);
        double avgArt = evals.stream().mapToDouble(e -> e.getArtScore() != null ? e.getArtScore().doubleValue() : 0).average().orElse(0);
        double avgPractice = evals.stream().mapToDouble(e -> e.getPracticeScore() != null ? e.getPracticeScore().doubleValue() : 0).average().orElse(0);

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
            double score = e.getTotalScore() != null ? e.getTotalScore().doubleValue() : 0;
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

    /**
     * 班级间聚类分布对比
     */
    public Map<String, Object> getClassComparison(String academicYear) {
        // 查询所有班级信息，构建 classId -> className 映射
        List<ClassInfo> classes = classInfoMapper.selectList(null);
        Map<Long, String> classIdToName = new HashMap<>();
        for (ClassInfo ci : classes) {
            classIdToName.put(ci.getId(), ci.getClassName());
        }

        // 查询所有学生信息，构建 studentInfo.id -> classId 映射
        List<StudentInfo> students = studentInfoMapper.selectList(null);
        Map<Long, Long> studentIdToClassId = new HashMap<>();
        for (StudentInfo si : students) {
            studentIdToClassId.put(si.getId(), si.getClassId());
        }

        // 查询该学年的综合评价数据（含聚类标签）
        List<ComprehensiveEvaluation> evals = comprehensiveMapper.selectList(
                new LambdaQueryWrapper<ComprehensiveEvaluation>()
                        .eq(ComprehensiveEvaluation::getAcademicYear, academicYear)
                        .isNotNull(ComprehensiveEvaluation::getClusterName));

        if (evals.isEmpty()) return new HashMap<>();

        // 收集所有聚类名称
        Set<String> clusterNames = new LinkedHashSet<>();
        for (ComprehensiveEvaluation e : evals) {
            if (e.getClusterName() != null) clusterNames.add(e.getClusterName());
        }

        // 按班级分组统计聚类分布
        Map<String, Map<String, Integer>> classClusterCount = new LinkedHashMap<>();
        for (ComprehensiveEvaluation e : evals) {
            Long classId = studentIdToClassId.get(e.getStudentId());
            String className = classId != null ? classIdToName.getOrDefault(classId, "未知班级") : "未知班级";
            String clusterName = e.getClusterName() != null ? e.getClusterName() : "未分类";
            classClusterCount.computeIfAbsent(className, k -> new LinkedHashMap<>())
                    .merge(clusterName, 1, Integer::sum);
        }

        // 构建返回数据
        List<String> classList = new ArrayList<>(classClusterCount.keySet());
        List<String> clusterList = new ArrayList<>(clusterNames);
        List<Map<String, Object>> series = new ArrayList<>();

        for (String clusterName : clusterList) {
            List<Integer> counts = new ArrayList<>();
            for (String className : classList) {
                counts.add(classClusterCount.getOrDefault(className, Collections.emptyMap())
                        .getOrDefault(clusterName, 0));
            }
            Map<String, Object> item = new HashMap<>();
            item.put("name", clusterName);
            item.put("data", counts);
            series.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("classes", classList);
        result.put("clusterNames", clusterList);
        result.put("series", series);
        return result;
    }
}
