package com.sqe.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sqe.entity.SysLog;
import com.sqe.mapper.SysLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 操作日志服务
 */
@Service
public class LogService extends ServiceImpl<SysLogMapper, SysLog> {

    /* 分页查询日志 */
    public Page<SysLog> pageList(int current, int size, String keyword) {
        LambdaQueryWrapper<SysLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysLog::getUsername, keyword)
                    .or().like(SysLog::getOperation, keyword));
        }
        wrapper.orderByDesc(SysLog::getCreateTime);
        return this.page(new Page<>(current, size), wrapper);
    }
}
