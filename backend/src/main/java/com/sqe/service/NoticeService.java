package com.sqe.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sqe.entity.SysNotice;
import com.sqe.entity.SysUser;
import com.sqe.mapper.SysNoticeMapper;
import com.sqe.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 通知管理服务
 */
@Service
public class NoticeService extends ServiceImpl<SysNoticeMapper, SysNotice> {

    @Autowired
    private SysUserMapper userMapper;

    /* 分页查询通知 */
    public Page<SysNotice> pageList(int current, int size, String keyword) {
        LambdaQueryWrapper<SysNotice> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(SysNotice::getTitle, keyword);
        }
        wrapper.orderByDesc(SysNotice::getCreateTime);
        Page<SysNotice> page = this.page(new Page<>(current, size), wrapper);
        page.getRecords().forEach(notice -> {
            if (notice.getPublisherId() != null) {
                SysUser user = userMapper.selectById(notice.getPublisherId());
                if (user != null) notice.setPublisherName(user.getRealName());
            }
        });
        return page;
    }
}
