package com.attendance.service;

import com.attendance.entity.Attendance;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AttendanceService extends IService<Attendance> {
    Attendance getTodayAttendance(Long userId);
    Attendance getByUserIdAndDate(Long userId, LocalDate date);
    List<Attendance> getMonthAttendance(Long userId, int year, int month);
    Map<String, Object> getMonthStats(Long userId, int year, int month);
    void checkIn(Long userId, String type, Double latitude, Double longitude, String address, String photo);
    boolean checkLocationValid(Long userId, Double latitude, Double longitude);
}
