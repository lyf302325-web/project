package com.attendance.controller;

import com.attendance.common.Result;
import com.attendance.entity.LeaveQuotaRule;
import com.attendance.entity.User;
import com.attendance.entity.UserLeaveBalance;
import com.attendance.mapper.LeaveQuotaRuleMapper;
import com.attendance.mapper.UserLeaveBalanceMapper;
import com.attendance.service.UserService;
import com.attendance.util.PasswordUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserLeaveBalanceMapper userLeaveBalanceMapper;

    @Autowired
    private LeaveQuotaRuleMapper leaveQuotaRuleMapper;

    @GetMapping("/info")
    public Result<?> getUserInfo(@AuthenticationPrincipal User user) {
        User userInfo = userService.findByIdWithDepartment(user.getId());
        userInfo.setPassword(null);
        return Result.success(userInfo);
    }

    @PostMapping("/change-password")
    public Result<?> changePassword(@AuthenticationPrincipal User user,
                                    @RequestBody Map<String, String> request) {
        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");

        User currentUser = userService.getById(user.getId());
        if (!PasswordUtil.matches(oldPassword, currentUser.getSalt(), currentUser.getPassword())) {
            return Result.error("原密码错误");
        }

        String newSalt = PasswordUtil.generateSalt();
        currentUser.setSalt(newSalt);
        currentUser.setPassword(PasswordUtil.encode(newPassword, newSalt));
        userService.updateById(currentUser);
        return Result.success();
    }

    @PostMapping("/unbind-wx")
    public Result<?> unbindWx(@AuthenticationPrincipal User user) {
        User currentUser = userService.getById(user.getId());
        if (currentUser.getOpenid() == null || currentUser.getOpenid().isEmpty()) {
            return Result.error("当前账号未绑定微信");
        }
        currentUser.setOpenid(null);
        userService.updateById(currentUser);
        return Result.success("已解除微信绑定");
    }

    @GetMapping("/leave-balance")
    public Result<?> leaveBalance(@AuthenticationPrincipal User user,
                                  @RequestParam(required = false) Integer year) {
        int y = year == null ? LocalDate.now().getYear() : year;
        List<UserLeaveBalance> list = userLeaveBalanceMapper.selectList(new LambdaQueryWrapper<UserLeaveBalance>()
                .eq(UserLeaveBalance::getUserId, user.getId())
                .eq(UserLeaveBalance::getYear, y)
                .orderByAsc(UserLeaveBalance::getLeaveType));

        User currentUser = userService.getById(user.getId());
        String position = currentUser == null ? null : currentUser.getPosition();

        List<LeaveQuotaRule> rules = leaveQuotaRuleMapper.selectList(new LambdaQueryWrapper<LeaveQuotaRule>()
                .eq(LeaveQuotaRule::getYear, y));
        Map<Integer, List<LeaveQuotaRule>> rulesByType = new HashMap<>();
        for (LeaveQuotaRule r : rules) {
            if (r == null || r.getLeaveType() == null) continue;
            rulesByType.computeIfAbsent(r.getLeaveType(), k -> new ArrayList<>()).add(r);
        }

        Map<Integer, UserLeaveBalance> balanceByType = new HashMap<>();
        if (list != null) {
            for (UserLeaveBalance b : list) {
                if (b == null || b.getLeaveType() == null) continue;
                balanceByType.put(b.getLeaveType(), b);
            }
        }

        for (int leaveType = 1; leaveType <= 6; leaveType++) {
            if (balanceByType.containsKey(leaveType)) continue;
            java.math.BigDecimal total = resolveTotalDays(rulesByType.get(leaveType), position);
            if (total == null) total = java.math.BigDecimal.ZERO;
            UserLeaveBalance b = new UserLeaveBalance();
            b.setUserId(user.getId());
            b.setLeaveType(leaveType);
            b.setYear(y);
            b.setTotalDays(total);
            b.setUsedDays(java.math.BigDecimal.ZERO);
            b.setRemainingDays(total);
            userLeaveBalanceMapper.insert(b);
            balanceByType.put(leaveType, b);
        }

        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (int leaveType = 1; leaveType <= 6; leaveType++) {
            UserLeaveBalance b = balanceByType.get(leaveType);
            Map<String, Object> item = new HashMap<>();
            item.put("leaveType", leaveType);
            item.put("leaveTypeName", getLeaveTypeName(leaveType));
            item.put("totalDays", b == null ? java.math.BigDecimal.ZERO : b.getTotalDays());
            item.put("usedDays", b == null ? java.math.BigDecimal.ZERO : b.getUsedDays());
            item.put("remainingDays", b == null ? java.math.BigDecimal.ZERO : b.getRemainingDays());
            result.add(item);
        }
        return Result.success(result);
    }

    private java.math.BigDecimal resolveTotalDays(List<LeaveQuotaRule> candidates, String position) {
        if (candidates == null || candidates.isEmpty()) return null;
        String pos = position == null ? null : position.trim();
        if (pos != null && pos.isEmpty()) pos = null;
        LeaveQuotaRule best = null;
        for (LeaveQuotaRule r : candidates) {
            String rulePos = r.getPosition() == null ? null : r.getPosition().trim();
            if (rulePos != null && rulePos.isEmpty()) rulePos = null;

            boolean match = rulePos != null && pos != null && Objects.equals(rulePos, pos);
            if (match) return r.getTotalDays();
            if (rulePos == null) best = r;
        }
        return best == null ? null : best.getTotalDays();
    }

    private String getLeaveTypeName(Integer type) {
        if (type == null) return "其他";
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
}
