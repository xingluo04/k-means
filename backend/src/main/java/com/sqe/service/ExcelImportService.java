package com.sqe.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sqe.entity.*;
import com.sqe.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Excel数据导入服务
 */
@Service
public class ExcelImportService {

    /* 权重常量 */
    private static final BigDecimal W_MORAL = new BigDecimal("0.25");
    private static final BigDecimal W_ACADEMIC = new BigDecimal("0.50");
    private static final BigDecimal W_PHYSICAL = new BigDecimal("0.08");
    private static final BigDecimal W_ART = new BigDecimal("0.08");
    private static final BigDecimal W_PRACTICE = new BigDecimal("0.09");

    @Autowired
    private StudentInfoMapper studentInfoMapper;
    @Autowired
    private SysUserMapper sysUserMapper;
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
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 导入Excel综测数据
     */
    @Transactional
    public Map<String, Object> importExcel(MultipartFile file, String academicYear) throws Exception {
        Map<String, Object> result = new HashMap<>();
        int success = 0, fail = 0;
        List<String> errors = new ArrayList<>();

        String suffix = FileUtil.getSuffix(file.getOriginalFilename());
        File tempFile = FileUtil.createTempFile("import_", "." + suffix, true);
        file.transferTo(tempFile);

        ExcelReader reader = ExcelUtil.getReader(tempFile);

        int totalRows = reader.getRowCount();
        int dataStartRow = findDataStartRow(reader);

        for (int r = dataStartRow; r < totalRows; r++) {
            try {
                String studentNo = readCellAsString(reader, 0, r);
                if (studentNo == null || studentNo.trim().isEmpty()) continue;

                String studentName = readCellAsString(reader, 1, r);

                StudentInfo student = studentInfoMapper.selectOne(
                        new LambdaQueryWrapper<StudentInfo>().eq(StudentInfo::getStudentNo, studentNo));
                if (student == null) {
                    student = createStudentAccount(studentNo, studentName);
                }

                Long studentId = student.getId();

                // 读取整行到List
                List<Object> row = new ArrayList<>();
                for (int c = 0; c < 36; c++) {
                    row.add(reader.readCellValue(c, r));
                }

                saveMoralFromExcel(studentId, academicYear, row);
                saveAcademicFromExcel(studentId, academicYear, row);
                savePhysicalFromExcel(studentId, academicYear, row);
                saveArtFromExcel(studentId, academicYear, row);
                savePracticeFromExcel(studentId, academicYear, row);
                saveComprehensive(studentId, academicYear);

                success++;
            } catch (Exception e) {
                errors.add("第" + (r + 1) + "行：" + e.getMessage());
                fail++;
            }
        }

        reader.close();
        FileUtil.del(tempFile);

        result.put("success", success);
        result.put("fail", fail);
        result.put("errors", errors);
        return result;
    }

    /**
     * 学号不存在时自动创建学生账号（用户名=学号，密码=123456）
     */
    private StudentInfo createStudentAccount(String studentNo, String studentName) {
        SysUser user = new SysUser();
        user.setUsername(studentNo);
        user.setPassword(passwordEncoder.encode("123456"));
        user.setRealName(studentName != null ? studentName : studentNo);
        user.setRole("student");
        user.setStatus(1);
        sysUserMapper.insert(user);

        StudentInfo student = new StudentInfo();
        student.setUserId(user.getId());
        student.setStudentNo(studentNo);
        student.setEnrollmentYear(extractEnrollmentYear(studentNo));
        studentInfoMapper.insert(student);
        return student;
    }

