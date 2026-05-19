package com.edu.live.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.live.common.BusinessException;
import com.edu.live.entity.Course;
import com.edu.live.entity.CourseOrder;
import com.edu.live.entity.User;
import com.edu.live.enums.CourseStatus;
import com.edu.live.enums.OrderStatus;
import com.edu.live.mapper.CourseMapper;
import com.edu.live.mapper.CourseOrderMapper;
import com.edu.live.mapper.UserMapper;
import com.edu.live.service.CourseOrderService;
import com.edu.live.vo.CourseOrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseOrderServiceImpl implements CourseOrderService {
    private final CourseOrderMapper courseOrderMapper;
    private final CourseMapper courseMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public CourseOrderVO create(Long userId, Long courseId) {
        Course course = requirePublishedCourse(courseId);
        if (isFree(course)) {
            throw new BusinessException(400, "免费课程无需创建订单");
        }
        if (hasPaidOrder(userId, courseId)) {
            throw new BusinessException(400, "已购买该课程");
        }
        CourseOrder existing = courseOrderMapper.selectOne(new LambdaQueryWrapper<CourseOrder>()
                .eq(CourseOrder::getUserId, userId)
                .eq(CourseOrder::getCourseId, courseId)
                .eq(CourseOrder::getStatus, OrderStatus.PENDING.name())
                .last("LIMIT 1"));
        if (existing != null) {
            return toVO(existing);
        }
        CourseOrder order = new CourseOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setCourseId(courseId);
        order.setAmount(course.getPrice());
        order.setStatus(OrderStatus.PENDING.name());
        courseOrderMapper.insert(order);
        return toVO(order);
    }

    @Override
    public Page<CourseOrderVO> pageMine(Long userId, long current, long size, String status) {
        Page<CourseOrder> page = courseOrderMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<CourseOrder>()
                .eq(CourseOrder::getUserId, userId)
                .eq(StringUtils.hasText(status), CourseOrder::getStatus, status)
                .orderByDesc(CourseOrder::getCreatedAt));
        return toVOPage(page);
    }

    @Override
    public CourseOrderVO detailMine(Long userId, Long id) {
        return toVO(requireUserOrder(userId, id));
    }

    @Override
    @Transactional
    public CourseOrderVO pay(Long userId, Long id) {
        CourseOrder order = requireUserOrder(userId, id);
        if (!OrderStatus.PENDING.name().equals(order.getStatus())) {
            throw new BusinessException(400, "仅待支付订单可支付");
        }
        if (hasPaidOrder(userId, order.getCourseId())) {
            throw new BusinessException(400, "已购买该课程");
        }
        requirePublishedCourse(order.getCourseId());
        order.setStatus(OrderStatus.PAID.name());
        order.setPaidAt(LocalDateTime.now());
        courseOrderMapper.updateById(order);
        courseOrderMapper.delete(new LambdaQueryWrapper<CourseOrder>()
                .eq(CourseOrder::getUserId, userId)
                .eq(CourseOrder::getCourseId, order.getCourseId())
                .eq(CourseOrder::getStatus, OrderStatus.PENDING.name())
                .ne(CourseOrder::getId, order.getId()));
        return toVO(order);
    }

    @Override
    public CourseOrderVO cancel(Long userId, Long id) {
        CourseOrder order = requireUserOrder(userId, id);
        if (!OrderStatus.PENDING.name().equals(order.getStatus())) {
            throw new BusinessException(400, "仅待支付订单可取消");
        }
        order.setStatus(OrderStatus.CANCELLED.name());
        courseOrderMapper.updateById(order);
        return toVO(order);
    }

    @Override
    public Page<CourseOrderVO> pageTeacher(Long teacherId, long current, long size, String status, Long userId, Long courseId) {
        LambdaQueryWrapper<CourseOrder> wrapper = new LambdaQueryWrapper<CourseOrder>()
                .eq(StringUtils.hasText(status), CourseOrder::getStatus, status)
                .eq(userId != null, CourseOrder::getUserId, userId)
                .eq(courseId != null, CourseOrder::getCourseId, courseId)
                .inSql(CourseOrder::getCourseId, "SELECT id FROM course WHERE teacher_id = " + teacherId + " AND deleted_at IS NULL")
                .orderByDesc(CourseOrder::getCreatedAt);
        Page<CourseOrder> page = courseOrderMapper.selectPage(new Page<>(current, size), wrapper);
        return toVOPage(page);
    }

    @Override
    public Page<CourseOrderVO> pageAdmin(long current, long size, String status, Long userId, Long courseId) {
        Page<CourseOrder> page = courseOrderMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<CourseOrder>()
                .eq(StringUtils.hasText(status), CourseOrder::getStatus, status)
                .eq(userId != null, CourseOrder::getUserId, userId)
                .eq(courseId != null, CourseOrder::getCourseId, courseId)
                .orderByDesc(CourseOrder::getCreatedAt));
        return toVOPage(page);
    }

    @Override
    public boolean hasCourseAccess(Long userId, Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null || !CourseStatus.PUBLISHED.name().equals(course.getStatus())) {
            return false;
        }
        return isFree(course) || userId != null && hasPaidOrder(userId, courseId);
    }

    @Override
    public CourseOrder requirePaidOrder(Long userId, Long courseId) {
        CourseOrder order = courseOrderMapper.selectOne(new LambdaQueryWrapper<CourseOrder>()
                .eq(CourseOrder::getUserId, userId)
                .eq(CourseOrder::getCourseId, courseId)
                .eq(CourseOrder::getStatus, OrderStatus.PAID.name())
                .last("LIMIT 1"));
        if (order == null) {
            throw new BusinessException(403, "请先购买课程");
        }
        return order;
    }

    private Course requirePublishedCourse(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null || !CourseStatus.PUBLISHED.name().equals(course.getStatus())) {
            throw new BusinessException(404, "课程不存在或未发布");
        }
        return course;
    }

    private CourseOrder requireUserOrder(Long userId, Long id) {
        CourseOrder order = courseOrderMapper.selectOne(new LambdaQueryWrapper<CourseOrder>()
                .eq(CourseOrder::getId, id)
                .eq(CourseOrder::getUserId, userId));
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        return order;
    }

    private boolean hasPaidOrder(Long userId, Long courseId) {
        return courseOrderMapper.exists(new LambdaQueryWrapper<CourseOrder>()
                .eq(CourseOrder::getUserId, userId)
                .eq(CourseOrder::getCourseId, courseId)
                .eq(CourseOrder::getStatus, OrderStatus.PAID.name()));
    }

    private boolean isFree(Course course) {
        return course.getPrice() == null || course.getPrice().compareTo(BigDecimal.ZERO) <= 0;
    }

    private String generateOrderNo() {
        return "CO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")) + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private Page<CourseOrderVO> toVOPage(Page<CourseOrder> page) {
        Page<CourseOrderVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(toVOList(page.getRecords()));
        return result;
    }

    private List<CourseOrderVO> toVOList(List<CourseOrder> orders) {
        if (orders.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = orders.stream().map(CourseOrder::getUserId).collect(Collectors.toSet());
        Set<Long> courseIds = orders.stream().map(CourseOrder::getCourseId).collect(Collectors.toSet());
        Map<Long, User> users = userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, Course> courses = courseMapper.selectBatchIds(courseIds).stream().collect(Collectors.toMap(Course::getId, Function.identity()));
        Set<Long> teacherIds = courses.values().stream().map(Course::getTeacherId).collect(Collectors.toSet());
        Map<Long, User> teachers = teacherIds.isEmpty() ? Map.of() : userMapper.selectBatchIds(teacherIds).stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return orders.stream().map(order -> fillVO(order, users.get(order.getUserId()), courses.get(order.getCourseId()), teachers)).toList();
    }

    private CourseOrderVO toVO(CourseOrder order) {
        Course course = courseMapper.selectById(order.getCourseId());
        User user = userMapper.selectById(order.getUserId());
        Map<Long, User> teachers = course == null ? Map.of() : Map.of(course.getTeacherId(), userMapper.selectById(course.getTeacherId()));
        return fillVO(order, user, course, teachers);
    }

    private CourseOrderVO fillVO(CourseOrder order, User user, Course course, Map<Long, User> teachers) {
        CourseOrderVO vo = new CourseOrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setUsername(user == null ? null : user.getUsername());
        vo.setCourseId(order.getCourseId());
        vo.setCourseTitle(course == null ? null : course.getTitle());
        vo.setTeacherId(course == null ? null : course.getTeacherId());
        User teacher = course == null ? null : teachers.get(course.getTeacherId());
        vo.setTeacherName(teacher == null ? null : teacher.getUsername());
        vo.setAmount(order.getAmount());
        vo.setStatus(order.getStatus());
        vo.setPaidAt(order.getPaidAt());
        vo.setCreatedAt(order.getCreatedAt());
        return vo;
    }
}
