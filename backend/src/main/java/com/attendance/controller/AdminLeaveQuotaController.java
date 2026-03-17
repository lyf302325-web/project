package com.attendance.controller;

import com.attendance.common.Result;
import com.attendance.entity.LeaveQuotaRule;
import com.attendance.entity.User;
import com.attendance.entity.UserLeaveBalance;
import com.attendance.mapper.LeaveQuotaRuleMapper;
import com.attendance.mapper.UserLeaveBalanceMapper;
import com.attendance.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/admin/leave-quota")
public class AdminLeaveQuotaController {

    @Autowired
    private LeaveQuotaRuleMapper leaveQuotaRuleMapper;

    @Autowired
    private UserLeaveBalanceMapper userLeaveBalanceMapper;

    @Autowired
    private UserService userService;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(required = false) Integer year) {
        int y = year == null ? LocalDate.now().getYear() : year;
        List<LeaveQuotaRule> list = leaveQuotaRuleMapper.selectList(new LambdaQueryWrapper<LeaveQuotaRule>()
                .eq(LeaveQuotaRule::getYear, y)
                .orderByAsc(LeaveQuotaRule::getLeaveType));
        return Result.success(list);
    }

    @PostMapping("/save")
    public Result<?> save(@AuthenticationPrincipal User operator, @RequestBody Map<String, Object> request) {
        int year = Integer.parseInt(request.getOrDefault("year", LocalDate.now().getYear()).toString());
        List<Map<String, Object>> rules = (List<Map<String, Object>>) request.getOrDefault("rules", Collections.emptyList());

        leaveQuotaRuleMapper.delete(new LambdaQueryWrapper<LeaveQuotaRule>().eq(LeaveQuotaRule::getYear, year));
        for (Map<String, Object> r : rules) {
            LeaveQuotaRule rule = new LeaveQuotaRule();
            rule.setYear(year);
            rule.setLeaveType(Integer.valueOf(r.get("leaveType").toString()));
            rule.setTotalDays(new BigDecimal(r.get("totalDays").toString()));
            String position = r.get("position") == null ? null : r.get("position").toString();
            if (position != null && position.trim().isEmpty()) position = null;
            rule.setPosition(position);
            leaveQuotaRuleMapper.insert(rule);
        }
        return Result.success(syncBalances(year));
    }

    @PostMapping("/init")
    public Result<?> initBalances(@RequestParam(required = false) Integer year,
                                  @RequestBody(required = false) Map<String, Object> body) {
        Integer bodyYear = null;
        if (body != null && body.get("year") != null) {
            try {
                bodyYear = Integer.valueOf(body.get("year").toString());
            } catch (Exception ignored) {}
        }
        Integer chosenYear = year != null ? year : bodyYear;
        int y = chosenYear == null ? LocalDate.now().getYear() : chosenYear;
        return Result.success(syncBalances(y));
    }

    private Map<String, Object> syncBalances(int y) {
        List<LeaveQuotaRule> rules = leaveQuotaRuleMapper.selectList(new LambdaQueryWrapper<LeaveQuotaRule>().eq(LeaveQuotaRule::getYear, y));
        Map<Integer, List<LeaveQuotaRule>> rulesByType = new HashMap<>();
        for (LeaveQuotaRule r : rules) {
            if (r == null || r.getLeaveType() == null) continue;
            rulesByType.computeIfAbsent(r.getLeaveType(), k -> new ArrayList<>()).add(r);
        }

        List<User> employees = userService.list(new LambdaQueryWrapper<User>().eq(User::getRoleType, 0));
        int updated = 0;

        for (User emp : employees) {
            for (int leaveType = 1; leaveType <= 6; leaveType++) {
                BigDecimal total = resolveTotalDays(rulesByType.get(leaveType), emp.getPosition());
                if (total == null) total = BigDecimal.ZERO;

                UserLeaveBalance existing = userLeaveBalanceMapper.selectOne(new LambdaQueryWrapper<UserLeaveBalance>()
                        .eq(UserLeaveBalance::getUserId, emp.getId())
                        .eq(UserLeaveBalance::getLeaveType, leaveType)
                        .eq(UserLeaveBalance::getYear, y));

                if (existing == null) {
                    UserLeaveBalance b = new UserLeaveBalance();
                    b.setUserId(emp.getId());
                    b.setLeaveType(leaveType);
                    b.setYear(y);
                    b.setTotalDays(total);
                    b.setUsedDays(BigDecimal.ZERO);
                    b.setRemainingDays(total);
                    userLeaveBalanceMapper.insert(b);
                    updated++;
                } else {
                    BigDecimal used = existing.getUsedDays() == null ? BigDecimal.ZERO : existing.getUsedDays();
                    existing.setTotalDays(total);
                    BigDecimal remaining = total.subtract(used);
                    if (remaining.compareTo(BigDecimal.ZERO) < 0) remaining = BigDecimal.ZERO;
                    existing.setRemainingDays(remaining);
                    userLeaveBalanceMapper.updateById(existing);
                    updated++;
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("year", y);
        result.put("updated", updated);
        result.put("employeeCount", employees.size());
        result.put("ruleCount", rules.size());
        return result;
    }

    private BigDecimal resolveTotalDays(List<LeaveQuotaRule> candidates, String position) {
        if (candidates == null || candidates.isEmpty()) return null;
        String pos = position == null ? null : position.trim();
        if (pos != null && pos.isEmpty()) pos = null;
        LeaveQuotaRule best = null;
        for (LeaveQuotaRule r : candidates) {
            String rulePos = r.getPosition() == null ? null : r.getPosition().trim();
            if (rulePos != null && rulePos.isEmpty()) rulePos = null;

            boolean match = rulePos != null && pos != null && rulePos.equals(pos);
            if (match) return r.getTotalDays();
            if (rulePos == null) best = r;
        }
        return best == null ? null : best.getTotalDays();
    }
}
