package com.attendance.service.impl;

import com.attendance.entity.User;
import com.attendance.mapper.UserMapper;
import com.attendance.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public User findByUsername(String username) {
        return baseMapper.selectByUsername(username);
    }

    @Override
    public User findByIdWithDepartment(Long id) {
        return baseMapper.selectUserWithDepartment(id);
    }

    @Override
    public List<User> findAllWithDepartment() {
        return baseMapper.selectAllWithDepartment();
    }

    @Override
    public List<User> searchByKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAllWithDepartment();
        }
        return baseMapper.searchByKeyword(keyword);
    }
}
