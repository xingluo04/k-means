package com.sqe.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqe.entity.*;
import com.sqe.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 评价管理服务
 */
@Service
public class EvaluationService {

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

    /* 品德评价分页查询 */
    public IPage<MoralEvaluation> moralPage(int current, int size, Long studentId, String semester) {
        return moralMapper.selectEvalPage(new Page<>(current, size), studentId, semester);
    }

    /* 保存品德评价：德育实践+品德表现(6项均值)+荣誉+社会工作+突出事例-扣分 */
    public void saveMoral(MoralEvaluation eval) {
        BigDecimal conductAvg = eval.getPoliticalThoughtScore()
                .add(eval.getIntegrityScore())
                .add(eval.getLearningAttitudeScore())
                .add(eval.getDisciplineScore())
                .add(eval.getCollectiveScore())
                .add(eval.getCivilityScore())
                .divide(BigDecimal.valueOf(6), 2, RoundingMode.HALF_UP);
        BigDecimal sum = eval.getMoralActivityScore()
                .add(conductAvg)
                .add(eval.getHonorScore())
                .add(eval.getSocialWorkScore())
                .add(eval.getOutstandingScore());
        BigDecimal total = sum.divide(BigDecimal.valueOf(5), 2, RoundingMode.HALF_UP)
                .subtract(eval.getDeductionScore());
        eval.setTotalScore(total.max(BigDecimal.ZERO));
        if (eval.getId() != null) moralMapper.updateById(eval);
        else moralMapper.insert(eval);
    }

    /* 学业评价分页查询 */
    public IPage<AcademicEvaluation> academicPage(int current, int size, Long studentId, String semester) {
        return academicMapper.selectEvalPage(new Page<>(current, size), studentId, semester);
    }

    /* 保存学业评价：学业表现+科研竞赛-扣分 */
    public void saveAcademic(AcademicEvaluation eval) {
        BigDecimal sum = eval.getAcademicPerformanceScore()
                .add(eval.getResearchScore());
        BigDecimal total = sum.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP)
                .subtract(eval.getAcademicDeduction());
        eval.setTotalScore(total.max(BigDecimal.ZERO));
        if (eval.getId() != null) academicMapper.updateById(eval);
        else academicMapper.insert(eval);
    }

    /* 体能评价分页查询 */
    public IPage<PhysicalEvaluation> physicalPage(int current, int size, Long studentId, String semester) {
        return physicalMapper.selectEvalPage(new Page<>(current, size), studentId, semester);
    }

    /* 保存体能评价：体质健康+体育课锻炼+体育竞赛-扣分 */
    public void savePhysical(PhysicalEvaluation eval) {
        BigDecimal sum = eval.getFitnessScore()
                .add(eval.getExerciseScore())
                .add(eval.getSportsCompetitionScore());
        BigDecimal total = sum.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP)
                .subtract(eval.getPhysicalDeduction());
        eval.setTotalScore(total.max(BigDecimal.ZERO));
        if (eval.getId() != null) physicalMapper.updateById(eval);
        else physicalMapper.insert(eval);
    }

    /* 艺术评价分页查询 */
    public IPage<ArtEvaluation> artPage(int current, int size, Long studentId, String semester) {
        return artMapper.selectEvalPage(new Page<>(current, size), studentId, semester);
    }

    /* 保存艺术评价：美育课程活动+文艺竞赛-扣分 */
    public void saveArt(ArtEvaluation eval) {
        BigDecimal sum = eval.getArtActivityScore()
                .add(eval.getArtCompetitionScore());
        BigDecimal total = sum.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP)
                .subtract(eval.getArtDeduction());
        eval.setTotalScore(total.max(BigDecimal.ZERO));
        if (eval.getId() != null) artMapper.updateById(eval);
        else artMapper.insert(eval);
    }

    /* 实践评价分页查询 */
    public IPage<PracticeEvaluation> practicePage(int current, int size, Long studentId, String semester) {
        return practiceMapper.selectEvalPage(new Page<>(current, size), studentId, semester);
    }

    /* 保存实践评价：日常劳动+劳动课程+志愿服务+实践竞赛-扣分 */
    public void savePractice(PracticeEvaluation eval) {
        BigDecimal sum = eval.getDailyLaborScore()
                .add(eval.getLaborCourseScore())
                .add(eval.getVolunteerScore())
                .add(eval.getPracticeCompetitionScore());
        BigDecimal total = sum.divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP)
                .subtract(eval.getPracticeDeduction());
        eval.setTotalScore(total.max(BigDecimal.ZERO));
        if (eval.getId() != null) practiceMapper.updateById(eval);
        else practiceMapper.insert(eval);
    }

    /* 综合评价分页查询 */
    public IPage<ComprehensiveEvaluation> comprehensivePage(int current, int size, Long studentId,
                                                             String semester, Integer clusterLabel, String keyword) {
        return comprehensiveMapper.selectEvalPage(new Page<>(current, size), studentId, semester, clusterLabel, keyword);
    }

    public ComprehensiveEvaluation getStudentComprehensive(Long studentId, String semester) {
        return comprehensiveMapper.selectOne(
                new LambdaQueryWrapper<ComprehensiveEvaluation>()
                        .eq(ComprehensiveEvaluation::getStudentId, studentId)
                        .eq(ComprehensiveEvaluation::getSemester, semester));
    }

    public List<ComprehensiveEvaluation> getStudentAllSemesters(Long studentId) {
        return comprehensiveMapper.selectList(
                new LambdaQueryWrapper<ComprehensiveEvaluation>()
                        .eq(ComprehensiveEvaluation::getStudentId, studentId)
                        .orderByAsc(ComprehensiveEvaluation::getSemester));
    }

    public void deleteMoral(Long id) { moralMapper.deleteById(id); }
    public void deleteAcademic(Long id) { academicMapper.deleteById(id); }
    public void deletePhysical(Long id) { physicalMapper.deleteById(id); }
    public void deleteArt(Long id) { artMapper.deleteById(id); }
    public void deletePractice(Long id) { practiceMapper.deleteById(id); }
}
