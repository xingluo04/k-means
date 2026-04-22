package com.sqe.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqe.common.Result;
import com.sqe.entity.SysNotice;
import com.sqe.service.NoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 通知管理控制器
 */
@RestController
@RequestMapping("/api/notice")
public class NoticeController {

    @Autowired
    private NoticeService noticeService;

    /* 分页查询通知 */
    @GetMapping("/page")
    public Result<Page<SysNotice>> page(@RequestParam(defaultValue = "1") int current,
                                         @RequestParam(defaultValue = "10") int size,
                                         @RequestParam(required = false) String keyword) {
        return Result.success(noticeService.pageList(current, size, keyword));
    }

    /* 获取通知详情 */
    @GetMapping("/{id}")
    public Result<SysNotice> detail(@PathVariable Long id) {
        return Result.success(noticeService.getById(id));
    }

    /* 新增通知 */
    @PostMapping
    public Result<?> add(@RequestBody SysNotice notice) {
        noticeService.save(notice);
        return Result.success();
    }

    /* 修改通知 */
    @PutMapping
    public Result<?> update(@RequestBody SysNotice notice) {
        noticeService.updateById(notice);
        return Result.success();
    }

    /* 删除通知 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        noticeService.removeById(id);
        return Result.success();
    }
}
