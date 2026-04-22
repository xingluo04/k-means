package com.sqe.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqe.common.Result;
import com.sqe.entity.SysLog;
import com.sqe.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 操作日志控制器
 */
@RestController
@RequestMapping("/api/log")
public class LogController {

    @Autowired
    private LogService logService;

    /* 分页查询日志 */
    @GetMapping("/page")
    public Result<Page<SysLog>> page(@RequestParam(defaultValue = "1") int current,
                                      @RequestParam(defaultValue = "10") int size,
                                      @RequestParam(required = false) String keyword) {
        return Result.success(logService.pageList(current, size, keyword));
    }

    /* 清空日志 */
    @DeleteMapping("/clear")
    public Result<?> clear() {
        logService.remove(null);
        return Result.success();
    }
}
