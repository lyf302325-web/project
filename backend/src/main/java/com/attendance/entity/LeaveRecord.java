package com.attendance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("leave_record")
public class LeaveRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal days;
    private String reason;
    private String images;
    private Integer status;
    private Long currentApproverId;
    private Integer currentStage;
    private Integer totalStage;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableField(exist = false)
    private String applicantName;
    @TableField(exist = false)
    private String leaveTypeName;
}