    /**
     * 从学号中提取入学年份，如无法识别则返回null
     */
    private Integer extractEnrollmentYear(String studentNo) {
        if (studentNo != null && studentNo.matches("\\d{8,}")) {
            String prefix = studentNo.substring(0, 4);
            try {
                int year = Integer.parseInt(prefix);
                if (year >= 2000 && year <= 2099) return year;
            } catch (NumberFormatException ignored) {}
            // 尝试前2位作为年份
            String prefix2 = studentNo.substring(0, 2);
            try {
                int year2 = Integer.parseInt(prefix2);
                if (year2 >= 20 && year2 <= 99) return 1900 + year2;
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private int findDataStartRow(ExcelReader reader) {
        int totalRows = reader.getRowCount();
        for (int r = 0; r < Math.min(totalRows, 15); r++) {
            String firstCell = readCellAsString(reader, 0, r);
            if (firstCell != null && firstCell.matches("\\d{6,}")) {
                return r;
            }
        }
        return 5;
    }

    private String readCellAsString(ExcelReader reader, int col, int row) {
        try {
            Object cell = reader.readCellValue(col, row);
            if (cell == null) return null;
            String str = cell.toString().trim();
            if (str.contains(".") && str.matches("\\d+\\.0+")) {
                str = str.substring(0, str.indexOf('.'));
            }
            return str;
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal readCellAsDecimal(List<Object> row, int index) {
        if (index >= row.size()) return BigDecimal.ZERO;
        Object cell = row.get(index);
        if (cell == null) return BigDecimal.ZERO;
        String str = cell.toString().trim();
        if (str.isEmpty()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(str).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private void saveMoralFromExcel(Long studentId, String academicYear, List<Object> row) {
        MoralEvaluation existing = moralMapper.selectOne(
                new LambdaQueryWrapper<MoralEvaluation>()
                        .eq(MoralEvaluation::getStudentId, studentId)
                        .eq(MoralEvaluation::getAcademicYear, academicYear));
        if (existing != null) return;

        MoralEvaluation eval = new MoralEvaluation();
        eval.setStudentId(studentId);
        eval.setAcademicYear(academicYear);
        eval.setMoralPracticeBase(readCellAsDecimal(row, 2));
        eval.setPoliticalThought(readCellAsDecimal(row, 3));
        eval.setIntegrity(readCellAsDecimal(row, 4));
        eval.setLearningAttitude(readCellAsDecimal(row, 5));
        eval.setDiscipline(readCellAsDecimal(row, 6));
        eval.setCollective(readCellAsDecimal(row, 7));
        eval.setCivility(readCellAsDecimal(row, 8));
        eval.setMoralCharacterSubtotal(readCellAsDecimal(row, 9));
        eval.setMoralHonor(readCellAsDecimal(row, 10));
        eval.setMoralSocialWork(readCellAsDecimal(row, 11));
        eval.setMoralOutstanding(readCellAsDecimal(row, 12));
        eval.setMoralBonusSubtotal(readCellAsDecimal(row, 13));
        eval.setMoralDeduction(readCellAsDecimal(row, 14));
        eval.setTotalScore(readCellAsDecimal(row, 15));
        moralMapper.insert(eval);
    }

    private void saveAcademicFromExcel(Long studentId, String academicYear, List<Object> row) {
        AcademicEvaluation existing = academicMapper.selectOne(
                new LambdaQueryWrapper<AcademicEvaluation>()
                        .eq(AcademicEvaluation::getStudentId, studentId)
                        .eq(AcademicEvaluation::getAcademicYear, academicYear));
        if (existing != null) return;

        AcademicEvaluation eval = new AcademicEvaluation();
        eval.setStudentId(studentId);
        eval.setAcademicYear(academicYear);
        eval.setWeightedAvgScore(readCellAsDecimal(row, 16));
        eval.setAcademicBonus(readCellAsDecimal(row, 17));
        eval.setAcademicOther(readCellAsDecimal(row, 18));
        eval.setAcademicBonusSubtotal(readCellAsDecimal(row, 19));
        eval.setAcademicDeduction(readCellAsDecimal(row, 20));
        eval.setTotalScore(readCellAsDecimal(row, 21));
        academicMapper.insert(eval);
    }

    private void savePhysicalFromExcel(Long studentId, String academicYear, List<Object> row) {
        PhysicalEvaluation existing = physicalMapper.selectOne(
                new LambdaQueryWrapper<PhysicalEvaluation>()
                        .eq(PhysicalEvaluation::getStudentId, studentId)
                        .eq(PhysicalEvaluation::getAcademicYear, academicYear));
        if (existing != null) return;

        PhysicalEvaluation eval = new PhysicalEvaluation();
        eval.setStudentId(studentId);
        eval.setAcademicYear(academicYear);
        eval.setPhysicalPerformance(readCellAsDecimal(row, 22));
        eval.setPhysicalBonus(readCellAsDecimal(row, 23));
        eval.setPhysicalDeduction(readCellAsDecimal(row, 24));
        eval.setTotalScore(readCellAsDecimal(row, 25));
        physicalMapper.insert(eval);
    }

    private void saveArtFromExcel(Long studentId, String academicYear, List<Object> row) {
        ArtEvaluation existing = artMapper.selectOne(
                new LambdaQueryWrapper<ArtEvaluation>()
                        .eq(ArtEvaluation::getStudentId, studentId)
                        .eq(ArtEvaluation::getAcademicYear, academicYear));
        if (existing != null) return;

        ArtEvaluation eval = new ArtEvaluation();
        eval.setStudentId(studentId);
        eval.setAcademicYear(academicYear);
        eval.setArtPerformance(readCellAsDecimal(row, 26));
        eval.setArtBonus(readCellAsDecimal(row, 27));
        eval.setArtDeduction(readCellAsDecimal(row, 28));
        eval.setTotalScore(readCellAsDecimal(row, 29));
        artMapper.insert(eval);
    }

    private void savePracticeFromExcel(Long studentId, String academicYear, List<Object> row) {
        PracticeEvaluation existing = practiceMapper.selectOne(
                new LambdaQueryWrapper<PracticeEvaluation>()
                        .eq(PracticeEvaluation::getStudentId, studentId)
                        .eq(PracticeEvaluation::getAcademicYear, academicYear));
        if (existing != null) return;

        PracticeEvaluation eval = new PracticeEvaluation();
        eval.setStudentId(studentId);
        eval.setAcademicYear(academicYear);
        eval.setLaborPerformance(readCellAsDecimal(row, 30));
        eval.setLaborBonus(readCellAsDecimal(row, 31));
        eval.setLaborDeduction(readCellAsDecimal(row, 32));
        eval.setTotalScore(readCellAsDecimal(row, 33));
        practiceMapper.insert(eval);
    }

    private void saveComprehensive(Long studentId, String academicYear) {
        ComprehensiveEvaluation existing = comprehensiveMapper.selectOne(
                new LambdaQueryWrapper<ComprehensiveEvaluation>()
                        .eq(ComprehensiveEvaluation::getStudentId, studentId)
                        .eq(ComprehensiveEvaluation::getAcademicYear, academicYear));
        if (existing != null) return;

        MoralEvaluation moral = moralMapper.selectOne(
                new LambdaQueryWrapper<MoralEvaluation>()
                        .eq(MoralEvaluation::getStudentId, studentId)
                        .eq(MoralEvaluation::getAcademicYear, academicYear));
        AcademicEvaluation academic = academicMapper.selectOne(
                new LambdaQueryWrapper<AcademicEvaluation>()
                        .eq(AcademicEvaluation::getStudentId, studentId)
                        .eq(AcademicEvaluation::getAcademicYear, academicYear));
        PhysicalEvaluation physical = physicalMapper.selectOne(
                new LambdaQueryWrapper<PhysicalEvaluation>()
                        .eq(PhysicalEvaluation::getStudentId, studentId)
                        .eq(PhysicalEvaluation::getAcademicYear, academicYear));
        ArtEvaluation art = artMapper.selectOne(
                new LambdaQueryWrapper<ArtEvaluation>()
                        .eq(ArtEvaluation::getStudentId, studentId)
                        .eq(ArtEvaluation::getAcademicYear, academicYear));
        PracticeEvaluation practice = practiceMapper.selectOne(
                new LambdaQueryWrapper<PracticeEvaluation>()
                        .eq(PracticeEvaluation::getStudentId, studentId)
                        .eq(PracticeEvaluation::getAcademicYear, academicYear));

        BigDecimal moralScore = moral != null ? moral.getTotalScore() : BigDecimal.ZERO;
        BigDecimal academicScore = academic != null ? academic.getTotalScore() : BigDecimal.ZERO;
        BigDecimal physicalScore = physical != null ? physical.getTotalScore() : BigDecimal.ZERO;
        BigDecimal artScore = art != null ? art.getTotalScore() : BigDecimal.ZERO;
        BigDecimal practiceScore = practice != null ? practice.getTotalScore() : BigDecimal.ZERO;

        BigDecimal totalScore = moralScore.multiply(W_MORAL)
                .add(academicScore.multiply(W_ACADEMIC))
                .add(physicalScore.multiply(W_PHYSICAL))
                .add(artScore.multiply(W_ART))
                .add(practiceScore.multiply(W_PRACTICE))
                .setScale(2, RoundingMode.HALF_UP);

        ComprehensiveEvaluation ce = new ComprehensiveEvaluation();
        ce.setStudentId(studentId);
        ce.setAcademicYear(academicYear);
        ce.setMoralScore(moralScore);
        ce.setAcademicScore(academicScore);
        ce.setPhysicalScore(physicalScore);
        ce.setArtScore(artScore);
        ce.setPracticeScore(practiceScore);
        ce.setTotalScore(totalScore);
        comprehensiveMapper.insert(ce);
    }
}
