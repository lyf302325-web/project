package com.attendance.mapper;

import com.attendance.entity.OvertimeRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OvertimeRecordMapper extends BaseMapper<OvertimeRecord> {

    @Select("SELECT o.*, u.name as applicant_name FROM overtime_record o LEFT JOIN user u ON o.user_id = u.id WHERE o.user_id = #{userId} ORDER BY o.create_time DESC")
    List<OvertimeRecord> selectByUserId(Long userId);

    @Select("SELECT o.*, u.name as applicant_name FROM overtime_record o LEFT JOIN user u ON o.user_id = u.id WHERE o.status = #{status} ORDER BY o.create_time DESC")
    List<OvertimeRecord> selectByStatus(Integer status);
}
