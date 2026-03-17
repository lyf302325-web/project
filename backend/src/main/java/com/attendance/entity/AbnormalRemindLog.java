package com.attendance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("abnormal_remind_log")
public class AbnormalRemindLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private LocalDate attendanceDate;
    private String remindType;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
