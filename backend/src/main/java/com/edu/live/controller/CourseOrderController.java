package com.edu.live.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.live.common.Result;
import com.edu.live.entity.User;
import com.edu.live.service.CourseOrderService;
import com.edu.live.vo.CourseOrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class CourseOrderController {
    private final CourseOrderService courseOrderService;

    @PostMapping("/courses/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<CourseOrderVO> create(@AuthenticationPrincipal User user, @PathVariable Long courseId) {
        return Result.success(courseOrderService.create(user.getId(), courseId));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Page<CourseOrderVO>> mine(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status) {
        return Result.success(courseOrderService.pageMine(user.getId(), current, size, status));
    }

    @GetMapping("/mine/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<CourseOrderVO> detail(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return Result.success(courseOrderService.detailMine(user.getId(), id));
    }

    @PostMapping("/mine/{id}/pay")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<CourseOrderVO> pay(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return Result.success(courseOrderService.pay(user.getId(), id));
    }

    @PostMapping("/mine/{id}/cancel")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<CourseOrderVO> cancel(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return Result.success(courseOrderService.cancel(user.getId(), id));
    }

    @GetMapping("/teacher")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<Page<CourseOrderVO>> teacherPage(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long courseId) {
        return Result.success(courseOrderService.pageTeacher(user.getId(), current, size, status, userId, courseId));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Page<CourseOrderVO>> adminPage(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long courseId) {
        return Result.success(courseOrderService.pageAdmin(current, size, status, userId, courseId));
    }
}
