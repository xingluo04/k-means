package com.sqe.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqe.common.Result;
import com.sqe.entity.SysUser;
import com.sqe.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制器
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    /* 分页查询用户列表 */
    @GetMapping("/page")
    public Result<Page<SysUser>> page(@RequestParam(defaultValue = "1") int current,
                                       @RequestParam(defaultValue = "10") int size,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) String role) {
        return Result.success(userService.pageList(current, size, keyword, role));
    }

    /* 获取当前登录用户信息 */
    @GetMapping("/info")
    public Result<SysUser> info(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(userService.getById(userId));
    }

    /* 新增用户 */
    @PostMapping
    public Result<?> add(@RequestBody SysUser user) {
        boolean success = userService.addUser(user);
        return success ? Result.success() : Result.error("用户名已存在");
    }

    /* 修改用户 */
    @PutMapping
    public Result<?> update(@RequestBody SysUser user) {
        userService.updateUser(user);
        return Result.success();
    }

    /* 删除用户 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        userService.removeById(id);
        return Result.success();
    }

    /* 重置密码 */
    @PutMapping("/resetPwd/{id}")
    public Result<?> resetPassword(@PathVariable Long id) {
        userService.resetPassword(id, "123456");
        return Result.success();
    }

    /* 修改状态 */
    @PutMapping("/status/{id}/{status}")
    public Result<?> changeStatus(@PathVariable Long id, @PathVariable Integer status) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(status);
        userService.updateById(user);
        return Result.success();
    }
}
