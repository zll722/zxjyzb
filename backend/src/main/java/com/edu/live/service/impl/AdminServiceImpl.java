package com.edu.live.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.live.common.BusinessException;
import com.edu.live.dto.AdminResetPasswordRequest;
import com.edu.live.dto.AdminTeacherProfileRequest;
import com.edu.live.dto.AdminUserCreateRequest;
import com.edu.live.dto.BarrageKeywordRequest;
import com.edu.live.entity.BarrageKeyword;
import com.edu.live.entity.Category;
import com.edu.live.entity.Course;
import com.edu.live.entity.LiveBarrage;
import com.edu.live.entity.LiveRoom;
import com.edu.live.entity.TeacherProfile;
import com.edu.live.entity.User;
import com.edu.live.enums.CourseStatus;
import com.edu.live.enums.UserRole;
import com.edu.live.mapper.BarrageKeywordMapper;
import com.edu.live.mapper.CategoryMapper;
import com.edu.live.mapper.CourseMapper;
import com.edu.live.mapper.LiveBarrageMapper;
import com.edu.live.mapper.LiveRoomMapper;
import com.edu.live.mapper.TeacherProfileMapper;
import com.edu.live.mapper.UserMapper;
import com.edu.live.service.AdminService;
import com.edu.live.vo.AdminUserVO;
import com.edu.live.vo.BarrageRecordVO;
import com.edu.live.vo.CourseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {
    private final UserMapper userMapper;
    private final TeacherProfileMapper teacherProfileMapper;
    private final CourseMapper courseMapper;
    private final CategoryMapper categoryMapper;
    private final LiveBarrageMapper liveBarrageMapper;
    private final LiveRoomMapper liveRoomMapper;
    private final BarrageKeywordMapper barrageKeywordMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Page<AdminUserVO> pageUsers(long current, long size, String role, Integer status, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(StringUtils.hasText(role), User::getRole, role)
                .eq(status != null, User::getStatus, status)
                .and(StringUtils.hasText(keyword), w -> w.like(User::getUsername, keyword).or().like(User::getEmail, keyword))
                .orderByDesc(User::getCreatedAt);
        Page<User> page = userMapper.selectPage(new Page<>(current, size), wrapper);
        Page<AdminUserVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(toUserVOList(page.getRecords()));
        return result;
    }

    @Override
    public AdminUserVO userDetail(Long id) {
        User user = requireUser(id);
        TeacherProfile profile = getTeacherProfile(id);
        return toUserVO(user, profile);
    }

    @Override
    @Transactional
    public AdminUserVO createTeacher(AdminUserCreateRequest request) {
        User user = createUser(request, UserRole.TEACHER.name());
        TeacherProfile profile = new TeacherProfile();
        profile.setUserId(user.getId());
        profile.setBio(request.getBio());
        profile.setTeachingYears(request.getTeachingYears() == null ? 0 : request.getTeachingYears());
        profile.setExpertise(request.getExpertise());
        teacherProfileMapper.insert(profile);
        return toUserVO(user, profile);
    }

    @Override
    public AdminUserVO createAdmin(AdminUserCreateRequest request) {
        User user = createUser(request, UserRole.ADMIN.name());
        return toUserVO(user, null);
    }

    @Override
    public void updateUserStatus(Long currentUserId, Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(400, "用户状态不合法");
        }
        if (currentUserId.equals(id) && status == 0) {
            throw new BusinessException(400, "不能禁用自己的账号");
        }
        User user = requireUser(id);
        user.setStatus(status);
        userMapper.updateById(user);
    }

    @Override
    public void resetUserPassword(Long id, AdminResetPasswordRequest request) {
        User user = requireUser(id);
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
    }

    @Override
    public void deleteUser(Long currentUserId, Long id) {
        if (currentUserId.equals(id)) {
            throw new BusinessException(400, "不能删除自己的账号");
        }
        requireUser(id);
        userMapper.deleteById(id);
    }

    @Override
    @Transactional
    public AdminUserVO updateTeacherProfile(Long userId, AdminTeacherProfileRequest request) {
        User user = requireUser(userId);
        if (!UserRole.TEACHER.name().equals(user.getRole())) {
            throw new BusinessException(400, "仅可维护教师资料");
        }
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
    public Page<CourseVO> pageCourses(long current, long size, String status) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<Course>()
                .eq(StringUtils.hasText(status), Course::getStatus, status)
                .orderByDesc(Course::getCreatedAt);
        Page<Course> page = courseMapper.selectPage(new Page<>(current, size), wrapper);
        Page<CourseVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(toCourseVOList(page.getRecords()));
        return result;
    }

    @Override
    public void reviewCourse(Long id, boolean approved) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException(404, "课程不存在");
        }
        course.setStatus(approved ? CourseStatus.PUBLISHED.name() : CourseStatus.OFFLINE.name());
        courseMapper.updateById(course);
    }

    @Override
    public Page<BarrageRecordVO> pageBarrages(long current, long size, Long roomId, String keyword, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<LiveBarrage> wrapper = new LambdaQueryWrapper<LiveBarrage>()
                .eq(roomId != null, LiveBarrage::getRoomId, roomId)
                .like(StringUtils.hasText(keyword), LiveBarrage::getContent, keyword)
                .ge(startTime != null, LiveBarrage::getCreatedAt, startTime)
                .le(endTime != null, LiveBarrage::getCreatedAt, endTime)
                .orderByDesc(LiveBarrage::getCreatedAt);
        Page<LiveBarrage> page = liveBarrageMapper.selectPage(new Page<>(current, size), wrapper);
        Page<BarrageRecordVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(toBarrageRecordVOList(page.getRecords()));
        return result;
    }

    @Override
    public void deleteBarrage(Long id) {
        if (liveBarrageMapper.deleteById(id) == 0) {
            throw new BusinessException(404, "弹幕不存在");
        }
    }

    @Override
    public List<BarrageKeyword> listBarrageKeywords() {
        return barrageKeywordMapper.selectList(new LambdaQueryWrapper<BarrageKeyword>().orderByDesc(BarrageKeyword::getCreatedAt));
    }

    @Override
    public BarrageKeyword createBarrageKeyword(BarrageKeywordRequest request) {
        String keyword = normalizeKeyword(request.getKeyword());
        if (barrageKeywordMapper.selectCount(new LambdaQueryWrapper<BarrageKeyword>().eq(BarrageKeyword::getKeyword, keyword)) > 0) {
            throw new BusinessException(400, "屏蔽词已存在");
        }
        BarrageKeyword barrageKeyword = new BarrageKeyword();
        barrageKeyword.setKeyword(keyword);
        barrageKeywordMapper.insert(barrageKeyword);
        return barrageKeyword;
    }

    @Override
    public BarrageKeyword updateBarrageKeyword(Long id, BarrageKeywordRequest request) {
        BarrageKeyword barrageKeyword = barrageKeywordMapper.selectById(id);
        if (barrageKeyword == null) {
            throw new BusinessException(404, "屏蔽词不存在");
        }
        String keyword = normalizeKeyword(request.getKeyword());
        if (barrageKeywordMapper.selectCount(new LambdaQueryWrapper<BarrageKeyword>()
                .eq(BarrageKeyword::getKeyword, keyword)
                .ne(BarrageKeyword::getId, id)) > 0) {
            throw new BusinessException(400, "屏蔽词已存在");
        }
        barrageKeyword.setKeyword(keyword);
        barrageKeywordMapper.updateById(barrageKeyword);
        return barrageKeyword;
    }

    @Override
    public void deleteBarrageKeyword(Long id) {
        if (barrageKeywordMapper.deleteById(id) == 0) {
            throw new BusinessException(404, "屏蔽词不存在");
        }
    }

    private User createUser(AdminUserCreateRequest request, String role) {
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())) > 0) {
            throw new BusinessException(400, "用户名已存在");
        }
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail())) > 0) {
            throw new BusinessException(400, "邮箱已被使用");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setRole(role);
        user.setStatus(1);
        userMapper.insert(user);
        return user;
    }

    private User requireUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

    private TeacherProfile getTeacherProfile(Long userId) {
        return teacherProfileMapper.selectOne(new LambdaQueryWrapper<TeacherProfile>().eq(TeacherProfile::getUserId, userId));
    }

    private List<AdminUserVO> toUserVOList(List<User> users) {
        if (users.isEmpty()) {
            return List.of();
        }
        Set<Long> teacherIds = users.stream()
                .filter(user -> UserRole.TEACHER.name().equals(user.getRole()))
                .map(User::getId)
                .collect(Collectors.toSet());
        Map<Long, TeacherProfile> profiles = teacherIds.isEmpty()
                ? Map.of()
                : teacherProfileMapper.selectList(new LambdaQueryWrapper<TeacherProfile>().in(TeacherProfile::getUserId, teacherIds))
                .stream()
                .collect(Collectors.toMap(TeacherProfile::getUserId, Function.identity()));
        return users.stream().map(user -> toUserVO(user, profiles.get(user.getId()))).toList();
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

    private List<CourseVO> toCourseVOList(List<Course> courses) {
        if (courses.isEmpty()) {
            return List.of();
        }
        Set<Long> categoryIds = courses.stream().map(Course::getCategoryId).collect(Collectors.toSet());
        Set<Long> teacherIds = courses.stream().map(Course::getTeacherId).collect(Collectors.toSet());
        Map<Long, Category> categories = categoryMapper.selectBatchIds(categoryIds).stream().collect(Collectors.toMap(Category::getId, Function.identity()));
        Map<Long, User> teachers = userMapper.selectBatchIds(teacherIds).stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return courses.stream().map(course -> toCourseVO(course, categories.get(course.getCategoryId()), teachers.get(course.getTeacherId()))).toList();
    }

    private List<BarrageRecordVO> toBarrageRecordVOList(List<LiveBarrage> barrages) {
        if (barrages.isEmpty()) {
            return List.of();
        }
        Set<Long> roomIds = barrages.stream().map(LiveBarrage::getRoomId).collect(Collectors.toSet());
        Set<Long> userIds = barrages.stream().map(LiveBarrage::getUserId).collect(Collectors.toSet());
        Map<Long, LiveRoom> rooms = liveRoomMapper.selectBatchIds(roomIds).stream().collect(Collectors.toMap(LiveRoom::getId, Function.identity()));
        Map<Long, User> users = userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return barrages.stream().map(barrage -> toBarrageRecordVO(barrage, rooms.get(barrage.getRoomId()), users.get(barrage.getUserId()))).toList();
    }

    private BarrageRecordVO toBarrageRecordVO(LiveBarrage barrage, LiveRoom room, User user) {
        BarrageRecordVO vo = new BarrageRecordVO();
        vo.setId(barrage.getId());
        vo.setRoomId(barrage.getRoomId());
        vo.setRoomTitle(room == null ? null : room.getTitle());
        vo.setUserId(barrage.getUserId());
        vo.setUsername(user == null ? null : user.getUsername());
        vo.setContent(barrage.getContent());
        vo.setCreatedAt(barrage.getCreatedAt());
        return vo;
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim();
    }

    private CourseVO toCourseVO(Course course, Category category, User teacher) {
        CourseVO vo = new CourseVO();
        vo.setId(course.getId());
        vo.setTitle(course.getTitle());
        vo.setCover(course.getCover());
        vo.setIntro(course.getIntro());
        vo.setCategoryId(course.getCategoryId());
        vo.setCategoryName(category == null ? null : category.getName());
        vo.setTeacherId(course.getTeacherId());
        vo.setTeacherName(teacher == null ? null : teacher.getUsername());
        vo.setPrice(course.getPrice());
        vo.setStatus(course.getStatus());
        vo.setFavoritesCount(course.getFavoritesCount());
        vo.setFavorite(false);
        vo.setCreatedAt(course.getCreatedAt());
        return vo;
    }
}
