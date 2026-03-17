package com.attendance.controller;

import com.attendance.common.Result;
import com.attendance.entity.Department;
import com.attendance.entity.OvertimeRecord;
import com.attendance.entity.ApprovalRecord;
import com.attendance.entity.User;
import com.attendance.mapper.ApprovalRecordMapper;
import com.attendance.mapper.OvertimeRecordMapper;
import com.attendance.service.DepartmentService;
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
@RequestMapping("/api/admin/overtime")
public class AdminOvertimeController {

    @Autowired
    private OvertimeRecordMapper overtimeRecordMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private ApprovalRecordMapper approvalRecordMapper;

    @GetMapping("/list")
    public Result<?> list(@RequestParam Integer status) {
        List<OvertimeRecord> list = overtimeRecordMapper.selectByStatus(status);
        if (status != null && status == 1) {
            list.removeIf(r -> r.getCurrentStage() == null || r.getCurrentStage() != 2);
        }
        for (OvertimeRecord r : list) {
            r.setOvertimeTypeName(getOvertimeTypeName(r.getOvertimeType()));
        }
        return Result.success(list);
    }

    @PostMapping("/approve/{id}")
    public Result<?> approve(@AuthenticationPrincipal User operator,
                             @PathVariable Long id,
                             @RequestBody Map<String, Object> request) {
        Integer status = request.get("status") == null ? null : Integer.valueOf(request.get("status").toString());
        if (status == null || (status != 3 && status != 2)) return Result.error("status必须为3(通过)或2(拒绝)");
        String opinion = request.get("opinion") == null ? null : request.get("opinion").toString();

        OvertimeRecord record = overtimeRecordMapper.selectById(id);
        if (record == null) return Result.error("记录不存在");
        if (record.getStatus() == null || record.getStatus() != 1) return Result.error("当前不在管理员终审阶段");

        ApprovalRecord ar = new ApprovalRecord();
        ar.setRecordType("overtime");
        ar.setRecordId(id);
        ar.setApproverId(operator.getId());
        ar.setStatus(status == 3 ? 1 : 2);
        ar.setOpinion(opinion);
        ar.setApprovalTime(LocalDateTime.now());
        ar.setLevel(2);
        approvalRecordMapper.insert(ar);

        record.setStatus(status);
        record.setCurrentApproverId(null);
        overtimeRecordMapper.updateById(record);
        return Result.success();
    }

    @GetMapping("/statistics")
    public Result<?> statistics(@RequestParam String startDate,
                                @RequestParam String endDate,
                                @RequestParam(required = false) Long departmentId) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        if (end.isBefore(start)) return Result.error("结束日期不能早于开始日期");

        List<User> employees = userService.list(new LambdaQueryWrapper<User>().eq(User::getRoleType, 0));
        if (departmentId != null) {
            employees.removeIf(u -> !Objects.equals(u.getDepartmentId(), departmentId));
        }
        Map<Long, User> userMap = employees.stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        List<Long> userIds = employees.stream().map(User::getId).collect(Collectors.toList());

        List<OvertimeRecord> records = userIds.isEmpty() ? Collections.emptyList() :
                overtimeRecordMapper.selectList(new LambdaQueryWrapper<OvertimeRecord>()
                        .in(OvertimeRecord::getUserId, userIds)
                        .eq(OvertimeRecord::getStatus, 3)
                        .between(OvertimeRecord::getStartTime, start.atStartOfDay(), end.plusDays(1).atStartOfDay())
                        .orderByDesc(OvertimeRecord::getCreateTime));

        Map<Long, java.math.BigDecimal> hoursByUser = new HashMap<>();
        for (OvertimeRecord r : records) {
            java.math.BigDecimal h = r.getHours() == null ? java.math.BigDecimal.ZERO : r.getHours();
            hoursByUser.put(r.getUserId(), hoursByUser.getOrDefault(r.getUserId(), java.math.BigDecimal.ZERO).add(h));
        }

        java.math.BigDecimal totalHours = java.math.BigDecimal.ZERO;
        for (java.math.BigDecimal h : hoursByUser.values()) totalHours = totalHours.add(h);

        List<Map<String, Object>> employeeStats = new ArrayList<>();
        for (User emp : employees) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", emp.getId());
            item.put("name", emp.getName());
            item.put("employeeNo", emp.getEmployeeNo());
            item.put("departmentId", emp.getDepartmentId());
            item.put("departmentName", emp.getDepartmentName());
            item.put("hours", hoursByUser.getOrDefault(emp.getId(), java.math.BigDecimal.ZERO));
            employeeStats.add(item);
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("departmentId", departmentId);
        if (departmentId != null) {
            Department dept = departmentService.getById(departmentId);
            summary.put("departmentName", dept == null ? null : dept.getName());
        }
        summary.put("employeeCount", employees.size());
        summary.put("totalHours", totalHours);

        Map<String, Object> result = new HashMap<>();
        result.put("summary", summary);
        result.put("employees", employeeStats);
        return Result.success(result);
    }

    @GetMapping("/statistics/export")
    public void export(@RequestParam String startDate,
                       @RequestParam String endDate,
                       @RequestParam(required = false) Long departmentId,
                       HttpServletResponse response) throws IOException {
        Result<?> statsResult = statistics(startDate, endDate, departmentId);
        Map<String, Object> data = (Map<String, Object>) statsResult.getData();
        List<Map<String, Object>> employees = (List<Map<String, Object>>) data.get("employees");

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("加班统计");

        XSSFCellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        String[] headers = {"姓名", "工号", "部门", "加班工时(小时)"};
        XSSFRow headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
            headerRow.getCell(i).setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 5000);
        }

        if (employees != null) {
            for (int i = 0; i < employees.size(); i++) {
                Map<String, Object> e = employees.get(i);
                XSSFRow row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(String.valueOf(e.get("name")));
                row.createCell(1).setCellValue(String.valueOf(e.get("employeeNo")));
                row.createCell(2).setCellValue(String.valueOf(e.get("departmentName")));
                row.createCell(3).setCellValue(String.valueOf(e.get("hours")));
            }
        }

        String fileName = URLEncoder.encode("overtime_" + startDate + "_" + endDate + ".xlsx", StandardCharsets.UTF_8.name());
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    private String getOvertimeTypeName(Integer type) {
        if (type == null) return "其他";
        if (type == 1) return "工作日";
        if (type == 2) return "周末";
        if (type == 3) return "节假日";
        return "其他";
    }
}

