package com.attendance.controller;

import com.attendance.common.Result;
import com.attendance.entity.Appeal;
import com.attendance.entity.User;
import com.attendance.mapper.AppealMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appeal")
public class AppealController {

    @Autowired
    private AppealMapper appealMapper;

    @GetMapping("/list")
    public Result<?> getList(@AuthenticationPrincipal User user,
                             @RequestParam(required = false) Integer status) {
        List<Appeal> list;
        if (status != null) {
            LambdaQueryWrapper<Appeal> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Appeal::getUserId, user.getId())
                   .eq(Appeal::getStatus, status)
                   .orderByDesc(Appeal::getCreateTime);
            list = appealMapper.selectList(wrapper);
        } else {
            list = appealMapper.selectByUserId(user.getId());
        }
        
        for (Appeal appeal : list) {
            appeal.setAppealTypeName(getAppealTypeName(appeal.getAppealType()));
        }
        return Result.success(list);
    }

    @PostMapping("/apply")
    public Result<?> apply(@AuthenticationPrincipal User user, @RequestBody Map<String, Object> request) {
        Appeal appeal = new Appeal();
        appeal.setUserId(user.getId());
        appeal.setAttendanceDate(LocalDate.parse(request.get("attendanceDate").toString()));
        appeal.setAppealType(Integer.valueOf(request.get("appealType").toString()));
        appeal.setReason((String) request.get("reason"));
        appeal.setStatus(0);
        
        appealMapper.insert(appeal);
        return Result.success();
    }

    private String getAppealTypeName(Integer type) {
        switch (type) {
            case 1: return "漏签到";
            case 2: return "漏签退";
            case 3: return "定位失败";
            case 4: return "设备故障";
            case 5: return "其他";
            default: return "其他";
        }
    }
}
