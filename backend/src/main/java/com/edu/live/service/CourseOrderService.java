package com.edu.live.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.live.entity.CourseOrder;
import com.edu.live.vo.CourseOrderVO;

public interface CourseOrderService {
    CourseOrderVO create(Long userId, Long courseId);

    Page<CourseOrderVO> pageMine(Long userId, long current, long size, String status);

    CourseOrderVO detailMine(Long userId, Long id);

    CourseOrderVO pay(Long userId, Long id);

    CourseOrderVO cancel(Long userId, Long id);

    Page<CourseOrderVO> pageTeacher(Long teacherId, long current, long size, String status, Long userId, Long courseId);

    Page<CourseOrderVO> pageAdmin(long current, long size, String status, Long userId, Long courseId);

    boolean hasCourseAccess(Long userId, Long courseId);

    CourseOrder requirePaidOrder(Long userId, Long courseId);
}
