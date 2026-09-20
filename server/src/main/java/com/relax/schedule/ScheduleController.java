package com.relax.schedule;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.relax.auth.CurrentUser;
import com.relax.common.api.ApiResponse;
import com.relax.technician.TechnicianAuditMapper;

@RestController
@RequestMapping("/api/v1")
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final TechnicianAuditMapper technicianAuditMapper;

    ScheduleController(ScheduleService scheduleService, TechnicianAuditMapper technicianAuditMapper) {
        this.scheduleService = scheduleService;
        this.technicianAuditMapper = technicianAuditMapper;
    }

    // === 公开接口：查看技师排班 ===
    @GetMapping("/technicians/{technicianId}/schedules")
    ApiResponse<List<ScheduleMapper.ScheduleView>> getTechnicianSchedules(
            @PathVariable long technicianId,
            @RequestParam(required = false) LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        return ApiResponse.success(scheduleService.listSchedulesByDate(technicianId, date));
    }

    // === 技师自己的排班管理 ===
    @GetMapping("/technician/schedules")
    ApiResponse<List<ScheduleMapper.ScheduleView>> listMySchedules(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(required = false) LocalDate fromDate) {
        long techId = getTechnicianId(currentUser.id());
        return ApiResponse.success(scheduleService.listSchedules(techId, fromDate));
    }

    @GetMapping("/technician/schedules/by-date")
    ApiResponse<List<ScheduleMapper.ScheduleView>> listMySchedulesByDate(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam @NotNull LocalDate date) {
        long techId = getTechnicianId(currentUser.id());
        return ApiResponse.success(scheduleService.listSchedulesByDate(techId, date));
    }

    @PostMapping("/technician/schedules")
    ApiResponse<ScheduleMapper.ScheduleView> createSchedule(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody ScheduleRequest request) {
        long techId = getTechnicianId(currentUser.id());
        return ApiResponse.success(scheduleService.createSchedule(techId,
                new ScheduleService.ScheduleRequest(request.scheduleDate(), request.startTime(),
                        request.endTime(), request.type())));
    }

    @PutMapping("/technician/schedules/{id}")
    ApiResponse<ScheduleMapper.ScheduleView> updateSchedule(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable long id, @Valid @RequestBody ScheduleRequest request) {
        long techId = getTechnicianId(currentUser.id());
        return ApiResponse.success(scheduleService.updateSchedule(techId, id,
                new ScheduleService.ScheduleRequest(request.scheduleDate(), request.startTime(),
                        request.endTime(), request.type())));
    }

    @DeleteMapping("/technician/schedules/{id}")
    ApiResponse<Void> deleteSchedule(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable long id) {
        long techId = getTechnicianId(currentUser.id());
        scheduleService.deleteSchedule(techId, id);
        return ApiResponse.success(null);
    }

    private long getTechnicianId(long userId) {
        return technicianAuditMapper.findAllTechnicians().stream()
                .filter(t -> t.userId() == userId)
                .findFirst()
                .map(TechnicianAuditMapper.TechnicianBrief::id)
                .orElseThrow(() -> new com.relax.common.api.BusinessException(
                        org.springframework.http.HttpStatus.FORBIDDEN, "NOT_TECHNICIAN", "当前账号不是技师"));
    }

    public record ScheduleRequest(
            @NotNull LocalDate scheduleDate,
            @NotBlank @Pattern(regexp = "\\d{2}:\\d{2}") String startTime,
            @NotBlank @Pattern(regexp = "\\d{2}:\\d{2}") String endTime,
            String type) {
    }
}
