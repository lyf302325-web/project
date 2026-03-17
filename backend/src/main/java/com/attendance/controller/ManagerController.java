package com.attendance.controller;

import com.attendance.common.Result;
import com.attendance.entity.*;
import com.attendance.mapper.*;
import com.attendance.service.AttendanceService;
import com.attendance.service.DepartmentService;
import com.attendance.service.LeaveService;
import com.attendance.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/manager")
public class ManagerController {

    @Autowired
    private UserService userService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private LeaveService leaveService;

    @Autowired
    private FieldworkMapper fieldworkMapper;

    @Autowired
    private AppealMapper appealMapper;

    @Autowired
    private ApprovalRecordMapper approvalRecordMapper;

    @Autowired
    private ApprovalFlowMapper approvalFlowMapper;

    @Autowired
    private OvertimeRecordMapper overtimeRecordMapper;

    private List<User> getDepartmentEmployees(Long departmentId) {
        if (departmentId == null) {
            return Collections.emptyList();
        }
        return userService.list(new LambdaQueryWrapper<User>()
                .eq(User::getRoleType, 0)
                .eq(User::getDepartmentId, departmentId));
    }

    private Map<Long, User> toUserMap(List<User> users) {
        Map<Long, User> map = new HashMap<>();
        for (User u : users) {
            map.put(u.getId(), u);
        }
        return map;
    }

    private String getLeaveTypeName(Integer type) {
        if (type == null) return "";
        switch (type) {
            case 1:
                return "事假";
            case 2:
                return "病假";
            case 3:
                return "年假";
            case 4:
                return "婚假";
            case 5:
                return "产假";
            case 6:
                return "丧假";
            default:
                return "其他";
        }
    }

    private String getAppealTypeName(Integer type) {
        if (type == null) return "";
        switch (type) {
            case 1:
                return "漏签到";
            case 2:
                return "漏签退";
            case 3:
                return "定位失败";
            case 4:
                return "设备故障";
            case 5:
                return "其他";
            default:
                return "其他";
        }
    }

    private Result<?> requireDepartment(User manager) {
        if (manager == null) return Result.error("未授权");
        if (manager.getDepartmentId() == null) return Result.error("未分配部门");
        if (manager.getRoleType() == null || (manager.getRoleType() != 1 && manager.getRoleType() != 2)) {
            return Result.error("无权限");
        }
        return null;
    }

    @GetMapping("/home")
    public Result<?> home(@AuthenticationPrincipal User manager) {
        Result<?> deny = requireDepartment(manager);
        if (deny != null) return deny;

        Long deptId = manager.getDepartmentId();
        Department dept = departmentService.getById(deptId);
        List<User> employees = getDepartmentEmployees(deptId);
        List<Long> employeeIds = employees.stream().map(User::getId).collect(Collectors.toList());

        long pendingLeave = 0;
        long pendingFieldwork = 0;
        long pendingAppeal = 0;
        long pendingOvertime = 0;
        long todayAbnormal = 0;

        if (!employeeIds.isEmpty()) {
            pendingLeave = leaveService.count(new LambdaQueryWrapper<LeaveRecord>()
                    .eq(LeaveRecord::getStatus, 0)
                    .in(LeaveRecord::getUserId, employeeIds));
            pendingFieldwork = fieldworkMapper.selectCount(new LambdaQueryWrapper<Fieldwork>()
                    .eq(Fieldwork::getStatus, 0)
                    .in(Fieldwork::getUserId, employeeIds));
            pendingAppeal = appealMapper.selectCount(new LambdaQueryWrapper<Appeal>()
                    .eq(Appeal::getStatus, 0)
                    .in(Appeal::getUserId, employeeIds));
            pendingOvertime = overtimeRecordMapper.selectCount(new LambdaQueryWrapper<OvertimeRecord>()
                    .eq(OvertimeRecord::getStatus, 0)
                    .eq(OvertimeRecord::getCurrentStage, 1)
                    .in(OvertimeRecord::getUserId, employeeIds));
            todayAbnormal = attendanceService.count(new LambdaQueryWrapper<Attendance>()
                    .eq(Attendance::getAttendanceDate, LocalDate.now())
                    .eq(Attendance::getIsAbnormal, 1)
                    .in(Attendance::getUserId, employeeIds));
        }

        Map<String, Object> pending = new HashMap<>();
        pending.put("leave", pendingLeave);
        pending.put("fieldwork", pendingFieldwork);
        pending.put("appeal", pendingAppeal);
        pending.put("overtime", pendingOvertime);

        Map<String, Object> result = new HashMap<>();
        result.put("departmentId", deptId);
        result.put("departmentName", dept != null ? dept.getName() : null);
        result.put("employeeCount", employees.size());
        result.put("pending", pending);
        result.put("todayAbnormal", todayAbnormal);
        return Result.success(result);
    }

