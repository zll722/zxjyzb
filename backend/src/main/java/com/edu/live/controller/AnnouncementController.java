package com.edu.live.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.live.common.Result;
import com.edu.live.dto.AnnouncementRequest;
import com.edu.live.entity.Announcement;
import com.edu.live.entity.User;
import com.edu.live.service.AnnouncementService;
import com.edu.live.vo.AnnouncementVO;
import jakarta.validation.Valid;
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
@RequestMapping("/announcements")
@RequiredArgsConstructor
public class AnnouncementController {
    private final AnnouncementService announcementService;

    @GetMapping
    public Result<Page<AnnouncementVO>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type) {
        return Result.success(announcementService.pagePublic(current, size, keyword, type));
    }

    @GetMapping("/{id}")
    public Result<AnnouncementVO> detail(@PathVariable Long id) {
        return Result.success(announcementService.publicDetail(id));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Page<AnnouncementVO>> adminPage(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type) {
        return Result.success(announcementService.pageAdmin(current, size, status, keyword, type));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Announcement> create(@AuthenticationPrincipal User user, @Valid @RequestBody AnnouncementRequest request) {
        return Result.success(announcementService.create(user.getId(), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Announcement> update(@PathVariable Long id, @Valid @RequestBody AnnouncementRequest request) {
        return Result.success(announcementService.update(id, request));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Announcement> publish(@PathVariable Long id, @RequestParam(defaultValue = "false") Boolean syncMessage) {
        return Result.success(announcementService.publish(id, syncMessage));
    }

    @PostMapping("/{id}/offline")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Announcement> offline(@PathVariable Long id) {
        return Result.success(announcementService.offline(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        announcementService.delete(id);
        return Result.success();
    }
}
