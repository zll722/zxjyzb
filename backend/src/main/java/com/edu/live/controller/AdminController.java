package com.edu.live.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.live.common.Result;
import com.edu.live.dto.AdminResetPasswordRequest;
import com.edu.live.dto.AdminTeacherProfileRequest;
import com.edu.live.dto.AdminUserCreateRequest;
import com.edu.live.dto.BarrageKeywordRequest;
import com.edu.live.entity.BarrageKeyword;
import com.edu.live.entity.User;
import com.edu.live.service.AdminService;
import com.edu.live.vo.AdminUserVO;
import com.edu.live.vo.BarrageRecordVO;
import com.edu.live.vo.CourseVO;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
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

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminService adminService;

    @GetMapping("/users")
    public Result<Page<AdminUserVO>> users(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        return Result.success(adminService.pageUsers(current, size, role, status, keyword));
    }

    @GetMapping("/users/{id}")
    public Result<AdminUserVO> userDetail(@PathVariable Long id) {
        return Result.success(adminService.userDetail(id));
    }

    @PostMapping("/teachers")
    public Result<AdminUserVO> createTeacher(@Valid @RequestBody AdminUserCreateRequest request) {
        return Result.success(adminService.createTeacher(request));
    }

    @PostMapping("/admins")
    public Result<AdminUserVO> createAdmin(@Valid @RequestBody AdminUserCreateRequest request) {
        return Result.success(adminService.createAdmin(request));
    }

    @PostMapping("/users/{id}/status")
    public Result<Void> updateUserStatus(@AuthenticationPrincipal User user, @PathVariable Long id, @RequestParam Integer status) {
        adminService.updateUserStatus(user.getId(), id, status);
        return Result.success();
    }

    @PostMapping("/users/{id}/password")
    public Result<Void> resetUserPassword(@PathVariable Long id, @Valid @RequestBody AdminResetPasswordRequest request) {
        adminService.resetUserPassword(id, request);
        return Result.success();
    }

    @DeleteMapping("/users/{id}")
    public Result<Void> deleteUser(@AuthenticationPrincipal User user, @PathVariable Long id) {
        adminService.deleteUser(user.getId(), id);
        return Result.success();
    }

    @PutMapping("/teachers/{userId}/profile")
    public Result<AdminUserVO> updateTeacherProfile(@PathVariable Long userId, @Valid @RequestBody AdminTeacherProfileRequest request) {
        return Result.success(adminService.updateTeacherProfile(userId, request));
    }

    @GetMapping("/courses")
    public Result<Page<CourseVO>> courses(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status) {
        return Result.success(adminService.pageCourses(current, size, status));
    }

    @PostMapping("/courses/{id}/review")
    public Result<Void> reviewCourse(@PathVariable Long id, @RequestParam boolean approved) {
        adminService.reviewCourse(id, approved);
        return Result.success();
    }

    @GetMapping("/barrages")
    public Result<Page<BarrageRecordVO>> barrages(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) LocalDateTime startTime,
            @RequestParam(required = false) LocalDateTime endTime) {
        return Result.success(adminService.pageBarrages(current, size, roomId, keyword, startTime, endTime));
    }

    @DeleteMapping("/barrages/{id}")
    public Result<Void> deleteBarrage(@PathVariable Long id) {
        adminService.deleteBarrage(id);
        return Result.success();
    }

    @GetMapping("/barrage-keywords")
    public Result<List<BarrageKeyword>> barrageKeywords() {
        return Result.success(adminService.listBarrageKeywords());
    }

    @PostMapping("/barrage-keywords")
    public Result<BarrageKeyword> createBarrageKeyword(@Valid @RequestBody BarrageKeywordRequest request) {
        return Result.success(adminService.createBarrageKeyword(request));
    }

    @PutMapping("/barrage-keywords/{id}")
    public Result<BarrageKeyword> updateBarrageKeyword(@PathVariable Long id, @Valid @RequestBody BarrageKeywordRequest request) {
        return Result.success(adminService.updateBarrageKeyword(id, request));
    }

    @DeleteMapping("/barrage-keywords/{id}")
    public Result<Void> deleteBarrageKeyword(@PathVariable Long id) {
        adminService.deleteBarrageKeyword(id);
        return Result.success();
    }
}
