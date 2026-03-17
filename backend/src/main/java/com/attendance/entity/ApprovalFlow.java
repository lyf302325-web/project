package com.attendance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("approval_flow")
public class ApprovalFlow {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String recordType;
    private Integer leaveType;
    private BigDecimal minDays;
    private BigDecimal maxDays;
    private Integer needAdmin;
    private Long approverId;
    private Integer level;
    private Long departmentId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableField(exist = false)
    private String approverName;
}
