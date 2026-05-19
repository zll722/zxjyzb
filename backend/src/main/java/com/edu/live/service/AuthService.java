package com.edu.live.service;

import com.edu.live.dto.AdminTeacherProfileRequest;
import com.edu.live.dto.ChangePasswordRequest;
import com.edu.live.dto.LoginRequest;
import com.edu.live.dto.RegisterRequest;
import com.edu.live.vo.AdminUserVO;
import com.edu.live.vo.LoginResponse;
import com.edu.live.vo.UserInfo;
import org.springframework.web.multipart.MultipartFile;

public interface AuthService {
    UserInfo register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);

    UserInfo currentUser(Long userId);

    AdminUserVO currentTeacherProfile(Long userId);

    AdminUserVO updateTeacherProfile(Long userId, AdminTeacherProfileRequest request);

    String uploadAvatar(Long userId, MultipartFile file);
}
