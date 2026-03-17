package com.attendance.mapper;

import com.attendance.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    @Select("SELECT u.*, d.name as department_name FROM user u LEFT JOIN department d ON u.department_id = d.id WHERE u.id = #{id}")
    User selectUserWithDepartment(Long id);
    
    @Select("SELECT u.*, d.name as department_name FROM user u LEFT JOIN department d ON u.department_id = d.id WHERE u.username = #{username}")
    User selectByUsername(String username);
    
    @Select("SELECT u.*, d.name as department_name FROM user u LEFT JOIN department d ON u.department_id = d.id ORDER BY u.id DESC")
    List<User> selectAllWithDepartment();
    
    @Select("SELECT u.*, d.name as department_name FROM user u LEFT JOIN department d ON u.department_id = d.id WHERE u.name LIKE CONCAT('%', #{keyword}, '%') OR u.employee_no LIKE CONCAT('%', #{keyword}, '%') ORDER BY u.id DESC")
    List<User> searchByKeyword(String keyword);
}
