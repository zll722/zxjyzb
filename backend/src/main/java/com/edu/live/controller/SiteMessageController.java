package com.edu.live.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.live.common.Result;
import com.edu.live.dto.SiteMessageRequest;
import com.edu.live.entity.SiteMessage;
import com.edu.live.entity.User;
import com.edu.live.service.SiteMessageService;
import com.edu.live.vo.SiteMessageVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class SiteMessageController {
    private final SiteMessageService siteMessageService;

    @GetMapping
    public Result<Page<SiteMessageVO>> page(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Integer readStatus) {
        return Result.success(siteMessageService.pageMyMessages(user.getId(), current, size, readStatus));
    }

    @GetMapping("/mine")
    public Result<Page<SiteMessageVO>> mine(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Integer readStatus) {
        return Result.success(siteMessageService.pageMyMessages(user.getId(), current, size, readStatus));
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount(@AuthenticationPrincipal User user) {
        return Result.success(siteMessageService.unreadCount(user.getId()));
    }

    @GetMapping("/{id}")
    public Result<SiteMessageVO> detail(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return Result.success(siteMessageService.detail(user.getId(), id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<SiteMessage> send(@AuthenticationPrincipal User user, @Valid @RequestBody SiteMessageRequest request) {
        return Result.success(siteMessageService.send(user.getId(), request));
    }

    @PostMapping("/{id}/read")
    public Result<Void> markRead(@AuthenticationPrincipal User user, @PathVariable Long id) {
        siteMessageService.markRead(user.getId(), id);
        return Result.success();
    }

    @PostMapping("/read-all")
    public Result<Void> markAllRead(@AuthenticationPrincipal User user) {
        siteMessageService.markAllRead(user.getId());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@AuthenticationPrincipal User user, @PathVariable Long id) {
        siteMessageService.delete(user.getId(), id);
        return Result.success();
    }
}
