package com.attendance.service;

import com.attendance.entity.LeaveRecord;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface LeaveService extends IService<LeaveRecord> {
    List<LeaveRecord> findByUserId(Long userId);
    List<LeaveRecord> findByStatus(Integer status);
    void approve(Long id, Integer status, String opinion);
}
