package com.attendance.service.impl;

import com.attendance.entity.Department;
import com.attendance.mapper.DepartmentMapper;
import com.attendance.service.DepartmentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department> implements DepartmentService {

    @Override
    public List<Department> findAllWithManager() {
        return baseMapper.selectAllWithManager();
    }
}
