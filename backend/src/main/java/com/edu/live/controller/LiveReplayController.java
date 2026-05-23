package com.edu.live.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.live.common.Result;
import com.edu.live.dto.LiveReplayRequest;
import com.edu.live.entity.LiveReplay;
import com.edu.live.entity.User;
import com.edu.live.service.LiveReplayService;
import com.edu.live.vo.LiveReplayVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/replays")
@RequiredArgsConstructor
public class LiveReplayController {
    private final LiveReplayService liveReplayService;

    @GetMapping
    public Result<Page<LiveReplayVO>> page(@RequestParam(defaultValue = "1") long current,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) Long teacherId) {
        return Result.success(liveReplayService.pagePublished(current, size, teacherId));
    }

    @GetMapping("/{id}")
    public Result<LiveReplayVO> detail(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return Result.success(liveReplayService.detail(id, user == null ? null : user.getId()));
    }

    @GetMapping("/{id}/stream")
    public ResponseEntity<Resource> stream(@AuthenticationPrincipal User user, @PathVariable Long id, HttpServletRequest request) {
        return liveReplayService.stream(id, user == null ? null : user.getId(), request);
    }

    @GetMapping("/teacher/mine")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<Page<LiveReplayVO>> mine(@AuthenticationPrincipal User user,
                                           @RequestParam(defaultValue = "1") long current,
                                           @RequestParam(defaultValue = "50") long size) {
        return Result.success(liveReplayService.pageTeacher(user.getId(), current, size));
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public Result<LiveReplay> create(@AuthenticationPrincipal User user, @Valid @RequestBody LiveReplayRequest request) {
        return Result.success(liveReplayService.create(user.getId(), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<LiveReplay> update(@AuthenticationPrincipal User user, @PathVariable Long id, @Valid @RequestBody LiveReplayRequest request) {
        return Result.success(liveReplayService.update(user.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<Void> delete(@AuthenticationPrincipal User user, @PathVariable Long id) {
        liveReplayService.delete(user.getId(), id);
        return Result.success();
    }

    @PostMapping("/{id}/video")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<LiveReplayVO> uploadVideo(@AuthenticationPrincipal User user, @PathVariable Long id, @RequestPart("file") MultipartFile file) {
        return Result.success(liveReplayService.uploadVideo(user.getId(), id, file));
    }
}
