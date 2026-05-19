package com.edu.live.controller;

import com.edu.live.common.Result;
import com.edu.live.entity.User;
import com.edu.live.service.StatsService;
import com.edu.live.vo.PlatformStatsVO;
import com.edu.live.vo.StudentStatsVO;
import com.edu.live.vo.TeacherCourseStatsVO;
import com.edu.live.vo.TeacherStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatsController {
    private final StatsService statsService;

    @GetMapping("/student/me")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<StudentStatsVO> studentStats(@AuthenticationPrincipal User user) {
        return Result.success(statsService.studentStats(user.getId()));
    }

    @GetMapping("/teacher/me")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<TeacherStatsVO> teacherStats(@AuthenticationPrincipal User user) {
        return Result.success(statsService.teacherStats(user.getId()));
    }

    @GetMapping("/teacher/courses")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<List<TeacherCourseStatsVO>> teacherCourseStats(@AuthenticationPrincipal User user,
                                                                 @RequestParam(required = false) Long courseId) {
        return Result.success(statsService.teacherCourseStats(user.getId(), courseId));
    }

    @GetMapping("/admin/platform")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PlatformStatsVO> platformStats() {
        return Result.success(statsService.platformStats());
    }
}
