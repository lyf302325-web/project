package com.attendance.service;

import com.attendance.entity.*;
import com.attendance.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class AttendanceReminderJob {

    @Autowired
    private AttendanceRuleMapper attendanceRuleMapper;

    @Autowired
    private NoticeConfigMapper noticeConfigMapper;

    @Autowired
    private AttendanceMapper attendanceMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AbnormalRemindLogMapper abnormalRemindLogMapper;

    @Autowired
    private WechatSubscribeService wechatSubscribeService;

    @Scheduled(fixedDelay = 300000)
    public void remindMissingCheckInOut() {
        AttendanceRule rule = attendanceRuleMapper.selectList(null).stream().findFirst().orElse(null);
        if (rule == null) return;

        NoticeConfig cfg = noticeConfigMapper.selectList(null).stream().findFirst().orElse(null);
        if (cfg == null || cfg.getAttendanceAlert() == null || cfg.getAttendanceAlert() != 1) return;

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        int missInAfter = cfg.getMissCheckInAfterMinutes() == null ? 30 : cfg.getMissCheckInAfterMinutes();
        int missOutAfter = cfg.getMissCheckOutAfterMinutes() == null ? 30 : cfg.getMissCheckOutAfterMinutes();

        LocalTime checkIn = LocalTime.parse(rule.getCheckInTime());
        LocalTime checkOut = LocalTime.parse(rule.getCheckOutTime());

        boolean needMissIn = cfg.getMissCheckInRemind() != null && cfg.getMissCheckInRemind() == 1 && now.isAfter(checkIn.plusMinutes(missInAfter));
        boolean needMissOut = cfg.getMissCheckOutRemind() != null && cfg.getMissCheckOutRemind() == 1 && now.isAfter(checkOut.plusMinutes(missOutAfter));

        if (!needMissIn && !needMissOut) return;

        List<User> employees = userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getRoleType, 0));
        for (User emp : employees) {
            if (emp.getOpenid() == null || emp.getOpenid().isEmpty()) continue;

            Attendance att = attendanceMapper.selectOne(new LambdaQueryWrapper<Attendance>()
                    .eq(Attendance::getUserId, emp.getId())
                    .eq(Attendance::getAttendanceDate, today));

            if (needMissIn) {
                boolean missIn = att == null || att.getCheckInTime() == null || att.getCheckInTime().isEmpty();
                if (missIn && !existsLog(emp.getId(), today, "miss_check_in")) {
                    insertLog(emp.getId(), today, "miss_check_in");
                    wechatSubscribeService.sendAttendanceAbnormal(emp.getOpenid(), "未打卡提醒", today + " " + rule.getCheckInTime());
                }
            }

            if (needMissOut) {
                boolean missOut = att != null && att.getCheckInTime() != null && (att.getCheckOutTime() == null || att.getCheckOutTime().isEmpty());
                if (missOut && !existsLog(emp.getId(), today, "miss_check_out")) {
                    insertLog(emp.getId(), today, "miss_check_out");
                    wechatSubscribeService.sendAttendanceAbnormal(emp.getOpenid(), "未签退提醒", today + " " + rule.getCheckOutTime());
                }
            }
        }
    }

    public void remindLateEarly(User user, AttendanceRule rule, boolean late, boolean early) {
        NoticeConfig cfg = noticeConfigMapper.selectList(null).stream().findFirst().orElse(null);
        if (cfg == null || cfg.getAttendanceAlert() == null || cfg.getAttendanceAlert() != 1) return;
        if (user == null || user.getOpenid() == null || user.getOpenid().isEmpty()) return;

        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        if (late && cfg.getLateRemind() != null && cfg.getLateRemind() == 1) {
            wechatSubscribeService.sendAttendanceAbnormal(user.getOpenid(), "迟到提醒", now);
        }
        if (early && cfg.getEarlyRemind() != null && cfg.getEarlyRemind() == 1) {
            wechatSubscribeService.sendAttendanceAbnormal(user.getOpenid(), "早退提醒", now);
        }
    }

    private boolean existsLog(Long userId, LocalDate date, String type) {
        return abnormalRemindLogMapper.selectCount(new LambdaQueryWrapper<AbnormalRemindLog>()
                .eq(AbnormalRemindLog::getUserId, userId)
                .eq(AbnormalRemindLog::getAttendanceDate, date)
                .eq(AbnormalRemindLog::getRemindType, type)) > 0;
    }

    private void insertLog(Long userId, LocalDate date, String type) {
        AbnormalRemindLog log = new AbnormalRemindLog();
        log.setUserId(userId);
        log.setAttendanceDate(date);
        log.setRemindType(type);
        abnormalRemindLogMapper.insert(log);
    }
}

