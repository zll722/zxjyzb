package com.edu.live.service;

import com.edu.live.vo.PlatformStatsVO;
import com.edu.live.vo.StudentStatsVO;
import com.edu.live.vo.TeacherCourseStatsVO;
import com.edu.live.vo.TeacherStatsVO;

import java.util.List;

public interface StatsService {
    StudentStatsVO studentStats(Long studentId);

    TeacherStatsVO teacherStats(Long teacherId);

    List<TeacherCourseStatsVO> teacherCourseStats(Long teacherId, Long courseId);

    PlatformStatsVO platformStats();
}
