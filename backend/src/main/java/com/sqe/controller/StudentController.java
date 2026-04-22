package com.sqe.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqe.common.Result;
import com.sqe.entity.StudentInfo;
import com.sqe.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学生信息控制器
 */
@RestController
@RequestMapping("/api/student")
public class StudentController {

    @Autowired
    private StudentService studentService;

    /* 分页查询学生 */
    @GetMapping("/page")
    public Result<IPage<StudentInfo>> page(@RequestParam(defaultValue = "1") int current,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Long classId) {
        return Result.success(studentService.pageList(current, size, keyword, classId));
    }

    /* 获取当前学生信息 */
    @GetMapping("/my")
    public Result<StudentInfo> myInfo(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(studentService.getByUserId(userId));
    }

    /* 获取家长关联的学生列表 */
    @GetMapping("/children")
    public Result<List<StudentInfo>> children(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(studentService.getByParentId(userId));
    }

    /* 根据班级获取学生列表 */
    @GetMapping("/byClass/{classId}")
    public Result<List<StudentInfo>> byClass(@PathVariable Long classId) {
        return Result.success(studentService.getByClassId(classId));
    }

    /* 新增学生信息 */
    @PostMapping
    public Result<?> add(@RequestBody StudentInfo studentInfo) {
        studentService.save(studentInfo);
        return Result.success();
    }

    /* 修改学生信息 */
    @PutMapping
    public Result<?> update(@RequestBody StudentInfo studentInfo) {
        studentService.updateById(studentInfo);
        return Result.success();
    }

    /* 删除学生信息 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        studentService.removeById(id);
        return Result.success();
    }
}
