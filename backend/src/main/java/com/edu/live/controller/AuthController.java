package com.edu.live.controller;

import com.edu.live.common.Result;
import com.edu.live.dto.AdminTeacherProfileRequest;
import com.edu.live.dto.ChangePasswordRequest;
import com.edu.live.dto.LoginRequest;
import com.edu.live.dto.RegisterRequest;
import com.edu.live.entity.User;
import com.edu.live.service.AuthService;
import com.edu.live.vo.AdminUserVO;
import com.edu.live.vo.LoginResponse;
import com.edu.live.vo.UserInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public Result<UserInfo> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @GetMapping("/me")
    public Result<UserInfo> currentUser(@AuthenticationPrincipal User user) {
        return Result.success(authService.currentUser(user.getId()));
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@AuthenticationPrincipal User user, @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(user.getId(), request);
        return Result.success();
    }

    @GetMapping("/teacher/profile")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<AdminUserVO> currentTeacherProfile(@AuthenticationPrincipal User user) {
        return Result.success(authService.currentTeacherProfile(user.getId()));
    }

    @PutMapping("/teacher/profile")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<AdminUserVO> updateTeacherProfile(@AuthenticationPrincipal User user, @Valid @RequestBody AdminTeacherProfileRequest request) {
        return Result.success(authService.updateTeacherProfile(user.getId(), request));
    }

    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@AuthenticationPrincipal User user, @RequestPart("file") MultipartFile file) {
        return Result.success(authService.uploadAvatar(user.getId(), file));
    }
}
