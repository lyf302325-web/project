package com.attendance.service;

import com.attendance.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface UserService extends IService<User> {
    User findByUsername(String username);
    User findByIdWithDepartment(Long id);
    List<User> findAllWithDepartment();
    List<User> searchByKeyword(String keyword);
}
