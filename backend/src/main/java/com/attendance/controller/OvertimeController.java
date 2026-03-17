package com.attendance.controller;

import com.attendance.common.Result;
import com.attendance.entity.Department;
import com.attendance.entity.OvertimeRecord;
import com.attendance.entity.User;
import com.attendance.mapper.OvertimeRecordMapper;
import com.attendance.service.DepartmentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/overtime")
public class OvertimeController {

    @Autowired
    private OvertimeRecordMapper overtimeRecordMapper;

    @Autowired
    private DepartmentService departmentService;

    @GetMapping("/list")
    public Result<?> list(@AuthenticationPrincipal User user, @RequestParam(required = false) Integer status) {
        List<OvertimeRecord> list;
        if (status != null) {
            list = overtimeRecordMapper.selectList(new LambdaQueryWrapper<OvertimeRecord>()
                    .eq(OvertimeRecord::getUserId, user.getId())
                    .eq(OvertimeRecord::getStatus, status)
                    .orderByDesc(OvertimeRecord::getCreateTime));
        } else {
            list = overtimeRecordMapper.selectByUserId(user.getId());
        }
        for (OvertimeRecord r : list) {
            r.setOvertimeTypeName(getOvertimeTypeName(r.getOvertimeType()));
        }
        return Result.success(list);
    }

    @GetMapping("/detail/{id}")
    public Result<?> detail(@AuthenticationPrincipal User user, @PathVariable Long id) {
        OvertimeRecord record = overtimeRecordMapper.selectById(id);
        if (record == null || !record.getUserId().equals(user.getId())) return Result.error("记录不存在");
        record.setOvertimeTypeName(getOvertimeTypeName(record.getOvertimeType()));
        return Result.success(record);
    }

    @PostMapping("/apply")
    public Result<?> apply(@AuthenticationPrincipal User user, @RequestBody Map<String, Object> request) {
        if (user.getDepartmentId() == null) return Result.error("未分配部门");
        Department dept = departmentService.getById(user.getDepartmentId());
        if (dept == null || dept.getManagerId() == null) return Result.error("部门未配置主管");

        Integer overtimeType = Integer.valueOf(request.get("overtimeType").toString());
        LocalDateTime startTime = parseDateTime(request.get("startTime"));
        LocalDateTime endTime = parseDateTime(request.get("endTime"));
        if (startTime == null || endTime == null) return Result.error("时间格式不正确");
        if (!endTime.isAfter(startTime)) return Result.error("结束时间必须晚于开始时间");

        BigDecimal hours = calcHours(startTime, endTime);

        OvertimeRecord record = new OvertimeRecord();
        record.setUserId(user.getId());
        record.setOvertimeType(overtimeType);
        record.setStartTime(startTime);
        record.setEndTime(endTime);
        record.setHours(hours);
        record.setReason(request.get("reason") == null ? "" : request.get("reason").toString());
        record.setImages(toJson(request.get("images")));
        record.setStatus(0);
        record.setCurrentStage(1);
        record.setTotalStage(2);
        record.setCurrentApproverId(dept.getManagerId());
        overtimeRecordMapper.insert(record);
        return Result.success();
    }

    @PostMapping("/cancel/{id}")
    public Result<?> cancel(@AuthenticationPrincipal User user, @PathVariable Long id) {
        OvertimeRecord record = overtimeRecordMapper.selectById(id);
        if (record == null || !record.getUserId().equals(user.getId())) return Result.error("记录不存在");
        if (record.getStatus() == null || (record.getStatus() != 0 && record.getStatus() != 1)) return Result.error("当前状态不可撤销");
        record.setStatus(4);
        record.setCurrentApproverId(null);
        overtimeRecordMapper.updateById(record);
        return Result.success();
    }

    private LocalDateTime parseDateTime(Object value) {
        if (value == null) return null;
        String s = value.toString().trim();
        if (s.isEmpty()) return null;
        try {
            return LocalDateTime.parse(s);
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception ignored) {
        }
        return null;
    }

    private BigDecimal calcHours(LocalDateTime start, LocalDateTime end) {
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes < 0) minutes = 0;
        return new BigDecimal(minutes).divide(new BigDecimal("60"), 2, java.math.RoundingMode.HALF_UP);
    }

    private String getOvertimeTypeName(Integer type) {
        if (type == null) return "其他";
        if (type == 1) return "工作日";
        if (type == 2) return "周末";
        if (type == 3) return "节假日";
        return "其他";
    }

    private String toJson(Object value) {
        if (value == null) return null;
        if (value instanceof String) return (String) value;
        try {
            return new ObjectMapper().writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }
}

