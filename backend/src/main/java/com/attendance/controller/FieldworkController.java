package com.attendance.controller;

import com.attendance.common.Result;
import com.attendance.entity.Fieldwork;
import com.attendance.entity.User;
import com.attendance.mapper.FieldworkMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fieldwork")
public class FieldworkController {

    @Autowired
    private FieldworkMapper fieldworkMapper;

    @GetMapping("/list")
    public Result<?> getList(@AuthenticationPrincipal User user,
                             @RequestParam(required = false) Integer status) {
        List<Fieldwork> list;
        if (status != null) {
            LambdaQueryWrapper<Fieldwork> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Fieldwork::getUserId, user.getId())
                   .eq(Fieldwork::getStatus, status)
                   .orderByDesc(Fieldwork::getCreateTime);
            list = fieldworkMapper.selectList(wrapper);
        } else {
            list = fieldworkMapper.selectByUserId(user.getId());
        }
        return Result.success(list);
    }

    @PostMapping("/apply")
    public Result<?> apply(@AuthenticationPrincipal User user, @RequestBody Map<String, Object> request) {
        Fieldwork fieldwork = new Fieldwork();
        fieldwork.setUserId(user.getId());
        fieldwork.setFieldworkDate(LocalDate.parse(request.get("fieldworkDate").toString()));
        fieldwork.setLocation((String) request.get("location"));
        fieldwork.setReason((String) request.get("reason"));
        fieldwork.setStatus(0);
        
        fieldworkMapper.insert(fieldwork);
        return Result.success();
    }
}
