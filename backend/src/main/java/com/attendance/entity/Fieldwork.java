package com.attendance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("fieldwork")
public class Fieldwork {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private LocalDate fieldworkDate;
    private String location;
    private String reason;
    private String images;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableField(exist = false)
    private String applicantName;
}
