package com.attendance.mapper;

import com.attendance.entity.Fieldwork;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FieldworkMapper extends BaseMapper<Fieldwork> {
    
    @Select("SELECT f.*, u.name as applicant_name FROM fieldwork f LEFT JOIN user u ON f.user_id = u.id WHERE f.status = #{status} ORDER BY f.create_time DESC")
    List<Fieldwork> selectByStatus(Integer status);
    
    @Select("SELECT f.*, u.name as applicant_name FROM fieldwork f LEFT JOIN user u ON f.user_id = u.id WHERE f.user_id = #{userId} ORDER BY f.create_time DESC")
    List<Fieldwork> selectByUserId(Long userId);
}
