package com.attendance.service;

import com.attendance.entity.Department;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface DepartmentService extends IService<Department> {
    List<Department> findAllWithManager();
}
