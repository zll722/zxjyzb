package com.edu.live.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.live.dto.AdminResetPasswordRequest;
import com.edu.live.dto.AdminTeacherProfileRequest;
import com.edu.live.dto.AdminUserCreateRequest;
import com.edu.live.dto.BarrageKeywordRequest;
import com.edu.live.entity.BarrageKeyword;
import com.edu.live.vo.AdminUserVO;
import com.edu.live.vo.BarrageRecordVO;
import com.edu.live.vo.CourseVO;

import java.time.LocalDateTime;
import java.util.List;

public interface AdminService {
    Page<AdminUserVO> pageUsers(long current, long size, String role, Integer status, String keyword);

    AdminUserVO userDetail(Long id);

    AdminUserVO createTeacher(AdminUserCreateRequest request);

    AdminUserVO createAdmin(AdminUserCreateRequest request);

    void updateUserStatus(Long currentUserId, Long id, Integer status);

    void resetUserPassword(Long id, AdminResetPasswordRequest request);

    void deleteUser(Long currentUserId, Long id);

    AdminUserVO updateTeacherProfile(Long userId, AdminTeacherProfileRequest request);

    Page<CourseVO> pageCourses(long current, long size, String status);

    void reviewCourse(Long id, boolean approved);

    Page<BarrageRecordVO> pageBarrages(long current, long size, Long roomId, String keyword, LocalDateTime startTime, LocalDateTime endTime);

    void deleteBarrage(Long id);

    List<BarrageKeyword> listBarrageKeywords();

    BarrageKeyword createBarrageKeyword(BarrageKeywordRequest request);

    BarrageKeyword updateBarrageKeyword(Long id, BarrageKeywordRequest request);

    void deleteBarrageKeyword(Long id);
}
