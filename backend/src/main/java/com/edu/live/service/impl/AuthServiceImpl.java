package com.edu.live.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.live.common.BusinessException;
import com.edu.live.dto.AdminTeacherProfileRequest;
import com.edu.live.dto.ChangePasswordRequest;
import com.edu.live.dto.LoginRequest;
import com.edu.live.dto.RegisterRequest;
import com.edu.live.entity.TeacherProfile;
import com.edu.live.entity.User;
import com.edu.live.enums.UserRole;
import com.edu.live.mapper.TeacherProfileMapper;
import com.edu.live.mapper.UserMapper;
import com.edu.live.security.JwtUtil;
import com.edu.live.service.AuthService;
import com.edu.live.vo.AdminUserVO;
import com.edu.live.vo.LoginResponse;
import com.edu.live.vo.UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private static final Set<String> AVATAR_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final UserMapper userMapper;
    private final TeacherProfileMapper teacherProfileMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${file.avatar-path}")
    private String avatarPath;

    @Override
    @Transactional
    public UserInfo register(RegisterRequest request) {
        boolean exists = userMapper.exists(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())
                .or()
                .eq(User::getEmail, request.getEmail()));
        if (exists) {
            throw new BusinessException(400, "用户名或邮箱已存在");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());
        user.setStatus(1);
        userMapper.insert(user);
        if (UserRole.TEACHER.name().equals(request.getRole())) {
            TeacherProfile profile = new TeacherProfile();
            profile.setUserId(user.getId());
            profile.setTeachingYears(0);
            teacherProfileMapper.insert(profile);
        }
        return toUserInfo(user);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(400, "用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(403, "账号已被禁用");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginResponse(token, toUserInfo(user));
    }

    @Override
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null || !passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(400, "原密码错误");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
    }

    @Override
    public UserInfo currentUser(Long userId) {
        User user = requireUser(userId);
        return toUserInfo(user);
    }

    @Override
    public AdminUserVO currentTeacherProfile(Long userId) {
        User user = requireTeacher(userId);
        TeacherProfile profile = getTeacherProfile(userId);
        return toUserVO(user, profile);
    }

    @Override
    @Transactional
    public AdminUserVO updateTeacherProfile(Long userId, AdminTeacherProfileRequest request) {
        User user = requireTeacher(userId);
        TeacherProfile profile = getTeacherProfile(userId);
        if (profile == null) {
            profile = new TeacherProfile();
            profile.setUserId(userId);
            profile.setBio(request.getBio());
            profile.setTeachingYears(request.getTeachingYears() == null ? 0 : request.getTeachingYears());
            profile.setExpertise(request.getExpertise());
            teacherProfileMapper.insert(profile);
        } else {
            profile.setBio(request.getBio());
            profile.setTeachingYears(request.getTeachingYears() == null ? 0 : request.getTeachingYears());
            profile.setExpertise(request.getExpertise());
            teacherProfileMapper.updateById(profile);
        }
        return toUserVO(user, profile);
    }

    @Override
    public String uploadAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "头像文件不能为空");
        }
        if (!AVATAR_TYPES.contains(file.getContentType())) {
            throw new BusinessException(400, "头像仅支持 JPG、PNG、WEBP 格式");
        }
        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.')) : ".png";
        String datePath = LocalDate.now().toString().replace("-", "/");
        String fileName = UUID.randomUUID() + ext;
        Path dir = Path.of(avatarPath, datePath);
        try {
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(fileName));
        } catch (IOException e) {
            throw new BusinessException("头像上传失败");
        }
        String url = "/uploads/avatar/" + datePath + "/" + fileName;
        User user = userMapper.selectById(userId);
        user.setAvatar(url);
        userMapper.updateById(user);
        return url;
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

    private User requireTeacher(Long userId) {
        User user = requireUser(userId);
        if (!UserRole.TEACHER.name().equals(user.getRole())) {
            throw new BusinessException(403, "仅教师可维护教师资料");
        }
        return user;
    }

    private TeacherProfile getTeacherProfile(Long userId) {
        return teacherProfileMapper.selectOne(new LambdaQueryWrapper<TeacherProfile>().eq(TeacherProfile::getUserId, userId));
    }

    private UserInfo toUserInfo(User user) {
        return new UserInfo(user.getId(), user.getUsername(), user.getEmail(), user.getRole(), user.getStatus(), user.getAvatar());
    }

    private AdminUserVO toUserVO(User user, TeacherProfile profile) {
        AdminUserVO vo = new AdminUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setEmail(user.getEmail());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setAvatar(user.getAvatar());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setUpdatedAt(user.getUpdatedAt());
        if (profile != null) {
            vo.setBio(profile.getBio());
            vo.setTeachingYears(profile.getTeachingYears());
            vo.setExpertise(profile.getExpertise());
        }
        return vo;
    }
}