    @GetMapping("/employee/list")
    public Result<?> employeeList(@AuthenticationPrincipal User manager) {
        Result<?> deny = requireDepartment(manager);
        if (deny != null) return deny;

        List<User> list = getDepartmentEmployees(manager.getDepartmentId());
        list.forEach(u -> {
            u.setPassword(null);
            u.setSalt(null);
        });
        return Result.success(list);
    }

    @GetMapping("/employee/detail/{id}")
    public Result<?> employeeDetail(@AuthenticationPrincipal User manager, @PathVariable Long id) {
        Result<?> deny = requireDepartment(manager);
        if (deny != null) return deny;

        User user = userService.findByIdWithDepartment(id);
        if (user == null) return Result.error("员工不存在");
        if (!Objects.equals(user.getDepartmentId(), manager.getDepartmentId())) return Result.error("无权限");
        if (user.getRoleType() == null || user.getRoleType() != 0) return Result.error("无权限");
        user.setPassword(null);
        user.setSalt(null);
        return Result.success(user);
    }

    @GetMapping("/attendance/detail")
    public Result<?> attendanceDetail(@AuthenticationPrincipal User manager,
                                     @RequestParam Long userId,
                                     @RequestParam String month) {
        Result<?> deny = requireDepartment(manager);
        if (deny != null) return deny;

        User emp = userService.getById(userId);
        if (emp == null) return Result.error("员工不存在");
        if (!Objects.equals(emp.getDepartmentId(), manager.getDepartmentId())) return Result.error("无权限");
        if (emp.getRoleType() == null || emp.getRoleType() != 0) return Result.error("无权限");

        String[] parts = month.split("-");
        int year = Integer.parseInt(parts[0]);
        int monthNum = Integer.parseInt(parts[1]);
        LocalDate startDate = LocalDate.of(year, monthNum, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

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

    @GetMapping("/leave/list")
    public Result<?> leaveList(@AuthenticationPrincipal User manager, @RequestParam Integer status) {
        Result<?> deny = requireDepartment(manager);
        if (deny != null) return deny;

        List<User> employees = getDepartmentEmployees(manager.getDepartmentId());
        if (employees.isEmpty()) return Result.success(Collections.emptyList());

        List<Long> employeeIds = employees.stream().map(User::getId).collect(Collectors.toList());
        Map<Long, User> userMap = toUserMap(employees);

        LambdaQueryWrapper<LeaveRecord> wrapper = new LambdaQueryWrapper<LeaveRecord>()
                .eq(LeaveRecord::getStatus, status)
                .in(LeaveRecord::getUserId, employeeIds)
                .orderByDesc(LeaveRecord::getCreateTime);
        if (status != null && status == 0) {
            wrapper.eq(LeaveRecord::getCurrentStage, 1).eq(LeaveRecord::getCurrentApproverId, manager.getId());
        }
        List<LeaveRecord> list = leaveService.list(wrapper);

        for (LeaveRecord r : list) {
            User u = userMap.get(r.getUserId());
            r.setApplicantName(u != null ? u.getName() : null);
            r.setLeaveTypeName(getLeaveTypeName(r.getLeaveType()));
        }
        return Result.success(list);
    }

    @PostMapping("/leave/approve/{id}")
    public Result<?> approveLeave(@AuthenticationPrincipal User manager,
                                 @PathVariable Long id,
                                 @RequestBody Map<String, Object> request) {
        Result<?> deny = requireDepartment(manager);
        if (deny != null) return deny;

        Integer status = request.get("status") == null ? null : Integer.valueOf(request.get("status").toString());
        if (status == null || (status != 1 && status != 2)) return Result.error("status必须为1(通过)或2(拒绝)");
        String opinion = request.get("opinion") == null ? null : request.get("opinion").toString();

        LeaveRecord record = leaveService.getById(id);
        if (record == null) return Result.error("记录不存在");

        User emp = userService.getById(record.getUserId());
        if (emp == null || !Objects.equals(emp.getDepartmentId(), manager.getDepartmentId())) return Result.error("无权限");

        if (record.getStatus() == null || record.getStatus() != 0) return Result.error("记录已处理");
        if (record.getCurrentStage() != null && record.getCurrentStage() != 1) return Result.error("当前不在主管审批阶段");
        if (record.getCurrentApproverId() != null && !Objects.equals(record.getCurrentApproverId(), manager.getId())) return Result.error("无权限");

        upsertApprovalRecord("leave", id, manager.getId(), status, opinion, 1);

        if (status == 2) {
            record.setStatus(2);
            record.setCurrentApproverId(null);
            leaveService.updateById(record);
            return Result.success();
        }

        Integer totalStage = record.getTotalStage() == null ? 1 : record.getTotalStage();
        if (totalStage <= 1) {
            record.setStatus(1);
            record.setCurrentApproverId(null);
            leaveService.updateById(record);
            return Result.success();
        }

        Long adminApproverId = resolveAdminApproverId(emp.getDepartmentId(), record.getLeaveType(), record.getDays());
        if (adminApproverId == null) return Result.error("未配置管理员终审审批人");
        record.setCurrentStage(2);
        record.setCurrentApproverId(adminApproverId);
        leaveService.updateById(record);
        return Result.success();
    }

    @GetMapping("/fieldwork/list")
    public Result<?> fieldworkList(@AuthenticationPrincipal User manager, @RequestParam Integer status) {
        Result<?> deny = requireDepartment(manager);
        if (deny != null) return deny;

        List<User> employees = getDepartmentEmployees(manager.getDepartmentId());
        if (employees.isEmpty()) return Result.success(Collections.emptyList());

        List<Long> employeeIds = employees.stream().map(User::getId).collect(Collectors.toList());
        Map<Long, User> userMap = toUserMap(employees);

        List<Fieldwork> list = fieldworkMapper.selectList(new LambdaQueryWrapper<Fieldwork>()
                .eq(Fieldwork::getStatus, status)
                .in(Fieldwork::getUserId, employeeIds)
                .orderByDesc(Fieldwork::getCreateTime));

        for (Fieldwork r : list) {
            User u = userMap.get(r.getUserId());
            r.setApplicantName(u != null ? u.getName() : null);
        }
        return Result.success(list);
    }

    @PostMapping("/fieldwork/approve/{id}")
    public Result<?> approveFieldwork(@AuthenticationPrincipal User manager,
                                      @PathVariable Long id,
                                      @RequestBody Map<String, Object> request) {
        Result<?> deny = requireDepartment(manager);
        if (deny != null) return deny;

        Integer status = request.get("status") == null ? null : Integer.valueOf(request.get("status").toString());
        if (status == null || (status != 1 && status != 2)) return Result.error("status必须为1(通过)或2(拒绝)");
        String opinion = request.get("opinion") == null ? null : request.get("opinion").toString();

        Fieldwork record = fieldworkMapper.selectById(id);
        if (record == null) return Result.error("记录不存在");

        User emp = userService.getById(record.getUserId());
        if (emp == null || !Objects.equals(emp.getDepartmentId(), manager.getDepartmentId())) return Result.error("无权限");

        if (record.getStatus() == null || record.getStatus() != 0) return Result.error("记录已处理");

        record.setStatus(status);
        fieldworkMapper.updateById(record);

        upsertApprovalRecord("fieldwork", id, manager.getId(), status, opinion, 1);
        return Result.success();
    }

    @GetMapping("/appeal/list")
    public Result<?> appealList(@AuthenticationPrincipal User manager, @RequestParam Integer status) {
        Result<?> deny = requireDepartment(manager);
        if (deny != null) return deny;

        List<User> employees = getDepartmentEmployees(manager.getDepartmentId());
        if (employees.isEmpty()) return Result.success(Collections.emptyList());

        List<Long> employeeIds = employees.stream().map(User::getId).collect(Collectors.toList());
        Map<Long, User> userMap = toUserMap(employees);

        List<Appeal> list = appealMapper.selectList(new LambdaQueryWrapper<Appeal>()
                .eq(Appeal::getStatus, status)
                .in(Appeal::getUserId, employeeIds)
                .orderByDesc(Appeal::getCreateTime));

        for (Appeal r : list) {
            User u = userMap.get(r.getUserId());
            r.setApplicantName(u != null ? u.getName() : null);
            r.setAppealTypeName(getAppealTypeName(r.getAppealType()));
        }
        return Result.success(list);
    }

    @PostMapping("/appeal/approve/{id}")
    public Result<?> approveAppeal(@AuthenticationPrincipal User manager,
                                   @PathVariable Long id,
                                   @RequestBody Map<String, Object> request) {
        Result<?> deny = requireDepartment(manager);
        if (deny != null) return deny;

        Integer status = request.get("status") == null ? null : Integer.valueOf(request.get("status").toString());
        if (status == null || (status != 1 && status != 2)) return Result.error("status必须为1(通过)或2(拒绝)");
        String opinion = request.get("opinion") == null ? null : request.get("opinion").toString();

        Appeal record = appealMapper.selectById(id);
        if (record == null) return Result.error("记录不存在");

        User emp = userService.getById(record.getUserId());
        if (emp == null || !Objects.equals(emp.getDepartmentId(), manager.getDepartmentId())) return Result.error("无权限");

        if (record.getStatus() == null || record.getStatus() != 0) return Result.error("记录已处理");

        record.setStatus(status);
        appealMapper.updateById(record);

        upsertApprovalRecord("appeal", id, manager.getId(), status, opinion, 1);
        return Result.success();
    }

    @GetMapping("/overtime/list")
    public Result<?> overtimeList(@AuthenticationPrincipal User manager, @RequestParam Integer status) {
        Result<?> deny = requireDepartment(manager);
        if (deny != null) return deny;

        List<User> employees = getDepartmentEmployees(manager.getDepartmentId());
        if (employees.isEmpty()) return Result.success(Collections.emptyList());

        List<Long> employeeIds = employees.stream().map(User::getId).collect(Collectors.toList());
        Map<Long, User> userMap = toUserMap(employees);

        LambdaQueryWrapper<OvertimeRecord> wrapper = new LambdaQueryWrapper<OvertimeRecord>()
                .eq(OvertimeRecord::getStatus, status)
                .in(OvertimeRecord::getUserId, employeeIds)
                .orderByDesc(OvertimeRecord::getCreateTime);
        if (status != null && status == 0) {
            wrapper.eq(OvertimeRecord::getCurrentStage, 1).eq(OvertimeRecord::getCurrentApproverId, manager.getId());
        }
        List<OvertimeRecord> list = overtimeRecordMapper.selectList(wrapper);

        for (OvertimeRecord r : list) {
            User u = userMap.get(r.getUserId());
            r.setApplicantName(u != null ? u.getName() : null);
            r.setOvertimeTypeName(getOvertimeTypeName(r.getOvertimeType()));
        }
        return Result.success(list);
    }

    @PostMapping("/overtime/approve/{id}")
    public Result<?> approveOvertime(@AuthenticationPrincipal User manager,
                                     @PathVariable Long id,
                                     @RequestBody Map<String, Object> request) {
        Result<?> deny = requireDepartment(manager);
        if (deny != null) return deny;

        Integer status = request.get("status") == null ? null : Integer.valueOf(request.get("status").toString());
        if (status == null || (status != 1 && status != 2)) return Result.error("status必须为1(通过)或2(拒绝)");
        String opinion = request.get("opinion") == null ? null : request.get("opinion").toString();

        OvertimeRecord record = overtimeRecordMapper.selectById(id);
        if (record == null) return Result.error("记录不存在");

        User emp = userService.getById(record.getUserId());
        if (emp == null || !Objects.equals(emp.getDepartmentId(), manager.getDepartmentId())) return Result.error("无权限");

        if (record.getStatus() == null || record.getStatus() != 0) return Result.error("记录已处理");
        if (record.getCurrentStage() == null || record.getCurrentStage() != 1) return Result.error("当前不在主管审批阶段");
        if (record.getCurrentApproverId() != null && !Objects.equals(record.getCurrentApproverId(), manager.getId())) return Result.error("无权限");

        upsertApprovalRecord("overtime", id, manager.getId(), status, opinion, 1);

        if (status == 2) {
            record.setStatus(2);
            record.setCurrentApproverId(null);
            overtimeRecordMapper.updateById(record);
            return Result.success();
        }

        User admin = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getRoleType, 1).last("limit 1"));
        Long adminId = admin == null ? null : admin.getId();
        if (adminId == null) return Result.error("未配置管理员账号");

        record.setStatus(1);
        record.setCurrentStage(2);
        record.setCurrentApproverId(adminId);
        overtimeRecordMapper.updateById(record);
        return Result.success();
    }

