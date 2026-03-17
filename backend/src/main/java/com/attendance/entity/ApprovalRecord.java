package com.attendance.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("approval_record")
public class ApprovalRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String recordType;
    private Long recordId;
    private Long approverId;
    private Integer status;
    private String opinion;
    private LocalDateTime approvalTime;
    private Integer level;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String approverName;
}

