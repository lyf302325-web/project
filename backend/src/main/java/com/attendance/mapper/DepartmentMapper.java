package com.attendance.mapper;

import com.attendance.entity.Department;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DepartmentMapper extends BaseMapper<Department> {
    
    @Select("SELECT d.*, u.name as manager_name FROM department d LEFT JOIN user u ON d.manager_id = u.id ORDER BY d.id")
    List<Department> selectAllWithManager();
}
