package com.sqe.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqe.common.Result;
import com.sqe.entity.ClassInfo;
import com.sqe.service.ClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 班级管理控制器
 */
@RestController
@RequestMapping("/api/class")
public class ClassController {

    @Autowired
    private ClassService classService;

    /* 分页查询班级 */
    @GetMapping("/page")
    public Result<Page<ClassInfo>> page(@RequestParam(defaultValue = "1") int current,
                                         @RequestParam(defaultValue = "10") int size,
                                         @RequestParam(required = false) String keyword) {
        return Result.success(classService.pageList(current, size, keyword));
    }

    /* 获取所有班级列表 */
    @GetMapping("/list")
    public Result<List<ClassInfo>> list() {
        return Result.success(classService.listAll());
    }

    /* 新增班级 */
    @PostMapping
    public Result<?> add(@RequestBody ClassInfo classInfo) {
        classService.save(classInfo);
        return Result.success();
    }

    /* 修改班级 */
    @PutMapping
    public Result<?> update(@RequestBody ClassInfo classInfo) {
        classService.updateById(classInfo);
        return Result.success();
    }

    /* 删除班级 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        classService.removeById(id);
        return Result.success();
    }
}
