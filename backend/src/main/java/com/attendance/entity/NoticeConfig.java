package com.attendance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("notice_config")
public class NoticeConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer attendanceAlert;
    private Integer approvalResult;
    private Integer pendingApproval;
    private Integer checkInReminder;
    private String morningReminderTime;
    private String eveningReminderTime;
    private Integer missCheckInAfterMinutes;
    private Integer missCheckOutAfterMinutes;
    private Integer lateRemind;
    private Integer earlyRemind;
    private Integer missCheckInRemind;
    private Integer missCheckOutRemind;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
