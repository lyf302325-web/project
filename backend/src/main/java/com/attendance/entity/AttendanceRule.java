package com.attendance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("attendance_rule")
public class AttendanceRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String shiftName;
    private Long departmentId;
    private String checkInTime;
    private String checkOutTime;
    private Integer earlyCheckInMinutes;
    private Integer lateThreshold;
    private Integer earlyThreshold;
    private Integer lateCheckOutMinutes;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer checkInRange;
    private Integer photoVerify;
    private Integer fieldworkRequired;
    private Integer enabled;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
