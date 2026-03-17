package com.attendance.controller;

import com.attendance.common.Result;
import com.attendance.entity.ApprovalFlow;
import com.attendance.entity.Department;
import com.attendance.entity.User;
import com.attendance.mapper.ApprovalFlowMapper;
import com.attendance.service.DepartmentService;
import com.attendance.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;

@RestController
@RequestMapping("/api/approval")
public class ApprovalController {

    @Autowired
    private ApprovalFlowMapper approvalFlowMapper;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private UserService userService;

    @GetMapping("/flow")
    public Result<?> getApprovalFlow(@AuthenticationPrincipal User user,
                                     @RequestParam BigDecimal days,
                                     @RequestParam(required = false) Integer leaveType) {
        List<ApprovalFlow> flows = approvalFlowMapper.selectAllWithApprover();

        flows.removeIf(f -> !"leave".equalsIgnoreCase(f.getRecordType() == null ? "leave" : f.getRecordType()));
        Long deptId = user == null ? null : user.getDepartmentId();
        if (deptId != null) {
            flows.removeIf(f -> f.getDepartmentId() != null && !f.getDepartmentId().equals(deptId));
        }
        if (leaveType != null) {
            flows.removeIf(f -> f.getLeaveType() != null && !f.getLeaveType().equals(leaveType));
        }
        flows.removeIf(f -> days.compareTo(f.getMinDays()) < 0 || days.compareTo(f.getMaxDays()) > 0);

        ApprovalFlow selected = flows.stream()
                .sorted(Comparator
                        .comparing((ApprovalFlow f) -> f.getDepartmentId() != null).reversed()
                        .thenComparing(f -> f.getLeaveType() != null, Comparator.reverseOrder())
                        .thenComparing(f -> f.getMaxDays().subtract(f.getMinDays()))
                        .thenComparing(f -> f.getLevel() == null ? 1 : f.getLevel(), Comparator.reverseOrder())
                        .thenComparing(f -> f.getId() == null ? 0L : f.getId(), Comparator.reverseOrder()))
                .findFirst().orElse(null);

        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> s1 = new HashMap<>();
        s1.put("level", 1);
        s1.put("role", "manager");
        String managerName = "部门主管";
        if (user != null && user.getDepartmentId() != null) {
            Department dept = departmentService.getById(user.getDepartmentId());
            if (dept != null && dept.getManagerId() != null) {
                User mgr = userService.getById(dept.getManagerId());
                if (mgr != null && mgr.getName() != null) managerName = mgr.getName();
            }
        }
        s1.put("approverName", managerName);
        result.add(s1);

        boolean needAdmin = selected != null && ((selected.getNeedAdmin() != null && selected.getNeedAdmin() == 1) || (selected.getLevel() != null && selected.getLevel() >= 2));
        if (needAdmin) {
            Map<String, Object> s2 = new HashMap<>();
            s2.put("level", 2);
            s2.put("role", "admin");
            String adminName = selected.getApproverName();
            if (adminName == null || adminName.isEmpty()) {
                User admin = userService.getOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getRoleType, 1)
                        .last("limit 1"));
                if (admin != null) adminName = admin.getName();
            }
            s2.put("approverName", adminName == null ? "管理员" : adminName);
            result.add(s2);
        }

        return Result.success(result);
    }
}
