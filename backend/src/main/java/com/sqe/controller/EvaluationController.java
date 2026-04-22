package com.sqe.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqe.common.Result;
import com.sqe.entity.*;
import com.sqe.service.EvaluationService;
import com.sqe.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评价管理控制器
 */
@RestController
@RequestMapping("/api/evaluation")
public class EvaluationController {

    @Autowired
    private EvaluationService evaluationService;
    @Autowired
    private StudentService studentService;

    /* 品德评价分页 */
    @GetMapping("/moral/page")
    public Result<IPage<MoralEvaluation>> moralPage(@RequestParam(defaultValue = "1") int current,
                                                     @RequestParam(defaultValue = "10") int size,
                                                     @RequestParam(required = false) Long studentId,
                                                     @RequestParam(required = false) String semester) {
        return Result.success(evaluationService.moralPage(current, size, studentId, semester));
    }

    /* 保存品德评价 */
    @PostMapping("/moral")
    public Result<?> saveMoral(@RequestBody MoralEvaluation eval) {
        evaluationService.saveMoral(eval);
        return Result.success();
    }

    /* 删除品德评价 */
    @DeleteMapping("/moral/{id}")
    public Result<?> deleteMoral(@PathVariable Long id) {
        evaluationService.deleteMoral(id);
        return Result.success();
    }

    /* 学业评价分页 */
    @GetMapping("/academic/page")
    public Result<IPage<AcademicEvaluation>> academicPage(@RequestParam(defaultValue = "1") int current,
                                                           @RequestParam(defaultValue = "10") int size,
                                                           @RequestParam(required = false) Long studentId,
                                                           @RequestParam(required = false) String semester) {
        return Result.success(evaluationService.academicPage(current, size, studentId, semester));
    }

    /* 保存学业评价 */
    @PostMapping("/academic")
    public Result<?> saveAcademic(@RequestBody AcademicEvaluation eval) {
        evaluationService.saveAcademic(eval);
        return Result.success();
    }

    /* 删除学业评价 */
    @DeleteMapping("/academic/{id}")
    public Result<?> deleteAcademic(@PathVariable Long id) {
        evaluationService.deleteAcademic(id);
        return Result.success();
    }

    /* 体能评价分页 */
    @GetMapping("/physical/page")
    public Result<IPage<PhysicalEvaluation>> physicalPage(@RequestParam(defaultValue = "1") int current,
                                                           @RequestParam(defaultValue = "10") int size,
                                                           @RequestParam(required = false) Long studentId,
                                                           @RequestParam(required = false) String semester) {
        return Result.success(evaluationService.physicalPage(current, size, studentId, semester));
    }

    /* 保存体能评价 */
    @PostMapping("/physical")
    public Result<?> savePhysical(@RequestBody PhysicalEvaluation eval) {
        evaluationService.savePhysical(eval);
        return Result.success();
    }

    /* 删除体能评价 */
    @DeleteMapping("/physical/{id}")
    public Result<?> deletePhysical(@PathVariable Long id) {
        evaluationService.deletePhysical(id);
        return Result.success();
    }

    /* 艺术评价分页 */
    @GetMapping("/art/page")
    public Result<IPage<ArtEvaluation>> artPage(@RequestParam(defaultValue = "1") int current,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 @RequestParam(required = false) Long studentId,
                                                 @RequestParam(required = false) String semester) {
        return Result.success(evaluationService.artPage(current, size, studentId, semester));
    }

    /* 保存艺术评价 */
    @PostMapping("/art")
    public Result<?> saveArt(@RequestBody ArtEvaluation eval) {
        evaluationService.saveArt(eval);
        return Result.success();
    }

    /* 删除艺术评价 */
    @DeleteMapping("/art/{id}")
    public Result<?> deleteArt(@PathVariable Long id) {
        evaluationService.deleteArt(id);
        return Result.success();
    }

    /* 实践评价分页 */
    @GetMapping("/practice/page")
    public Result<IPage<PracticeEvaluation>> practicePage(@RequestParam(defaultValue = "1") int current,
                                                           @RequestParam(defaultValue = "10") int size,
                                                           @RequestParam(required = false) Long studentId,
                                                           @RequestParam(required = false) String semester) {
        return Result.success(evaluationService.practicePage(current, size, studentId, semester));
    }

    /* 保存实践评价 */
    @PostMapping("/practice")
    public Result<?> savePractice(@RequestBody PracticeEvaluation eval) {
        evaluationService.savePractice(eval);
        return Result.success();
    }

    /* 删除实践评价 */
    @DeleteMapping("/practice/{id}")
    public Result<?> deletePractice(@PathVariable Long id) {
        evaluationService.deletePractice(id);
        return Result.success();
    }

    /* 综合评价分页 */
    @GetMapping("/comprehensive/page")
    public Result<IPage<ComprehensiveEvaluation>> comprehensivePage(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) Integer clusterLabel,
            @RequestParam(required = false) String keyword) {
        return Result.success(evaluationService.comprehensivePage(current, size, studentId, semester, clusterLabel, keyword));
    }

    /* 获取学生综合评价详情 */
    @GetMapping("/comprehensive/detail")
    public Result<ComprehensiveEvaluation> comprehensiveDetail(@RequestParam Long studentId,
                                                                @RequestParam String semester) {
        return Result.success(evaluationService.getStudentComprehensive(studentId, semester));
    }

    /* 获取学生成长轨迹 */
    @GetMapping("/comprehensive/growth/{studentId}")
    public Result<List<ComprehensiveEvaluation>> growth(@PathVariable Long studentId) {
        return Result.success(evaluationService.getStudentAllSemesters(studentId));
    }

    /* 获取当前学生的综合评价（学生角色） */
    @GetMapping("/comprehensive/my")
    public Result<List<ComprehensiveEvaluation>> myComprehensive(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        StudentInfo student = studentService.getByUserId(userId);
        if (student == null) return Result.error("未找到学生信息");
        return Result.success(evaluationService.getStudentAllSemesters(student.getId()));
    }
}
