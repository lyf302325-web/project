package com.attendance.controller;

import com.attendance.common.Result;
import com.attendance.entity.*;
import com.attendance.mapper.*;
import com.attendance.service.*;
import com.attendance.util.PasswordUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private LeaveService leaveService;

    @Autowired
    private AttendanceRuleMapper attendanceRuleMapper;

    @Autowired
    private ApprovalFlowMapper approvalFlowMapper;

    @Autowired
    private ApprovalRecordMapper approvalRecordMapper;

    @Autowired
    private UserLeaveBalanceMapper userLeaveBalanceMapper;

    @Autowired
    private LeaveQuotaRuleMapper leaveQuotaRuleMapper;

    @Autowired
    private OvertimeRecordMapper overtimeRecordMapper;

    @Autowired
    private FieldworkMapper fieldworkMapper;

    @Autowired
    private AppealMapper appealMapper;

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Autowired
    private NoticeConfigMapper noticeConfigMapper;

    @Autowired
    private AttendanceGroupMapper attendanceGroupMapper;

    private void writeLog(User operator, String action, String detail, HttpServletRequest request) {
        try {
            OperationLog log = new OperationLog();
            if (operator != null) {
                log.setOperatorId(operator.getId());
                log.setOperatorName(operator.getName());
            }
            log.setAction(action);
            log.setDetail(detail);
            if (request != null) {
                log.setIp(request.getRemoteAddr());
            }
            log.setCreateTime(LocalDateTime.now());
            operationLogMapper.insert(log);
        } catch (Exception ignored) {
        }
    }

    @GetMapping("/today-stats")
    public Result<?> getTodayStats() {
        long totalEmployees = userService.count(new LambdaQueryWrapper<User>().eq(User::getRoleType, 0));
        long checkedIn = attendanceService.count(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getAttendanceDate, LocalDate.now())
                .isNotNull(Attendance::getCheckInTime));
        long late = attendanceService.count(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getAttendanceDate, LocalDate.now())
                .eq(Attendance::getCheckInStatus, 1));

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEmployees", totalEmployees);
        stats.put("checkedIn", checkedIn);
        stats.put("late", late);
        stats.put("absent", totalEmployees - checkedIn);
        return Result.success(stats);
    }

    @GetMapping("/pending-count")
    public Result<?> getPendingCount() {
        long leave = leaveService.count(new LambdaQueryWrapper<LeaveRecord>()
                .eq(LeaveRecord::getStatus, 0)
                .eq(LeaveRecord::getCurrentStage, 2));
        long fieldwork = fieldworkMapper.selectCount(new LambdaQueryWrapper<Fieldwork>().eq(Fieldwork::getStatus, 0));
        long appeal = appealMapper.selectCount(new LambdaQueryWrapper<Appeal>().eq(Appeal::getStatus, 0));
        long overtime = overtimeRecordMapper.selectCount(new LambdaQueryWrapper<OvertimeRecord>()
                .eq(OvertimeRecord::getStatus, 1)
                .eq(OvertimeRecord::getCurrentStage, 2));

        Map<String, Object> count = new HashMap<>();
        count.put("leave", leave);
        count.put("fieldwork", fieldwork);
        count.put("appeal", appeal);
        count.put("overtime", overtime);
        return Result.success(count);
    }

    @GetMapping("/employee/list")
    public Result<?> getEmployeeList(@RequestParam(required = false) String keyword) {
        List<User> list = userService.searchByKeyword(keyword);
        list.forEach(u -> {
            u.setPassword(null);
            u.setSalt(null);
        });
        return Result.success(list);
    }

    @GetMapping("/employee/detail/{id}")
    public Result<?> getEmployeeDetail(@PathVariable Long id) {
        User user = userService.findByIdWithDepartment(id);
        if (user != null) {
            user.setPassword(null);
            user.setSalt(null);
        }
        return Result.success(user);
    }

    @PostMapping("/employee/add")
    public Result<?> addEmployee(@AuthenticationPrincipal User operator,
                                HttpServletRequest httpServletRequest,
                                @RequestBody User user) {
        String rawPassword = user.getPassword();
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            rawPassword = "123456";
        }

        String salt = PasswordUtil.generateSalt();
        user.setSalt(salt);
        user.setPassword(PasswordUtil.encode(rawPassword, salt));
        if (user.getRoleType() == null) {
            user.setRoleType(0);
        }
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        user.setRegisterStatus(1);
        user.setLoginFailCount(0);
        user.setLockUntil(null);

        userService.save(user);
        writeLog(operator, "新增员工", "username=" + user.getUsername(), httpServletRequest);
        return Result.success();
    }

    @PutMapping("/employee/update/{id}")
    public Result<?> updateEmployee(@AuthenticationPrincipal User operator,
                                    HttpServletRequest httpServletRequest,
                                    @PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        userService.updateById(user);
        writeLog(operator, "修改员工", "userId=" + id, httpServletRequest);
        return Result.success();
    }

    @DeleteMapping("/employee/delete/{id}")
    public Result<?> deleteEmployee(@AuthenticationPrincipal User operator,
                                    HttpServletRequest httpServletRequest,
                                    @PathVariable Long id) {
        userService.removeById(id);
        writeLog(operator, "删除员工", "userId=" + id, httpServletRequest);
        return Result.success();
    }

    @PostMapping("/employee/reset-password/{id}")
    public Result<?> resetPassword(@AuthenticationPrincipal User operator,
                                   HttpServletRequest httpServletRequest,
                                   @PathVariable Long id) {
        User user = userService.getById(id);
        if (user != null) {
            String salt = PasswordUtil.generateSalt();
            user.setSalt(salt);
            user.setPassword(PasswordUtil.encode("123456", salt));
            user.setLoginFailCount(0);
            user.setLockUntil(null);
            userService.updateById(user);
            writeLog(operator, "重置密码", "userId=" + id, httpServletRequest);
        }
        return Result.success();
    }

    @GetMapping("/employee/pending")
    public Result<?> getPendingEmployees() {
        List<User> list = userService.list(new LambdaQueryWrapper<User>()
                .eq(User::getRoleType, 0)
                .eq(User::getRegisterStatus, 0));
        list.forEach(u -> {
            u.setPassword(null);
            u.setSalt(null);
        });
        return Result.success(list);
    }

    @PostMapping("/employee/approve/{id}")
    public Result<?> approveEmployee(@AuthenticationPrincipal User operator,
                                     HttpServletRequest httpServletRequest,
                                     @PathVariable Long id,
                                     @RequestBody Map<String, Object> request) {
        Integer registerStatus = request.get("registerStatus") == null ? null : Integer.valueOf(request.get("registerStatus").toString());
        if (registerStatus == null || (registerStatus != 1 && registerStatus != 2)) {
            return Result.error("registerStatus必须为1(通过)或2(驳回)");
        }
        User user = userService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        user.setRegisterStatus(registerStatus);
        userService.updateById(user);
        writeLog(operator, "审核注册", "userId=" + id + ", registerStatus=" + registerStatus, httpServletRequest);
        return Result.success();
    }

    @GetMapping("/department/list")
    public Result<?> getDepartmentList() {
        return Result.success(departmentService.findAllWithManager());
    }

    @PostMapping("/department/add")
    public Result<?> addDepartment(@AuthenticationPrincipal User operator,
                                   HttpServletRequest httpServletRequest,
                                   @RequestBody Department department) {
        departmentService.save(department);
        writeLog(operator, "新增部门", "department=" + department.getName(), httpServletRequest);
        return Result.success();
    }

    @PutMapping("/department/update/{id}")
    public Result<?> updateDepartment(@AuthenticationPrincipal User operator,
                                      HttpServletRequest httpServletRequest,
                                      @PathVariable Long id, @RequestBody Department department) {
        department.setId(id);
        departmentService.updateById(department);
        writeLog(operator, "修改部门", "departmentId=" + id, httpServletRequest);
        return Result.success();
    }

    @DeleteMapping("/department/delete/{id}")
    public Result<?> deleteDepartment(@AuthenticationPrincipal User operator,
                                      HttpServletRequest httpServletRequest,
                                      @PathVariable Long id) {
        departmentService.removeById(id);
        writeLog(operator, "删除部门", "departmentId=" + id, httpServletRequest);
        return Result.success();
    }

    @GetMapping("/rule/get")
    public Result<?> getRule(@RequestParam(required = false) Long attendanceGroupId) {
        AttendanceRule rule = null;
        if (attendanceGroupId != null) {
            rule = attendanceRuleMapper.selectOne(new LambdaQueryWrapper<AttendanceRule>()
                    .eq(AttendanceRule::getDepartmentId, attendanceGroupId)
                    .eq(AttendanceRule::getEnabled, 1)
                    .orderByDesc(AttendanceRule::getId)
                    .last("limit 1"));
        }
        if (rule == null) {
            rule = attendanceRuleMapper.selectOne(new LambdaQueryWrapper<AttendanceRule>()
                    .eq(AttendanceRule::getEnabled, 1)
                    .orderByDesc(AttendanceRule::getId)
                    .last("limit 1"));
        }
        return Result.success(rule);
    }

    @GetMapping("/rule/list")
    public Result<?> listRules(@RequestParam(required = false) Long attendanceGroupId) {
        LambdaQueryWrapper<AttendanceRule> w = new LambdaQueryWrapper<>();
        if (attendanceGroupId != null) {
            w.eq(AttendanceRule::getDepartmentId, attendanceGroupId);
        }
        w.orderByDesc(AttendanceRule::getEnabled).orderByDesc(AttendanceRule::getId);
        return Result.success(attendanceRuleMapper.selectList(w));
    }

    @PostMapping("/rule/save")
    public Result<?> saveRule(@AuthenticationPrincipal User operator,
                              HttpServletRequest httpServletRequest,
                              @RequestBody AttendanceRule rule) {
        if (rule.getEnabled() == null) rule.setEnabled(1);
        if (rule.getEarlyCheckInMinutes() == null) rule.setEarlyCheckInMinutes(120);
        if (rule.getLateCheckOutMinutes() == null) rule.setLateCheckOutMinutes(240);
        if (rule.getShiftName() == null || rule.getShiftName().trim().isEmpty()) {
            rule.setShiftName("默认班次");
        }

        if (rule.getId() != null) {
            attendanceRuleMapper.updateById(rule);
        } else {
            attendanceRuleMapper.insert(rule);
        }

        if (rule.getEnabled() != null && rule.getEnabled() == 1) {
            LambdaQueryWrapper<AttendanceRule> other = new LambdaQueryWrapper<>();
            if (rule.getDepartmentId() == null) {
                other.isNull(AttendanceRule::getDepartmentId);
            } else {
                other.eq(AttendanceRule::getDepartmentId, rule.getDepartmentId());
            }
            other.ne(AttendanceRule::getId, rule.getId());
            AttendanceRule patch = new AttendanceRule();
            patch.setEnabled(0);
            attendanceRuleMapper.update(patch, other);
        }

        writeLog(operator, "配置考勤规则", "ruleId=" + rule.getId(), httpServletRequest);
        return Result.success();
    }

    @PostMapping("/rule/enable/{id}")
    public Result<?> enableRule(@AuthenticationPrincipal User operator,
                                HttpServletRequest httpServletRequest,
                                @PathVariable Long id) {
        AttendanceRule target = attendanceRuleMapper.selectById(id);
        if (target == null) return Result.error("规则不存在");

        LambdaQueryWrapper<AttendanceRule> other = new LambdaQueryWrapper<>();
        if (target.getDepartmentId() == null) {
            other.isNull(AttendanceRule::getDepartmentId);
        } else {
            other.eq(AttendanceRule::getDepartmentId, target.getDepartmentId());
        }
        AttendanceRule disable = new AttendanceRule();
        disable.setEnabled(0);
        attendanceRuleMapper.update(disable, other);

        AttendanceRule enable = new AttendanceRule();
        enable.setId(id);
        enable.setEnabled(1);
        attendanceRuleMapper.updateById(enable);

        writeLog(operator, "启用班次规则", "ruleId=" + id, httpServletRequest);
        return Result.success();
    }

    @GetMapping("/attendance-group/list")
    public Result<?> attendanceGroupList() {
        return Result.success(attendanceGroupMapper.selectList(new LambdaQueryWrapper<AttendanceGroup>().orderByDesc(AttendanceGroup::getId)));
    }

    @PostMapping("/attendance-group/add")
    public Result<?> addAttendanceGroup(@AuthenticationPrincipal User operator,
                                        HttpServletRequest httpServletRequest,
                                        @RequestBody AttendanceGroup group) {
        attendanceGroupMapper.insert(group);
        writeLog(operator, "新增考勤组", "groupId=" + group.getId(), httpServletRequest);
        return Result.success();
    }

    @PutMapping("/attendance-group/update/{id}")
    public Result<?> updateAttendanceGroup(@AuthenticationPrincipal User operator,
                                           HttpServletRequest httpServletRequest,
                                           @PathVariable Long id,
                                           @RequestBody AttendanceGroup group) {
        group.setId(id);
        attendanceGroupMapper.updateById(group);
        writeLog(operator, "修改考勤组", "groupId=" + id, httpServletRequest);
        return Result.success();
    }

    @DeleteMapping("/attendance-group/delete/{id}")
    public Result<?> deleteAttendanceGroup(@AuthenticationPrincipal User operator,
                                           HttpServletRequest httpServletRequest,
                                           @PathVariable Long id) {
        attendanceGroupMapper.deleteById(id);
        writeLog(operator, "删除考勤组", "groupId=" + id, httpServletRequest);
        return Result.success();
    }

    @PostMapping("/attendance-group/assign")
    public Result<?> assignAttendanceGroup(@AuthenticationPrincipal User operator,
                                           HttpServletRequest httpServletRequest,
                                           @RequestBody Map<String, Object> request) {
        Long groupId = request.get("groupId") == null ? null : Long.valueOf(request.get("groupId").toString());
        List<Long> departmentIds = ((List<?>) request.getOrDefault("departmentIds", java.util.Collections.emptyList()))
                .stream().map(x -> Long.valueOf(String.valueOf(x))).collect(java.util.stream.Collectors.toList());
        if (groupId == null) return Result.error("groupId不能为空");
        if (departmentIds.isEmpty()) return Result.error("departmentIds不能为空");

        Department patch = new Department();
        patch.setAttendanceGroupId(groupId);
        departmentService.update(patch, new LambdaQueryWrapper<Department>().in(Department::getId, departmentIds));

        User uPatch = new User();
        uPatch.setAttendanceGroupId(groupId);
        userService.update(uPatch, new LambdaQueryWrapper<User>().in(User::getDepartmentId, departmentIds));

        writeLog(operator, "分配考勤组", "groupId=" + groupId + ", departmentIds=" + departmentIds, httpServletRequest);
        return Result.success();
    }

    @GetMapping("/approval-flow/list")
    public Result<?> getApprovalFlowList() {
        return Result.success(approvalFlowMapper.selectAllWithApprover());
    }

    @PostMapping("/approval-flow/save")
    public Result<?> saveApprovalFlow(@RequestBody Map<String, Object> request) {
        List<Map<String, Object>> rules = (List<Map<String, Object>>) request.get("rules");
        approvalFlowMapper.delete(null);
        for (Map<String, Object> r : rules) {
            ApprovalFlow flow = new ApprovalFlow();
            flow.setRecordType(r.get("recordType") == null ? "leave" : r.get("recordType").toString());
            if (r.get("departmentId") != null && !r.get("departmentId").toString().trim().isEmpty()) {
                flow.setDepartmentId(Long.valueOf(r.get("departmentId").toString()));
            }
            if (r.get("leaveType") != null && !r.get("leaveType").toString().trim().isEmpty()) {
                flow.setLeaveType(Integer.valueOf(r.get("leaveType").toString()));
            }
            flow.setMinDays(new java.math.BigDecimal(r.get("minDays").toString()));
            flow.setMaxDays(new java.math.BigDecimal(r.get("maxDays").toString()));
            Integer level = null;
            if (r.get("level") != null && !r.get("level").toString().trim().isEmpty()) {
                level = Integer.valueOf(r.get("level").toString());
            }
            flow.setLevel(level == null ? 1 : level);
            if (r.get("approverId") != null && !r.get("approverId").toString().trim().isEmpty()) {
                flow.setApproverId(Long.valueOf(r.get("approverId").toString()));
            }
            boolean needAdmin = Boolean.TRUE.equals(r.get("needAdmin")) || (flow.getLevel() != null && flow.getLevel() >= 2);
            flow.setNeedAdmin(needAdmin ? 1 : 0);
            approvalFlowMapper.insert(flow);
        }
        return Result.success();
    }

    @GetMapping("/leave/list")
    public Result<?> getLeaveList(@RequestParam Integer status) {
        List<LeaveRecord> list = leaveService.findByStatus(status);
        if (status != null && status == 0) {
            list.removeIf(r -> r.getCurrentStage() == null || r.getCurrentStage() != 2);
        }
        return Result.success(list);
    }

    @PostMapping("/leave/approve/{id}")
    public Result<?> approveLeave(@AuthenticationPrincipal User operator,
                                  @PathVariable Long id, @RequestBody Map<String, Object> request) {
        Integer status = Integer.valueOf(request.get("status").toString());
        if (status != 1 && status != 2) return Result.error("status必须为1(通过)或2(拒绝)");
        String opinion = request.get("opinion") == null ? null : request.get("opinion").toString();

        LeaveRecord record = leaveService.getById(id);
        if (record == null) return Result.error("记录不存在");
        if (record.getStatus() == null || record.getStatus() != 0) return Result.error("记录已处理");
        if (record.getCurrentStage() == null || record.getCurrentStage() != 2) return Result.error("当前不在管理员终审阶段");

        ApprovalRecord ar = new ApprovalRecord();
        ar.setRecordType("leave");
        ar.setRecordId(id);
        ar.setApproverId(operator.getId());
        ar.setStatus(status);
        ar.setOpinion(opinion);
        ar.setApprovalTime(LocalDateTime.now());
        ar.setLevel(2);
        approvalRecordMapper.insert(ar);

        if (status == 2) {
            record.setStatus(2);
            record.setCurrentApproverId(null);
            leaveService.updateById(record);
            return Result.success();
        }

        record.setStatus(1);
        record.setCurrentApproverId(null);
        leaveService.updateById(record);

        int year = LocalDate.now().getYear();
        UserLeaveBalance balance = userLeaveBalanceMapper.selectOne(new LambdaQueryWrapper<UserLeaveBalance>()
                .eq(UserLeaveBalance::getUserId, record.getUserId())
                .eq(UserLeaveBalance::getLeaveType, record.getLeaveType())
                .eq(UserLeaveBalance::getYear, year));
        if (balance != null) {
            java.math.BigDecimal used = balance.getUsedDays() == null ? java.math.BigDecimal.ZERO : balance.getUsedDays();
            java.math.BigDecimal remaining = balance.getRemainingDays() == null ? java.math.BigDecimal.ZERO : balance.getRemainingDays();
            java.math.BigDecimal d = record.getDays() == null ? java.math.BigDecimal.ZERO : record.getDays();
            balance.setUsedDays(used.add(d));
            balance.setRemainingDays(remaining.subtract(d));
            userLeaveBalanceMapper.updateById(balance);
        }
        return Result.success();
    }

    @GetMapping("/fieldwork/list")
    public Result<?> getFieldworkList(@RequestParam Integer status) {
        return Result.success(fieldworkMapper.selectByStatus(status));
    }

    @PostMapping("/fieldwork/approve/{id}")
    public Result<?> approveFieldwork(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Integer status = Integer.valueOf(request.get("status").toString());
        Fieldwork fieldwork = fieldworkMapper.selectById(id);
        if (fieldwork != null) {
            fieldwork.setStatus(status);
            fieldworkMapper.updateById(fieldwork);
        }
        return Result.success();
    }

    @GetMapping("/appeal/list")
    public Result<?> getAppealList(@RequestParam Integer status) {
        return Result.success(appealMapper.selectByStatus(status));
    }

    @PostMapping("/appeal/approve/{id}")
    public Result<?> approveAppeal(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Integer status = Integer.valueOf(request.get("status").toString());
        Appeal appeal = appealMapper.selectById(id);
        if (appeal != null) {
            appeal.setStatus(status);
            appealMapper.updateById(appeal);
        }
        return Result.success();
    }

    @GetMapping("/statistics")
    public Result<?> getStatistics(@RequestParam(required = false) Long departmentId,
                                   @RequestParam String month) {
        String[] parts = month.split("-");
        int year = Integer.parseInt(parts[0]);
        int monthNum = Integer.parseInt(parts[1]);
        LocalDate startDate = LocalDate.of(year, monthNum, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(User::getRoleType, 0);
        if (departmentId != null) {
            userWrapper.eq(User::getDepartmentId, departmentId);
        }
        List<User> employees = userService.list(userWrapper);

        int totalDays = endDate.getDayOfMonth();
        int normalCount = 0;
        int lateCount = 0;
        int earlyCount = 0;
        int absentCount = 0;
        int leaveCount = 0;

        List<Map<String, Object>> employeeStats = new java.util.ArrayList<>();

        for (User emp : employees) {
            LambdaQueryWrapper<Attendance> attWrapper = new LambdaQueryWrapper<>();
            attWrapper.eq(Attendance::getUserId, emp.getId())
                    .between(Attendance::getAttendanceDate, startDate, endDate);
            List<Attendance> attendances = attendanceService.list(attWrapper);

            int empNormal = 0, empLate = 0, empEarly = 0, empAbsent = 0;
            for (Attendance att : attendances) {
                if (att.getCheckInStatus() != null && att.getCheckInStatus() == 1) {
                    empLate++;
                    lateCount++;
                } else if (att.getCheckInTime() != null) {
                    empNormal++;
                    normalCount++;
                }
                if (att.getCheckOutStatus() != null && att.getCheckOutStatus() == 2) {
                    empEarly++;
                    earlyCount++;
                }
            }
            empAbsent = totalDays - attendances.size();
            absentCount += empAbsent;

            Map<String, Object> empStat = new HashMap<>();
            empStat.put("id", emp.getId());
            empStat.put("name", emp.getName());
            empStat.put("employeeNo", emp.getEmployeeNo());
            empStat.put("normal", empNormal);
            empStat.put("late", empLate);
            empStat.put("early", empEarly);
            empStat.put("absent", empAbsent);
            empStat.put("leave", 0);
            employeeStats.add(empStat);
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("normal", normalCount);
        summary.put("late", lateCount);
        summary.put("early", earlyCount);
        summary.put("absent", absentCount);
        summary.put("leave", leaveCount);

        Map<String, Object> result = new HashMap<>();
        result.put("summary", summary);
        result.put("employees", employeeStats);
        return Result.success(result);
    }

    @GetMapping("/statistics/export")
    public void exportStatistics(@RequestParam(required = false) Long departmentId,
                                 @RequestParam String month,
                                 HttpServletResponse response) throws IOException {
        Result<?> statsResult = getStatistics(departmentId, month);
        Map<String, Object> data = (Map<String, Object>) statsResult.getData();
        List<Map<String, Object>> employees = (List<Map<String, Object>>) data.get("employees");

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("考勤统计");

        // 表头样式
        XSSFCellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        String[] headers = {"姓名", "工号", "正常", "迟到", "早退", "缺勤", "请假"};
        XSSFRow headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
            headerRow.getCell(i).setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 4000);
        }

        // 数据行
        if (employees != null) {
            for (int i = 0; i < employees.size(); i++) {
                Map<String, Object> e = employees.get(i);
                XSSFRow row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(String.valueOf(e.get("name")));
                row.createCell(1).setCellValue(String.valueOf(e.get("employeeNo")));
                row.createCell(2).setCellValue(toInt(e.get("normal")));
                row.createCell(3).setCellValue(toInt(e.get("late")));
                row.createCell(4).setCellValue(toInt(e.get("early")));
                row.createCell(5).setCellValue(toInt(e.get("absent")));
                row.createCell(6).setCellValue(toInt(e.get("leave")));
            }
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=attendance_" + month + ".xlsx");
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    private int toInt(Object obj) {
        if (obj == null) return 0;
        try { return Integer.parseInt(obj.toString()); } catch (Exception e) { return 0; }
    }

    @GetMapping("/log/list")
    public Result<?> getLogList(@RequestParam String startDate, @RequestParam String endDate) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(OperationLog::getCreateTime,
                        LocalDate.parse(startDate).atStartOfDay(),
                        LocalDate.parse(endDate).plusDays(1).atStartOfDay())
                .orderByDesc(OperationLog::getCreateTime);
        return Result.success(operationLogMapper.selectList(wrapper));
    }

    @GetMapping("/notice/config")
    public Result<?> getNoticeConfig() {
        NoticeConfig config = noticeConfigMapper.selectList(null).stream().findFirst().orElse(null);
        if (config == null) {
            config = new NoticeConfig();
            config.setAttendanceAlert(1);
            config.setApprovalResult(1);
            config.setPendingApproval(1);
            config.setCheckInReminder(0);
            config.setMorningReminderTime("08:50");
            config.setEveningReminderTime("18:00");
            config.setMissCheckInAfterMinutes(30);
            config.setMissCheckOutAfterMinutes(30);
            config.setLateRemind(1);
            config.setEarlyRemind(1);
            config.setMissCheckInRemind(1);
            config.setMissCheckOutRemind(1);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("attendanceAlert", config.getAttendanceAlert() == 1);
        result.put("approvalResult", config.getApprovalResult() == 1);
        result.put("pendingApproval", config.getPendingApproval() == 1);
        result.put("checkInReminder", config.getCheckInReminder() == 1);
        result.put("morningReminderTime", config.getMorningReminderTime());
        result.put("eveningReminderTime", config.getEveningReminderTime());
        result.put("missCheckInAfterMinutes", config.getMissCheckInAfterMinutes());
        result.put("missCheckOutAfterMinutes", config.getMissCheckOutAfterMinutes());
        result.put("lateRemind", config.getLateRemind() == 1);
        result.put("earlyRemind", config.getEarlyRemind() == 1);
        result.put("missCheckInRemind", config.getMissCheckInRemind() == 1);
        result.put("missCheckOutRemind", config.getMissCheckOutRemind() == 1);
        return Result.success(result);
    }

    @PostMapping("/notice/config")
    public Result<?> saveNoticeConfig(@RequestBody Map<String, Object> request) {
        NoticeConfig config = noticeConfigMapper.selectList(null).stream().findFirst().orElse(null);
        if (config == null) {
            config = new NoticeConfig();
        }
        config.setAttendanceAlert(Boolean.TRUE.equals(request.get("attendanceAlert")) ? 1 : 0);
        config.setApprovalResult(Boolean.TRUE.equals(request.get("approvalResult")) ? 1 : 0);
        config.setPendingApproval(Boolean.TRUE.equals(request.get("pendingApproval")) ? 1 : 0);
        config.setCheckInReminder(Boolean.TRUE.equals(request.get("checkInReminder")) ? 1 : 0);
        if (request.get("morningReminderTime") != null) {
            config.setMorningReminderTime(request.get("morningReminderTime").toString());
        }
        if (request.get("eveningReminderTime") != null) {
            config.setEveningReminderTime(request.get("eveningReminderTime").toString());
        }
        if (request.get("missCheckInAfterMinutes") != null) {
            config.setMissCheckInAfterMinutes(Integer.valueOf(request.get("missCheckInAfterMinutes").toString()));
        }
        if (request.get("missCheckOutAfterMinutes") != null) {
            config.setMissCheckOutAfterMinutes(Integer.valueOf(request.get("missCheckOutAfterMinutes").toString()));
        }
        config.setLateRemind(Boolean.TRUE.equals(request.get("lateRemind")) ? 1 : 0);
        config.setEarlyRemind(Boolean.TRUE.equals(request.get("earlyRemind")) ? 1 : 0);
        config.setMissCheckInRemind(Boolean.TRUE.equals(request.get("missCheckInRemind")) ? 1 : 0);
        config.setMissCheckOutRemind(Boolean.TRUE.equals(request.get("missCheckOutRemind")) ? 1 : 0);
        if (config.getId() == null) {
            noticeConfigMapper.insert(config);
        } else {
            noticeConfigMapper.updateById(config);
        }
        return Result.success();
    }

    @PostMapping("/attendance/correct")
    public Result<?> correctAttendance(@AuthenticationPrincipal User operator,
                                       HttpServletRequest httpServletRequest,
                                       @RequestBody Map<String, Object> request) {
        Long id = Long.valueOf(request.get("id").toString());
        Attendance attendance = attendanceService.getById(id);
        if (attendance == null) {
            return Result.error("考勤记录不存在");
        }

        if (request.containsKey("checkInTime")) {
            attendance.setCheckInTime(request.get("checkInTime") != null ? request.get("checkInTime").toString() : null);
        }
        if (request.containsKey("checkOutTime")) {
            attendance.setCheckOutTime(request.get("checkOutTime") != null ? request.get("checkOutTime").toString() : null);
        }
        if (request.containsKey("checkInStatus")) {
            attendance.setCheckInStatus(Integer.valueOf(request.get("checkInStatus").toString()));
        }
        if (request.containsKey("checkOutStatus")) {
            attendance.setCheckOutStatus(request.get("checkOutStatus") != null ? Integer.valueOf(request.get("checkOutStatus").toString()) : null);
        }
        if (request.containsKey("status")) {
            attendance.setStatus(Integer.valueOf(request.get("status").toString()));
        }
        if (request.containsKey("isAbnormal")) {
            attendance.setIsAbnormal(Integer.valueOf(request.get("isAbnormal").toString()));
        }

        attendanceService.updateById(attendance);
        writeLog(operator, "修正考勤记录", "attendanceId=" + id, httpServletRequest);
        return Result.success("修正成功");
    }

    @GetMapping("/attendance/detail")
    public Result<?> getAttendanceDetail(@RequestParam Long userId, @RequestParam String month) {
        String[] parts = month.split("-");
        int year = Integer.parseInt(parts[0]);
        int monthNum = Integer.parseInt(parts[1]);
        LocalDate startDate = LocalDate.of(year, monthNum, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        User emp = userService.getById(userId);
        if (emp == null) {
            return Result.error("员工不存在");
        }

        LambdaQueryWrapper<Attendance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Attendance::getUserId, userId)
                .between(Attendance::getAttendanceDate, startDate, endDate)
                .orderByDesc(Attendance::getAttendanceDate);
        List<Attendance> list = attendanceService.list(wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("name", emp.getName());
        result.put("employeeNo", emp.getEmployeeNo());
        result.put("records", list);
        return Result.success(result);
    }
}
