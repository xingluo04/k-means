package com.sqe.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sqe.entity.*;
import com.sqe.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据汇总服务 — 从五维评价表聚合数据到综合评价表
 */
@Service
public class DataAggregationService {

    @Autowired
    private MoralEvaluationMapper moralMapper;
    @Autowired
    private AcademicEvaluationMapper academicMapper;
    @Autowired
    private PhysicalEvaluationMapper physicalMapper;
    @Autowired
    private ArtEvaluationMapper artMapper;
    @Autowired
    private PracticeEvaluationMapper practiceMapper;
    @Autowired
    private ComprehensiveEvaluationMapper comprehensiveMapper;

    private static final BigDecimal W_MORAL = new BigDecimal("0.25");
    private static final BigDecimal W_ACADEMIC = new BigDecimal("0.50");
    private static final BigDecimal W_PHYSICAL = new BigDecimal("0.08");
    private static final BigDecimal W_ART = new BigDecimal("0.08");
    private static final BigDecimal W_PRACTICE = new BigDecimal("0.09");

    /**
     * 汇总各维度评价数据到综合评价表，使用加权总分
     */
    public void aggregateEvaluations(String academicYear) {
        List<MoralEvaluation> morals = moralMapper.selectList(
                new LambdaQueryWrapper<MoralEvaluation>().eq(MoralEvaluation::getAcademicYear, academicYear));
        Map<Long, BigDecimal> moralMap = morals.stream()
                .collect(Collectors.toMap(MoralEvaluation::getStudentId, MoralEvaluation::getTotalScore, (a, b) -> b));

        List<AcademicEvaluation> academics = academicMapper.selectList(
                new LambdaQueryWrapper<AcademicEvaluation>().eq(AcademicEvaluation::getAcademicYear, academicYear));
        Map<Long, BigDecimal> academicMap = academics.stream()
                .collect(Collectors.toMap(AcademicEvaluation::getStudentId, AcademicEvaluation::getTotalScore, (a, b) -> b));

        List<PhysicalEvaluation> physicals = physicalMapper.selectList(
                new LambdaQueryWrapper<PhysicalEvaluation>().eq(PhysicalEvaluation::getAcademicYear, academicYear));
        Map<Long, BigDecimal> physicalMap = physicals.stream()
                .collect(Collectors.toMap(PhysicalEvaluation::getStudentId, PhysicalEvaluation::getTotalScore, (a, b) -> b));

        List<ArtEvaluation> arts = artMapper.selectList(
                new LambdaQueryWrapper<ArtEvaluation>().eq(ArtEvaluation::getAcademicYear, academicYear));
        Map<Long, BigDecimal> artMap = arts.stream()
                .collect(Collectors.toMap(ArtEvaluation::getStudentId, ArtEvaluation::getTotalScore, (a, b) -> b));

        List<PracticeEvaluation> practices = practiceMapper.selectList(
                new LambdaQueryWrapper<PracticeEvaluation>().eq(PracticeEvaluation::getAcademicYear, academicYear));
        Map<Long, BigDecimal> practiceMap = practices.stream()
                .collect(Collectors.toMap(PracticeEvaluation::getStudentId, PracticeEvaluation::getTotalScore, (a, b) -> b));

        Set<Long> allStudentIds = new HashSet<>();
        allStudentIds.addAll(moralMap.keySet());
        allStudentIds.addAll(academicMap.keySet());
        allStudentIds.addAll(physicalMap.keySet());
        allStudentIds.addAll(artMap.keySet());
        allStudentIds.addAll(practiceMap.keySet());

        for (Long studentId : allStudentIds) {
            BigDecimal moral = moralMap.getOrDefault(studentId, BigDecimal.ZERO);
            BigDecimal academic = academicMap.getOrDefault(studentId, BigDecimal.ZERO);
            BigDecimal physical = physicalMap.getOrDefault(studentId, BigDecimal.ZERO);
            BigDecimal art = artMap.getOrDefault(studentId, BigDecimal.ZERO);
            BigDecimal practice = practiceMap.getOrDefault(studentId, BigDecimal.ZERO);

            BigDecimal total = moral.multiply(W_MORAL)
                    .add(academic.multiply(W_ACADEMIC))
                    .add(physical.multiply(W_PHYSICAL))
                    .add(art.multiply(W_ART))
                    .add(practice.multiply(W_PRACTICE))
                    .setScale(2, RoundingMode.HALF_UP);

            ComprehensiveEvaluation existing = comprehensiveMapper.selectOne(
                    new LambdaQueryWrapper<ComprehensiveEvaluation>()
                            .eq(ComprehensiveEvaluation::getStudentId, studentId)
                            .eq(ComprehensiveEvaluation::getAcademicYear, academicYear));

            if (existing != null) {
                existing.setMoralScore(moral);
                existing.setAcademicScore(academic);
                existing.setPhysicalScore(physical);
                existing.setArtScore(art);
                existing.setPracticeScore(practice);
                existing.setTotalScore(total);
                comprehensiveMapper.updateById(existing);
            } else {
                ComprehensiveEvaluation ce = new ComprehensiveEvaluation();
                ce.setStudentId(studentId);
                ce.setAcademicYear(academicYear);
                ce.setMoralScore(moral);
                ce.setAcademicScore(academic);
                ce.setPhysicalScore(physical);
                ce.setArtScore(art);
                ce.setPracticeScore(practice);
                ce.setTotalScore(total);
                comprehensiveMapper.insert(ce);
            }
        }
    }

    /**
     * 加载上一学年综合评价数据
     */
    public Map<Long, ComprehensiveEvaluation> loadPreviousYearEvaluations(String academicYear) {
        String prevYear = previousAcademicYear(academicYear);
        if (prevYear == null) return Collections.emptyMap();
        List<ComprehensiveEvaluation> prevList = comprehensiveMapper.selectList(
                new LambdaQueryWrapper<ComprehensiveEvaluation>()
                        .eq(ComprehensiveEvaluation::getAcademicYear, prevYear));
        Map<Long, ComprehensiveEvaluation> map = new HashMap<>();
        for (ComprehensiveEvaluation e : prevList) {
            map.put(e.getStudentId(), e);
        }
        return map;
    }

    /**
     * 解析上一学年
     */
    public String previousAcademicYear(String year) {
        if ("2024-2025".equals(year)) return "2023-2024";
        if ("2023-2024".equals(year)) return null;
        try {
            String[] parts = year.split("-");
            int start = Integer.parseInt(parts[0]);
            return (start - 1) + "-" + start;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从综合评价列表构建原始5维特征矩阵
     */
    public double[][] buildRawData(List<ComprehensiveEvaluation> evaluations) {
        double[][] rawData = new double[evaluations.size()][5];
        for (int i = 0; i < evaluations.size(); i++) {
            ComprehensiveEvaluation e = evaluations.get(i);
            rawData[i] = new double[]{
                    e.getMoralScore().doubleValue(), e.getAcademicScore().doubleValue(),
                    e.getPhysicalScore().doubleValue(), e.getArtScore().doubleValue(),
                    e.getPracticeScore().doubleValue()
            };
        }
        return rawData;
    }
}
