package com.sqe.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sqe.entity.StudentInfo;
import com.sqe.mapper.StudentInfoMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 学生信息服务
 */
@Service
public class StudentService extends ServiceImpl<StudentInfoMapper, StudentInfo> {

    /* 分页查询学生 */
    public IPage<StudentInfo> pageList(int current, int size, String keyword, Long classId) {
        return baseMapper.selectStudentPage(new Page<>(current, size), keyword, classId);
    }

    /* 根据用户ID获取学生信息 */
    public StudentInfo getByUserId(Long userId) {
        return this.getOne(new LambdaQueryWrapper<StudentInfo>().eq(StudentInfo::getUserId, userId));
    }

    /* 根据家长ID获取关联学生 */
    public List<StudentInfo> getByParentId(Long parentId) {
        return this.list(new LambdaQueryWrapper<StudentInfo>().eq(StudentInfo::getParentId, parentId));
    }

    /* 根据班级ID获取学生列表 */
    public List<StudentInfo> getByClassId(Long classId) {
        return this.list(new LambdaQueryWrapper<StudentInfo>().eq(StudentInfo::getClassId, classId));
    }
}
