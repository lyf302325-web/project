package com.attendance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("overtime_record")
public class OvertimeRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer overtimeType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal hours;
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
    private String overtimeTypeName;
}
