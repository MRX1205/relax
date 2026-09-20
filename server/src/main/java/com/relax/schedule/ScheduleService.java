package com.relax.schedule;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.relax.common.api.BusinessException;

@Service
public class ScheduleService {

    private final ScheduleMapper scheduleMapper;

    ScheduleService(ScheduleMapper scheduleMapper) {
        this.scheduleMapper = scheduleMapper;
    }

    public List<ScheduleMapper.ScheduleView> listSchedules(long technicianId, LocalDate fromDate) {
        if (fromDate == null) {
            fromDate = LocalDate.now();
        }
        return scheduleMapper.findByTechnician(technicianId, fromDate);
    }

    public List<ScheduleMapper.ScheduleView> listSchedulesByDate(long technicianId, LocalDate date) {
        return scheduleMapper.findByTechnicianAndDate(technicianId, date);
    }

    @Transactional
    public ScheduleMapper.ScheduleView createSchedule(long technicianId, ScheduleRequest request) {
        validateTimeRange(request.startTime(), request.endTime());
        if (scheduleMapper.countConflict(technicianId, request.scheduleDate(), request.startTime(), request.endTime()) > 0) {
            throw new BusinessException("SCHEDULE_CONFLICT", "该时段与已有排班冲突");
        }
        long id = IdWorker.getId();
        scheduleMapper.insert(id, technicianId, request.scheduleDate(), request.startTime(), request.endTime(),
                request.type() == null ? "AVAILABLE" : request.type());
        return scheduleMapper.findById(id).orElseThrow();
    }

    @Transactional
    public ScheduleMapper.ScheduleView updateSchedule(long technicianId, long scheduleId, ScheduleRequest request) {
        ScheduleMapper.ScheduleView existing = scheduleMapper.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "SCHEDULE_NOT_FOUND", "排班不存在"));
        if (existing.technicianId() != technicianId) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "SCHEDULE_NOT_OWNED", "无权操作该排班");
        }
        validateTimeRange(request.startTime(), request.endTime());
        if (scheduleMapper.countConflictExcluding(technicianId, request.scheduleDate(),
                request.startTime(), request.endTime(), scheduleId) > 0) {
            throw new BusinessException("SCHEDULE_CONFLICT", "该时段与已有排班冲突");
        }
        scheduleMapper.update(scheduleId, request.startTime(), request.endTime(),
                request.type() == null ? "AVAILABLE" : request.type());
        return scheduleMapper.findById(scheduleId).orElseThrow();
    }

    @Transactional
    public void deleteSchedule(long technicianId, long scheduleId) {
        ScheduleMapper.ScheduleView existing = scheduleMapper.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "SCHEDULE_NOT_FOUND", "排班不存在"));
        if (existing.technicianId() != technicianId) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "SCHEDULE_NOT_OWNED", "无权操作该排班");
        }
        scheduleMapper.delete(scheduleId);
    }

    private void validateTimeRange(String startTime, String endTime) {
        if (startTime.compareTo(endTime) >= 0) {
            throw new BusinessException("INVALID_TIME_RANGE", "开始时间必须早于结束时间");
        }
    }

    public record ScheduleRequest(LocalDate scheduleDate, String startTime, String endTime, String type) {
    }
}
