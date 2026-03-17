package com.attendance.mapper;

import com.attendance.entity.Appeal;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AppealMapper extends BaseMapper<Appeal> {
    
    @Select("SELECT a.*, u.name as applicant_name FROM appeal a LEFT JOIN user u ON a.user_id = u.id WHERE a.status = #{status} ORDER BY a.create_time DESC")
    List<Appeal> selectByStatus(Integer status);
    
    @Select("SELECT a.*, u.name as applicant_name FROM appeal a LEFT JOIN user u ON a.user_id = u.id WHERE a.user_id = #{userId} ORDER BY a.create_time DESC")
    List<Appeal> selectByUserId(Long userId);
}
