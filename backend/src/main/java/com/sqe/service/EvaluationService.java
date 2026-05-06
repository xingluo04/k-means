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

    /* 德育评价分页查询 */
    public IPage<MoralEvaluation> moralPage(int current, int size, Long studentId, String academicYear) {
        return moralMapper.selectEvalPage(new Page<>(current, size), studentId, academicYear);
    }

    /* 保存德育评价：德育总分 = (德育实践基础分 + 品德表现小计 + 德育奖励分小计) / 3 - 扣分 */
    public void saveMoral(MoralEvaluation eval) {
        // 计算品德表现小计
        BigDecimal characterSubtotal = eval.getPoliticalThought()
                .add(eval.getIntegrity())
                .add(eval.getLearningAttitude())
                .add(eval.getDiscipline())
                .add(eval.getCollective())
                .add(eval.getCivility());
        eval.setMoralCharacterSubtotal(characterSubtotal);

        // 计算奖励分小计
        BigDecimal bonusSubtotal = eval.getMoralHonor()
                .add(eval.getMoralSocialWork())
                .add(eval.getMoralOutstanding());
        eval.setMoralBonusSubtotal(bonusSubtotal);

        // 总分 = (实践分 + 品德表现 + 奖励分) / 3 - 扣分
        BigDecimal sum = eval.getMoralPracticeBase()
                .add(characterSubtotal)
                .add(bonusSubtotal);
        BigDecimal total = sum.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP)
                .subtract(eval.getMoralDeduction());
        eval.setTotalScore(total.max(BigDecimal.ZERO));
        if (eval.getId() != null) moralMapper.updateById(eval);
        else moralMapper.insert(eval);
    }

    /* 智育评价分页查询 */
    public IPage<AcademicEvaluation> academicPage(int current, int size, Long studentId, String academicYear) {
        return academicMapper.selectEvalPage(new Page<>(current, size), studentId, academicYear);
    }

    /* 保存智育评价：智育总分 = (学分加权平均成绩 + 智育奖励分小计) / 2 - 扣分 */
    public void saveAcademic(AcademicEvaluation eval) {
        BigDecimal bonusSubtotal = eval.getAcademicBonus().add(eval.getAcademicOther());
        eval.setAcademicBonusSubtotal(bonusSubtotal);

        BigDecimal sum = eval.getWeightedAvgScore().add(bonusSubtotal);
        BigDecimal total = sum.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP)
                .subtract(eval.getAcademicDeduction());
        eval.setTotalScore(total.max(BigDecimal.ZERO));
        if (eval.getId() != null) academicMapper.updateById(eval);
        else academicMapper.insert(eval);
    }

    /* 体育评价分页查询 */
    public IPage<PhysicalEvaluation> physicalPage(int current, int size, Long studentId, String academicYear) {
        return physicalMapper.selectEvalPage(new Page<>(current, size), studentId, academicYear);
    }

    /* 保存体育评价：体育总分 = (体育表现分 + 体育奖励分) / 2 - 扣分 */
    public void savePhysical(PhysicalEvaluation eval) {
        BigDecimal sum = eval.getPhysicalPerformance().add(eval.getPhysicalBonus());
        BigDecimal total = sum.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP)
                .subtract(eval.getPhysicalDeduction());
        eval.setTotalScore(total.max(BigDecimal.ZERO));
        if (eval.getId() != null) physicalMapper.updateById(eval);
        else physicalMapper.insert(eval);
    }

    /* 美育评价分页查询 */
    public IPage<ArtEvaluation> artPage(int current, int size, Long studentId, String academicYear) {
        return artMapper.selectEvalPage(new Page<>(current, size), studentId, academicYear);
    }

    /* 保存美育评价：美育总分 = (美育表现分 + 美育奖励分) / 2 - 扣分 */
    public void saveArt(ArtEvaluation eval) {
        BigDecimal sum = eval.getArtPerformance().add(eval.getArtBonus());
        BigDecimal total = sum.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP)
                .subtract(eval.getArtDeduction());
        eval.setTotalScore(total.max(BigDecimal.ZERO));
        if (eval.getId() != null) artMapper.updateById(eval);
        else artMapper.insert(eval);
    }

    /* 劳动教育评价分页查询 */
    public IPage<PracticeEvaluation> practicePage(int current, int size, Long studentId, String academicYear) {
        return practiceMapper.selectEvalPage(new Page<>(current, size), studentId, academicYear);
    }

    /* 保存劳动教育评价：劳动教育总分 = (劳动教育表现分 + 劳动教育奖励分) / 2 - 扣分 */
    public void savePractice(PracticeEvaluation eval) {
        BigDecimal sum = eval.getLaborPerformance().add(eval.getLaborBonus());
        BigDecimal total = sum.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP)
                .subtract(eval.getLaborDeduction());
        eval.setTotalScore(total.max(BigDecimal.ZERO));
        if (eval.getId() != null) practiceMapper.updateById(eval);
        else practiceMapper.insert(eval);
    }

    /* 综合评价分页查询 */
    public IPage<ComprehensiveEvaluation> comprehensivePage(int current, int size, Long studentId,
                                                             String academicYear, Integer clusterLabel, String keyword) {
        return comprehensiveMapper.selectEvalPage(new Page<>(current, size), studentId, academicYear, clusterLabel, keyword);
    }

    public ComprehensiveEvaluation getStudentComprehensive(Long studentId, String academicYear) {
        return comprehensiveMapper.selectOne(
                new LambdaQueryWrapper<ComprehensiveEvaluation>()
                        .eq(ComprehensiveEvaluation::getStudentId, studentId)
                        .eq(ComprehensiveEvaluation::getAcademicYear, academicYear));
    }

    public List<ComprehensiveEvaluation> getStudentAllSemesters(Long studentId) {
        return comprehensiveMapper.selectList(
                new LambdaQueryWrapper<ComprehensiveEvaluation>()
                        .eq(ComprehensiveEvaluation::getStudentId, studentId)
                        .orderByAsc(ComprehensiveEvaluation::getAcademicYear));
    }

    public void deleteMoral(Long id) { moralMapper.deleteById(id); }
    public void deleteAcademic(Long id) { academicMapper.deleteById(id); }
    public void deletePhysical(Long id) { physicalMapper.deleteById(id); }
    public void deleteArt(Long id) { artMapper.deleteById(id); }
    public void deletePractice(Long id) { practiceMapper.deleteById(id); }
}
