package com.attendance.service.impl;

import com.attendance.entity.Attendance;
import com.attendance.entity.AttendanceRule;
import com.attendance.entity.Department;
import com.attendance.entity.User;
import com.attendance.mapper.AttendanceMapper;
import com.attendance.mapper.AttendanceRuleMapper;
import com.attendance.mapper.DepartmentMapper;
import com.attendance.mapper.UserMapper;
import com.attendance.service.AttendanceService;
import com.attendance.service.AttendanceReminderJob;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AttendanceServiceImpl extends ServiceImpl<AttendanceMapper, Attendance> implements AttendanceService {

    @Autowired
    private AttendanceRuleMapper attendanceRuleMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private AttendanceReminderJob attendanceReminderJob;

    @Override
    public Attendance getTodayAttendance(Long userId) {
        return getByUserIdAndDate(userId, LocalDate.now());
    }

    @Override
    public Attendance getByUserIdAndDate(Long userId, LocalDate date) {
        LambdaQueryWrapper<Attendance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Attendance::getUserId, userId)
               .eq(Attendance::getAttendanceDate, date);
        return getOne(wrapper);
    }

    @Override
    public List<Attendance> getMonthAttendance(Long userId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);
        
        LambdaQueryWrapper<Attendance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Attendance::getUserId, userId)
               .between(Attendance::getAttendanceDate, startDate, endDate)
               .orderByDesc(Attendance::getAttendanceDate);
        return list(wrapper);
    }

    @Override
    public Map<String, Object> getMonthStats(Long userId, int year, int month) {
        List<Attendance> attendanceList = getMonthAttendance(userId, year, month);
        
        int normalDays = 0, lateDays = 0, earlyDays = 0, leaveDays = 0, absentDays = 0;
        
        for (Attendance a : attendanceList) {
            if (a.getStatus() == 0) normalDays++;
            else if (a.getStatus() == 2) leaveDays++;
            else if (a.getStatus() == 3) absentDays++;
            
            if (a.getCheckInStatus() != null && a.getCheckInStatus() == 1) lateDays++;
            if (a.getCheckOutStatus() != null && a.getCheckOutStatus() == 2) earlyDays++;
        }
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("normalDays", normalDays);
        stats.put("lateDays", lateDays);
        stats.put("earlyDays", earlyDays);
        stats.put("leaveDays", leaveDays);
        stats.put("absentDays", absentDays);
        return stats;
    }

    @Override
    public void checkIn(Long userId, String type, Double latitude, Double longitude, String address, String photo) {
        LocalDate today = LocalDate.now();
        String currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        
        Attendance attendance = getByUserIdAndDate(userId, today);
        AttendanceRule rule = findRuleForUser(userId);
        User user = userMapper.selectById(userId);
        
        if ("in".equals(type)) {
            if (attendance == null) {
                attendance = new Attendance();
                attendance.setUserId(userId);
                attendance.setAttendanceDate(today);
            }
            attendance.setCheckInTime(currentTime);
            attendance.setCheckInLocation(address);
            if (photo != null && !photo.isEmpty()) {
                attendance.setCheckInPhoto(photo);
            }

            if (rule != null && rule.getCheckInTime() != null && rule.getEarlyCheckInMinutes() != null) {
                LocalTime base = LocalTime.parse(rule.getCheckInTime());
                LocalTime earliest = base.minusMinutes(rule.getEarlyCheckInMinutes());
                if (LocalTime.now().isBefore(earliest)) {
                    throw new RuntimeException("未到可签到时间");
                }
            }
            
            // 判断是否迟到
            if (rule != null) {
                LocalTime checkInTime = LocalTime.parse(rule.getCheckInTime());
                LocalTime lateTime = checkInTime.plusMinutes(rule.getLateThreshold());
                if (LocalTime.now().isAfter(lateTime)) {
                    attendance.setCheckInStatus(1); // 迟到
                } else {
                    attendance.setCheckInStatus(0); // 正常
                }
            } else {
                attendance.setCheckInStatus(0);
            }
        } else {
            if (attendance == null) {
                throw new RuntimeException("请先签到");
            }
            attendance.setCheckOutTime(currentTime);
            attendance.setCheckOutLocation(address);
            if (photo != null && !photo.isEmpty()) {
                attendance.setCheckOutPhoto(photo);
            }

            if (rule != null && rule.getCheckOutTime() != null && rule.getLateCheckOutMinutes() != null) {
                LocalTime base = LocalTime.parse(rule.getCheckOutTime());
                LocalTime latest = base.plusMinutes(rule.getLateCheckOutMinutes());
                if (LocalTime.now().isAfter(latest)) {
                    throw new RuntimeException("已超过可签退时间");
                }
            }
            
            // 判断是否早退
            if (rule != null) {
                LocalTime checkOutTime = LocalTime.parse(rule.getCheckOutTime());
                LocalTime earlyTime = checkOutTime.minusMinutes(rule.getEarlyThreshold());
                if (LocalTime.now().isBefore(earlyTime)) {
                    attendance.setCheckOutStatus(2); // 早退
                } else {
                    attendance.setCheckOutStatus(0); // 正常
                }
            } else {
                attendance.setCheckOutStatus(0);
            }
        }
        
        saveOrUpdate(attendance);

        if ("in".equals(type) && attendance.getCheckInStatus() != null && attendance.getCheckInStatus() == 1) {
            attendanceReminderJob.remindLateEarly(user, rule, true, false);
        }
        if (!"in".equals(type) && attendance.getCheckOutStatus() != null && attendance.getCheckOutStatus() == 2) {
            attendanceReminderJob.remindLateEarly(user, rule, false, true);
        }
    }

    @Override
    public boolean checkLocationValid(Long userId, Double latitude, Double longitude) {
        AttendanceRule rule = findRuleForUser(userId);
        if (rule == null || rule.getLatitude() == null || rule.getLongitude() == null) {
            return true; // 未配置位置，默认允许
        }
        
        double distance = calculateDistance(latitude, longitude, 
                rule.getLatitude().doubleValue(), rule.getLongitude().doubleValue());
        return distance <= rule.getCheckInRange();
    }

    private AttendanceRule findRuleForUser(Long userId) {
        if (userId == null) {
            AttendanceRule rule = attendanceRuleMapper.selectOne(new LambdaQueryWrapper<AttendanceRule>()
                    .eq(AttendanceRule::getEnabled, 1)
                    .orderByDesc(AttendanceRule::getId)
                    .last("limit 1"));
            return rule != null ? rule : attendanceRuleMapper.selectList(null).stream().findFirst().orElse(null);
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return attendanceRuleMapper.selectList(null).stream().findFirst().orElse(null);
        }

        Long groupId = user.getAttendanceGroupId();
        if (groupId == null && user.getDepartmentId() != null) {
            Department dept = departmentMapper.selectById(user.getDepartmentId());
            if (dept != null && dept.getAttendanceGroupId() != null) {
                groupId = dept.getAttendanceGroupId();
            } else {
                groupId = user.getDepartmentId();
            }
        }

        AttendanceRule rule = null;
        if (groupId != null) {
            rule = attendanceRuleMapper.selectOne(new LambdaQueryWrapper<AttendanceRule>()
                    .eq(AttendanceRule::getDepartmentId, groupId)
                    .eq(AttendanceRule::getEnabled, 1)
                    .orderByDesc(AttendanceRule::getId)
                    .last("limit 1"));
            if (rule == null) {
                rule = attendanceRuleMapper.selectOne(new LambdaQueryWrapper<AttendanceRule>()
                        .eq(AttendanceRule::getDepartmentId, groupId)
                        .orderByDesc(AttendanceRule::getId)
                        .last("limit 1"));
            }
        }
        if (rule == null) {
            rule = attendanceRuleMapper.selectOne(new LambdaQueryWrapper<AttendanceRule>()
                    .eq(AttendanceRule::getEnabled, 1)
                    .orderByDesc(AttendanceRule::getId)
                    .last("limit 1"));
        }
        if (rule == null) {
            rule = attendanceRuleMapper.selectList(null).stream().findFirst().orElse(null);
        }
        return rule;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // 地球半径（米）
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
