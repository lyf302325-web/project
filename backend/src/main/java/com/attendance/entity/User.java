package com.attendance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String employeeNo;
    private String username;
    private String password;
    private String salt;
    private String name;
    private Long departmentId;
    private Long attendanceGroupId;
    private LocalDate hireDate;
    private LocalDate resignDate;
    private String position;
    private String phone;
    private Integer roleType;
    private Integer status;
    private Integer registerStatus;
    private Integer loginFailCount;
    private LocalDateTime lockUntil;
    private String openid;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableField(exist = false)
    private String departmentName;
}
