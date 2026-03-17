package com.attendance.controller;

import com.attendance.common.Result;
import com.attendance.entity.Attendance;
import com.attendance.entity.AttendanceRule;
import com.attendance.entity.Department;
import com.attendance.entity.User;
import com.attendance.mapper.AttendanceRuleMapper;
import com.attendance.mapper.DepartmentMapper;
import com.attendance.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private AttendanceRuleMapper attendanceRuleMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @GetMapping("/today")
    public Result<?> getTodayAttendance(@AuthenticationPrincipal User user) {
        Attendance attendance = attendanceService.getTodayAttendance(user.getId());
        return Result.success(attendance);
    }

    @GetMapping("/month")
    public Result<?> getMonthAttendance(@AuthenticationPrincipal User user,
                                        @RequestParam int year,
                                        @RequestParam int month) {
        List<Attendance> list = attendanceService.getMonthAttendance(user.getId(), year, month);
        Map<String, Object> stats = attendanceService.getMonthStats(user.getId(), year, month);
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("stats", stats);
        return Result.success(result);
    }

    @GetMapping("/month-stats")
    public Result<?> getMonthStats(@AuthenticationPrincipal User user) {
        LocalDate now = LocalDate.now();
        Map<String, Object> stats = attendanceService.getMonthStats(user.getId(), now.getYear(), now.getMonthValue());
        return Result.success(stats);
    }

    @GetMapping("/rule")
    public Result<?> getRule(@AuthenticationPrincipal User user) {
        AttendanceRule rule = null;
        if (user != null) {
            Long groupId = user.getAttendanceGroupId();
            if (groupId == null && user.getDepartmentId() != null) {
                Department dept = departmentMapper.selectById(user.getDepartmentId());
                if (dept != null && dept.getAttendanceGroupId() != null) {
                    groupId = dept.getAttendanceGroupId();
                } else {
                    groupId = user.getDepartmentId();
                }
            }
            rule = attendanceRuleMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AttendanceRule>()
                    .eq(AttendanceRule::getDepartmentId, groupId)
                    .eq(AttendanceRule::getEnabled, 1)
                    .orderByDesc(AttendanceRule::getId)
                    .last("limit 1"));
        }
        if (rule == null) {
            rule = attendanceRuleMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AttendanceRule>()
                    .eq(AttendanceRule::getEnabled, 1)
                    .orderByDesc(AttendanceRule::getId)
                    .last("limit 1"));
        }
        return Result.success(rule);
    }

    @PostMapping("/check-location")
    public Result<?> checkLocation(@AuthenticationPrincipal User user, @RequestBody Map<String, Double> location) {
        Double latitude = location.get("latitude");
        Double longitude = location.get("longitude");
        boolean valid = attendanceService.checkLocationValid(user == null ? null : user.getId(), latitude, longitude);
        Map<String, Object> result = new HashMap<>();
        result.put("valid", valid);
        return Result.success(result);
    }

    @PostMapping("/checkin")
    public Result<?> checkIn(@AuthenticationPrincipal User user, @RequestBody Map<String, Object> request) {
        String type = (String) request.get("type");
        Double latitude = request.get("latitude") != null ? Double.valueOf(request.get("latitude").toString()) : null;
        Double longitude = request.get("longitude") != null ? Double.valueOf(request.get("longitude").toString()) : null;
        String address = (String) request.get("address");
        String photo = (String) request.get("photo");
        
        try {
            attendanceService.checkIn(user.getId(), type, latitude, longitude, address, photo);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/export")
    public void exportPersonalReport(@AuthenticationPrincipal User user,
                                     @RequestParam int year,
                                     @RequestParam int month,
                                     HttpServletResponse response) throws IOException {
        List<Attendance> list = attendanceService.getMonthAttendance(user.getId(), year, month);
        Map<String, Object> stats = attendanceService.getMonthStats(user.getId(), year, month);

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("个人考勤报表");

        XSSFCellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        // 统计概览
        XSSFRow titleRow = sheet.createRow(0);
        titleRow.createCell(0).setCellValue(user.getName() + " - " + year + "年" + month + "月考勤报表");

        XSSFRow statsHeaderRow = sheet.createRow(2);
        String[] statsHeaders = {"正常", "迟到", "早退", "请假", "旷工"};
        for (int i = 0; i < statsHeaders.length; i++) {
            statsHeaderRow.createCell(i).setCellValue(statsHeaders[i]);
            statsHeaderRow.getCell(i).setCellStyle(headerStyle);
        }
        XSSFRow statsRow = sheet.createRow(3);
        statsRow.createCell(0).setCellValue(toInt(stats.get("normalDays")));
        statsRow.createCell(1).setCellValue(toInt(stats.get("lateDays")));
        statsRow.createCell(2).setCellValue(toInt(stats.get("earlyDays")));
        statsRow.createCell(3).setCellValue(toInt(stats.get("leaveDays")));
        statsRow.createCell(4).setCellValue(toInt(stats.get("absentDays")));

        // 明细表头
        XSSFRow detailHeaderRow = sheet.createRow(5);
        String[] headers = {"日期", "签到时间", "签到状态", "签退时间", "签退状态"};
        for (int i = 0; i < headers.length; i++) {
            detailHeaderRow.createCell(i).setCellValue(headers[i]);
            detailHeaderRow.getCell(i).setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 4500);
        }

        for (int i = 0; i < list.size(); i++) {
            Attendance att = list.get(i);
            XSSFRow row = sheet.createRow(i + 6);
            row.createCell(0).setCellValue(att.getAttendanceDate().toString());
            row.createCell(1).setCellValue(att.getCheckInTime() != null ? att.getCheckInTime() : "未签到");
            row.createCell(2).setCellValue(att.getCheckInStatus() != null ? (att.getCheckInStatus() == 0 ? "正常" : "迟到") : "");
            row.createCell(3).setCellValue(att.getCheckOutTime() != null ? att.getCheckOutTime() : "未签退");
            row.createCell(4).setCellValue(att.getCheckOutStatus() != null ? (att.getCheckOutStatus() == 0 ? "正常" : "早退") : "");
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=my_attendance_" + year + "_" + month + ".xlsx");
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    private int toInt(Object obj) {
        if (obj == null) return 0;
        try { return Integer.parseInt(obj.toString()); } catch (Exception e) { return 0; }
    }
}