    private void upsertApprovalRecord(String recordType, Long recordId, Long approverId, Integer status, String opinion, Integer level) {
        LambdaQueryWrapper<ApprovalRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalRecord::getRecordType, recordType)
                .eq(ApprovalRecord::getRecordId, recordId)
                .eq(ApprovalRecord::getApproverId, approverId);

        ApprovalRecord existing = approvalRecordMapper.selectOne(wrapper);
        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            existing.setStatus(status);
            existing.setOpinion(opinion);
            existing.setApprovalTime(now);
            approvalRecordMapper.updateById(existing);
            return;
        }

        ApprovalRecord record = new ApprovalRecord();
        record.setRecordType(recordType);
        record.setRecordId(recordId);
        record.setApproverId(approverId);
        record.setStatus(status);
        record.setOpinion(opinion);
        record.setApprovalTime(now);
        record.setLevel(level);
        approvalRecordMapper.insert(record);
    }

    private String getOvertimeTypeName(Integer type) {
        if (type == null) return "其他";
        if (type == 1) return "工作日";
        if (type == 2) return "周末";
        if (type == 3) return "节假日";
        return "其他";
    }

    private Long resolveAdminApproverId(Long departmentId, Integer leaveType, Object daysObj) {
        java.math.BigDecimal days = daysObj == null ? java.math.BigDecimal.ZERO : new java.math.BigDecimal(daysObj.toString());
        List<ApprovalFlow> flows = approvalFlowMapper.selectAllWithApprover();
        flows.removeIf(f -> !"leave".equalsIgnoreCase(f.getRecordType() == null ? "leave" : f.getRecordType()));
        flows.removeIf(f -> (f.getNeedAdmin() == null || f.getNeedAdmin() != 1) && (f.getLevel() == null || f.getLevel() < 2));
        if (departmentId != null) {
            flows.removeIf(f -> f.getDepartmentId() != null && !departmentId.equals(f.getDepartmentId()));
        }
        flows.removeIf(f -> f.getLeaveType() != null && leaveType != null && !f.getLeaveType().equals(leaveType));
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
            boolean fSpecific = f.getLeaveType() != null;
            boolean sSpecific = selected.getLeaveType() != null;
            if (fSpecific && !sSpecific) selected = f;
        }
        if (selected != null && selected.getApproverId() != null) return selected.getApproverId();

        User admin = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getRoleType, 1).last("limit 1"));
        return admin == null ? null : admin.getId();
    }

    @GetMapping("/approval/records")
    public Result<?> approvalRecords(@AuthenticationPrincipal User manager,
                                    @RequestParam(required = false) String type) {
        Result<?> deny = requireDepartment(manager);
        if (deny != null) return deny;

        List<User> employees = getDepartmentEmployees(manager.getDepartmentId());
        if (employees.isEmpty()) return Result.success(Collections.emptyList());

        List<Long> employeeIds = employees.stream().map(User::getId).collect(Collectors.toList());
        Map<Long, User> employeeMap = toUserMap(employees);

        LambdaQueryWrapper<ApprovalRecord> wrapper = new LambdaQueryWrapper<>();
        if (type != null && !type.trim().isEmpty()) {
            wrapper.eq(ApprovalRecord::getRecordType, type.trim());
        }
        wrapper.orderByDesc(ApprovalRecord::getCreateTime);
        List<ApprovalRecord> records = approvalRecordMapper.selectList(wrapper);
        if (records.isEmpty()) return Result.success(Collections.emptyList());

        Set<Long> approverIds = records.stream().map(ApprovalRecord::getApproverId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> approverNameMap = userService.listByIds(approverIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName, (a, b) -> a));

        Map<String, List<Long>> idsByType = new HashMap<>();
        for (ApprovalRecord r : records) {
            if (r.getRecordType() == null || r.getRecordId() == null) continue;
            idsByType.computeIfAbsent(r.getRecordType(), k -> new ArrayList<>()).add(r.getRecordId());
        }

        Map<Long, LeaveRecord> leaveMap = new HashMap<>();
        Map<Long, Fieldwork> fieldworkMap = new HashMap<>();
        Map<Long, Appeal> appealMap = new HashMap<>();
        Map<Long, OvertimeRecord> overtimeMap = new HashMap<>();

        if (idsByType.containsKey("leave")) {
            for (LeaveRecord lr : leaveService.listByIds(new HashSet<>(idsByType.get("leave")))) {
                leaveMap.put(lr.getId(), lr);
            }
        }
        if (idsByType.containsKey("fieldwork")) {
            for (Fieldwork fw : fieldworkMapper.selectBatchIds(new HashSet<>(idsByType.get("fieldwork")))) {
                fieldworkMap.put(fw.getId(), fw);
            }
        }
        if (idsByType.containsKey("appeal")) {
            for (Appeal ap : appealMapper.selectBatchIds(new HashSet<>(idsByType.get("appeal")))) {
                appealMap.put(ap.getId(), ap);
            }
        }
        if (idsByType.containsKey("overtime")) {
            for (OvertimeRecord ot : overtimeRecordMapper.selectBatchIds(new HashSet<>(idsByType.get("overtime")))) {
                overtimeMap.put(ot.getId(), ot);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (ApprovalRecord r : records) {
            r.setApproverName(approverNameMap.get(r.getApproverId()));

            Map<String, Object> item = new HashMap<>();
            item.put("id", r.getId());
            item.put("recordType", r.getRecordType());
            item.put("recordId", r.getRecordId());
            item.put("approverId", r.getApproverId());
            item.put("approverName", r.getApproverName());
            item.put("status", r.getStatus());
            item.put("opinion", r.getOpinion());
            item.put("approvalTime", r.getApprovalTime());
            item.put("createTime", r.getCreateTime());

            if ("leave".equals(r.getRecordType())) {
                LeaveRecord lr = leaveMap.get(r.getRecordId());
                if (lr == null || !employeeMap.containsKey(lr.getUserId())) continue;
                User emp = employeeMap.get(lr.getUserId());
                item.put("applicantId", lr.getUserId());
                item.put("applicantName", emp != null ? emp.getName() : null);
                item.put("startDate", lr.getStartDate());
                item.put("endDate", lr.getEndDate());
                item.put("days", lr.getDays());
                item.put("leaveType", lr.getLeaveType());
                item.put("leaveTypeName", getLeaveTypeName(lr.getLeaveType()));
                item.put("reason", lr.getReason());
            } else if ("fieldwork".equals(r.getRecordType())) {
                Fieldwork fw = fieldworkMap.get(r.getRecordId());
                if (fw == null || !employeeMap.containsKey(fw.getUserId())) continue;
                User emp = employeeMap.get(fw.getUserId());
                item.put("applicantId", fw.getUserId());
                item.put("applicantName", emp != null ? emp.getName() : null);
                item.put("fieldworkDate", fw.getFieldworkDate());
                item.put("location", fw.getLocation());
                item.put("reason", fw.getReason());
            } else if ("appeal".equals(r.getRecordType())) {
                Appeal ap = appealMap.get(r.getRecordId());
                if (ap == null || !employeeMap.containsKey(ap.getUserId())) continue;
                User emp = employeeMap.get(ap.getUserId());
                item.put("applicantId", ap.getUserId());
                item.put("applicantName", emp != null ? emp.getName() : null);
                item.put("attendanceDate", ap.getAttendanceDate());
                item.put("appealType", ap.getAppealType());
                item.put("appealTypeName", getAppealTypeName(ap.getAppealType()));
                item.put("reason", ap.getReason());
            } else if ("overtime".equals(r.getRecordType())) {
                OvertimeRecord ot = overtimeMap.get(r.getRecordId());
                if (ot == null || !employeeMap.containsKey(ot.getUserId())) continue;
                User emp = employeeMap.get(ot.getUserId());
                item.put("applicantId", ot.getUserId());
                item.put("applicantName", emp != null ? emp.getName() : null);
                item.put("overtimeType", ot.getOvertimeType());
                item.put("overtimeTypeName", getOvertimeTypeName(ot.getOvertimeType()));
                item.put("startTime", ot.getStartTime());
                item.put("endTime", ot.getEndTime());
                item.put("hours", ot.getHours());
                item.put("reason", ot.getReason());
            } else {
                continue;
            }

            if (!employeeIds.contains(item.get("applicantId"))) continue;
            result.add(item);
        }

        return Result.success(result);
    }

    @GetMapping("/statistics")
    public Result<?> statistics(@AuthenticationPrincipal User manager,
                               @RequestParam String startDate,
                               @RequestParam String endDate) {
        Result<?> deny = requireDepartment(manager);
        if (deny != null) return deny;

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        if (end.isBefore(start)) return Result.error("结束日期不能早于开始日期");

        List<User> employees = getDepartmentEmployees(manager.getDepartmentId());
        Map<Long, User> userMap = toUserMap(employees);
        List<Long> employeeIds = employees.stream().map(User::getId).collect(Collectors.toList());

        List<Attendance> allAttendances = employeeIds.isEmpty() ? Collections.emptyList() :
                attendanceService.list(new LambdaQueryWrapper<Attendance>()
                        .in(Attendance::getUserId, employeeIds)
                        .between(Attendance::getAttendanceDate, start, end));

        Map<Long, List<Attendance>> attendanceByUser = allAttendances.stream()
                .collect(Collectors.groupingBy(Attendance::getUserId));

        List<Fieldwork> allFieldworks = employeeIds.isEmpty() ? Collections.emptyList() :
                fieldworkMapper.selectList(new LambdaQueryWrapper<Fieldwork>()
                        .eq(Fieldwork::getStatus, 1)
                        .in(Fieldwork::getUserId, employeeIds)
                        .between(Fieldwork::getFieldworkDate, start, end));

        Map<Long, Long> fieldworkCountByUser = allFieldworks.stream()
                .collect(Collectors.groupingBy(Fieldwork::getUserId, Collectors.counting()));

        int normalCount = 0;
        int lateCount = 0;
        int earlyCount = 0;
        int absentCount = 0;
        int abnormalCount = 0;
        int fieldworkCount = 0;

        List<Map<String, Object>> employeeStats = new ArrayList<>();

        for (User emp : employees) {
            List<Attendance> list = attendanceByUser.getOrDefault(emp.getId(), Collections.emptyList());
            int empNormal = 0;
            int empLate = 0;
            int empEarly = 0;
            int empAbsent = 0;
            int empAbnormal = 0;

            for (Attendance att : list) {
                if (att.getIsAbnormal() != null && att.getIsAbnormal() == 1) {
                    empAbnormal++;
                    abnormalCount++;
                }
                if (att.getStatus() != null && att.getStatus() == 3) {
                    empAbsent++;
                    absentCount++;
                }
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

            int empFieldwork = fieldworkCountByUser.getOrDefault(emp.getId(), 0L).intValue();
            fieldworkCount += empFieldwork;

            Map<String, Object> s = new HashMap<>();
            s.put("id", emp.getId());
            s.put("name", emp.getName());
            s.put("employeeNo", emp.getEmployeeNo());
            s.put("departmentId", emp.getDepartmentId());
            s.put("departmentName", emp.getDepartmentName());
            s.put("normal", empNormal);
            s.put("late", empLate);
            s.put("early", empEarly);
            s.put("absent", empAbsent);
            s.put("abnormal", empAbnormal);
            s.put("fieldwork", empFieldwork);
            employeeStats.add(s);
        }

        Department dept = departmentService.getById(manager.getDepartmentId());

        Map<String, Object> summary = new HashMap<>();
        summary.put("departmentId", manager.getDepartmentId());
        summary.put("departmentName", dept != null ? dept.getName() : null);
        summary.put("employeeCount", employees.size());
        summary.put("normal", normalCount);
        summary.put("late", lateCount);
        summary.put("early", earlyCount);
        summary.put("absent", absentCount);
        summary.put("abnormal", abnormalCount);
        summary.put("fieldwork", fieldworkCount);

        Map<String, Object> result = new HashMap<>();
        result.put("summary", summary);
        result.put("employees", employeeStats);
        return Result.success(result);
    }

    @GetMapping("/statistics/export")
    public void exportStatistics(@AuthenticationPrincipal User manager,
                                 @RequestParam String startDate,
                                 @RequestParam String endDate,
                                 HttpServletResponse response) throws IOException {
        Result<?> deny = requireDepartment(manager);
        if (deny != null) {
            response.setStatus(403);
            return;
        }

        Result<?> statsResult = statistics(manager, startDate, endDate);
        Map<String, Object> data = (Map<String, Object>) statsResult.getData();
        List<Map<String, Object>> employees = (List<Map<String, Object>>) data.get("employees");

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("部门考勤统计");

        XSSFCellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        String[] headers = {"姓名", "工号", "正常", "迟到", "早退", "旷工", "外勤", "异常"};
        XSSFRow headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
            headerRow.getCell(i).setCellStyle(headerStyle);
        }

        for (int i = 0; i < employees.size(); i++) {
            Map<String, Object> emp = employees.get(i);
            XSSFRow row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(emp.get("name") == null ? "" : emp.get("name").toString());
            row.createCell(1).setCellValue(emp.get("employeeNo") == null ? "" : emp.get("employeeNo").toString());
            row.createCell(2).setCellValue(Integer.parseInt(emp.get("normal").toString()));
            row.createCell(3).setCellValue(Integer.parseInt(emp.get("late").toString()));
            row.createCell(4).setCellValue(Integer.parseInt(emp.get("early").toString()));
            row.createCell(5).setCellValue(Integer.parseInt(emp.get("absent").toString()));
            row.createCell(6).setCellValue(Integer.parseInt(emp.get("fieldwork").toString()));
            row.createCell(7).setCellValue(Integer.parseInt(emp.get("abnormal").toString()));
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            int width = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.min(width + 1024, 20 * 256));
        }

        String fileName = "部门考勤报表_" + startDate + "_至_" + endDate + ".xlsx";
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString()).replaceAll("\\+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    @GetMapping("/notice/list")
    public Result<?> noticeList(@AuthenticationPrincipal User manager) {
        Result<?> deny = requireDepartment(manager);
        if (deny != null) return deny;

        List<Map<String, Object>> messages = new ArrayList<>();

        Result<?> homeResult = home(manager);
        Map<String, Object> home = (Map<String, Object>) homeResult.getData();
        Map<String, Object> pending = (Map<String, Object>) home.get("pending");

        Map<String, Object> m1 = new HashMap<>();
        m1.put("type", "pending_approval");
        m1.put("title", "待审批提醒");
        m1.put("content", "请假" + pending.get("leave") + "，外勤" + pending.get("fieldwork") + "，申诉" + pending.get("appeal"));
        m1.put("time", LocalDateTime.now());
        messages.add(m1);

        Map<String, Object> m2 = new HashMap<>();
        m2.put("type", "attendance_alert");
        m2.put("title", "当日考勤异常");
        m2.put("content", "异常人数：" + home.get("todayAbnormal"));
        m2.put("time", LocalDateTime.now());
        messages.add(m2);

        List<Map<String, Object>> recentApprovals = (List<Map<String, Object>>) approvalRecords(manager, null).getData();
        int limit = Math.min(20, recentApprovals.size());
        for (int i = 0; i < limit; i++) {
            Map<String, Object> r = recentApprovals.get(i);
            Map<String, Object> msg = new HashMap<>();
            msg.put("type", "approval_result");
            msg.put("title", "审批结果");
            String statusText = "已通过";
            Object st = r.get("status");
            if (st != null && Integer.parseInt(st.toString()) == 2) statusText = "已拒绝";
            msg.put("content", (r.get("applicantName") == null ? "" : r.get("applicantName").toString())
                    + " - " + statusText + "（" + (r.get("recordType") == null ? "" : r.get("recordType").toString()) + "）");
            msg.put("time", r.get("approvalTime") != null ? r.get("approvalTime") : r.get("createTime"));
            msg.put("recordType", r.get("recordType"));
            msg.put("recordId", r.get("recordId"));
            messages.add(msg);
        }

        return Result.success(messages);
    }
}
