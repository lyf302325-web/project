package com.attendance.mapper;

import com.attendance.entity.ApprovalFlow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ApprovalFlowMapper extends BaseMapper<ApprovalFlow> {
    
    @Select("SELECT af.*, u.name as approver_name FROM approval_flow af LEFT JOIN user u ON af.approver_id = u.id ORDER BY af.min_days")
    List<ApprovalFlow> selectAllWithApprover();
}
