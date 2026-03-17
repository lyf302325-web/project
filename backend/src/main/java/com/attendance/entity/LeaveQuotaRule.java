package com.attendance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("leave_quota_rule")
public class LeaveQuotaRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer leaveType;
    private Integer year;
    private BigDecimal totalDays;
    private String position;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
