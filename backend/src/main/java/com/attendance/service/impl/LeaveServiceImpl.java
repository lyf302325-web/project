package com.attendance.service.impl;

import com.attendance.entity.LeaveRecord;
import com.attendance.mapper.LeaveRecordMapper;
import com.attendance.service.LeaveService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LeaveServiceImpl extends ServiceImpl<LeaveRecordMapper, LeaveRecord> implements LeaveService {

    @Override
    public List<LeaveRecord> findByUserId(Long userId) {
        return baseMapper.selectByUserId(userId);
    }

    @Override
    public List<LeaveRecord> findByStatus(Integer status) {
        return baseMapper.selectByStatus(status);
    }

    @Override
    public void approve(Long id, Integer status, String opinion) {
        LeaveRecord record = getById(id);
        if (record != null) {
            record.setStatus(status);
            updateById(record);
        }
    }
}
