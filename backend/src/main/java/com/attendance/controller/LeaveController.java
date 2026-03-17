package com.attendance.controller;

import com.attendance.common.Result;
import com.attendance.entity.ApprovalFlow;
import com.attendance.entity.Department;
import com.attendance.entity.LeaveRecord;
import com.attendance.entity.UserLeaveBalance;
import com.attendance.entity.User;
import com.attendance.mapper.ApprovalFlowMapper;
import com.attendance.mapper.UserLeaveBalanceMapper;
import com.attendance.service.DepartmentService;
import com.attendance.service.LeaveService;
import com.attendance.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leave")
public class LeaveController {

    @Autowired
    private LeaveService leaveService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private UserService userService;

    @Autowired
    private ApprovalFlowMapper approvalFlowMapper;

    @Autowired
    private UserLeaveBalanceMapper userLeaveBalanceMapper;

    @GetMapping("/list")
    public Result<?> getList(@AuthenticationPrincipal User user,
                             @RequestParam(required = false) Integer status) {
        List<LeaveRecord> list;
        if (status != null) {
            list = leaveService.findByStatus(status);
            list.removeIf(r -> !r.getUserId().equals(user.getId()));
        } else {
            list = leaveService.findByUserId(user.getId());
        }
        
        for (LeaveRecord record : list) {
            record.setLeaveTypeName(getLeaveTypeName(record.getLeaveType()));
        }
        return Result.success(list);
    }

    @GetMapping("/detail/{id}")
    public Result<?> getDetail(@PathVariable Long id) {
        LeaveRecord record = leaveService.getById(id);
        if (record != null) {
            record.setLeaveTypeName(getLeaveTypeName(record.getLeaveType()));
        }
        return Result.success(record);
    }

    @PostMapping("/apply")
    public Result<?> apply(@AuthenticationPrincipal User user, @RequestBody Map<String, Object> request) {
        if (user.getDepartmentId() == null) return Result.error("未分配部门");
        Department dept = departmentService.getById(user.getDepartmentId());
        if (dept == null || dept.getManagerId() == null) return Result.error("部门未配置主管");

        Integer leaveType = Integer.valueOf(request.get("leaveType").toString());
        BigDecimal days = new BigDecimal(request.get("days").toString());
        if (days.compareTo(BigDecimal.ZERO) <= 0) return Result.error("请假天数不合法");

        int year = LocalDate.now().getYear();
        UserLeaveBalance balance = userLeaveBalanceMapper.selectOne(new LambdaQueryWrapper<UserLeaveBalance>()
                .eq(UserLeaveBalance::getUserId, user.getId())
                .eq(UserLeaveBalance::getLeaveType, leaveType)
                .eq(UserLeaveBalance::getYear, year));
        if (balance != null && balance.getRemainingDays() != null) {
            if (days.compareTo(balance.getRemainingDays()) > 0) {
                return Result.error("请假天数超过剩余余额");
            }
        }

        LeaveRecord record = new LeaveRecord();
        record.setUserId(user.getId());
        record.setLeaveType(leaveType);
        record.setStartDate(LocalDate.parse(request.get("startDate").toString()));
        record.setEndDate(LocalDate.parse(request.get("endDate").toString()));
        record.setDays(days);
        record.setReason((String) request.get("reason"));
        record.setImages(toJson(request.get("images")));
        record.setStatus(0);
        record.setCurrentApproverId(dept.getManagerId());
        record.setCurrentStage(1);
        record.setTotalStage(resolveTotalStage(user.getDepartmentId(), leaveType, days));
        
        leaveService.save(record);
        return Result.success();
    }

    @PostMapping("/cancel/{id}")
    public Result<?> cancel(@PathVariable Long id) {
        LeaveRecord record = leaveService.getById(id);
        if (record != null && record.getStatus() == 0) {
            record.setStatus(3);
            leaveService.updateById(record);
        }
        return Result.success();
    }

    private String getLeaveTypeName(Integer type) {
        switch (type) {
            case 1: return "事假";
            case 2: return "病假";
            case 3: return "年假";
            case 4: return "婚假";
            case 5: return "产假";
            case 6: return "丧假";
            default: return "其他";
        }
    }

    private Integer resolveTotalStage(Long departmentId, Integer leaveType, BigDecimal days) {
        List<ApprovalFlow> flows = approvalFlowMapper.selectAllWithApprover();
        flows.removeIf(f -> !"leave".equalsIgnoreCase(f.getRecordType() == null ? "leave" : f.getRecordType()));
        if (departmentId != null) {
            flows.removeIf(f -> f.getDepartmentId() != null && !departmentId.equals(f.getDepartmentId()));
        }
        flows.removeIf(f -> f.getLeaveType() != null && !f.getLeaveType().equals(leaveType));
        flows.removeIf(f -> days.compareTo(f.getMinDays()) < 0 || days.compareTo(f.getMaxDays()) > 0);

        ApprovalFlow selected = null;
        for (ApprovalFlow f : flows) {
            if (selected == null) {
                selected = f;
                continue;
            }
            boolean fd = f.getDepartmentId() != null;
            boolean sd = selected.getDepartmentId() != null;
            if (fd && !sd) {
                selected = f;
                continue;
            }
            boolean fl = f.getLeaveType() != null;
            boolean sl = selected.getLeaveType() != null;
            if (fl && !sl) {
                selected = f;
            }
        }
        boolean needAdmin = selected != null && ((selected.getNeedAdmin() != null && selected.getNeedAdmin() == 1) || (selected.getLevel() != null && selected.getLevel() >= 2));
        return needAdmin ? 2 : 1;
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
