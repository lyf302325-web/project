package com.attendance.mapper;

import com.attendance.entity.LeaveRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LeaveRecordMapper extends BaseMapper<LeaveRecord> {
    
    @Select("SELECT l.*, u.name as applicant_name FROM leave_record l LEFT JOIN user u ON l.user_id = u.id WHERE l.status = #{status} ORDER BY l.create_time DESC")
    List<LeaveRecord> selectByStatus(Integer status);
    
    @Select("SELECT l.*, u.name as applicant_name FROM leave_record l LEFT JOIN user u ON l.user_id = u.id WHERE l.user_id = #{userId} ORDER BY l.create_time DESC")
    List<LeaveRecord> selectByUserId(Long userId);
}
