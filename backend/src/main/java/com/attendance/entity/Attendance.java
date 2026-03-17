package com.attendance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("attendance")
public class Attendance {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private LocalDate attendanceDate;
    private String checkInTime;
    private String checkOutTime;
    private Integer checkInStatus;
    private Integer checkOutStatus;
    private String checkInLocation;
    private String checkOutLocation;
    private String checkInPhoto;
    private String checkOutPhoto;
    private Integer status;
    private Integer isAbnormal;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
