package com.edu.live.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.live.dto.ChapterRequest;
import com.edu.live.dto.CourseRequest;
import com.edu.live.dto.LearningRecordRequest;
import com.edu.live.entity.Chapter;
import com.edu.live.entity.Course;
import com.edu.live.entity.LearningRecord;
import com.edu.live.vo.CourseVO;
import com.edu.live.vo.RecentLearningRecordVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface CourseService {
    Page<CourseVO> pageCourses(long current, long size, Long categoryId, String keyword, String status, Long teacherId, Long userId);

    CourseVO detail(Long id, Long userId);

    Course create(Long teacherId, CourseRequest request);

    Course update(Long teacherId, Long id, CourseRequest request);

    void delete(Long teacherId, Long id);

    void submitReview(Long teacherId, Long id);

    void review(Long id, boolean approved);

    Chapter createChapter(Long teacherId, Long courseId, ChapterRequest request);

    Chapter uploadChapterVideo(Long teacherId, Long courseId, Long chapterId, MultipartFile file, Integer duration);

    ResponseEntity<Resource> streamChapterVideo(Long courseId, Long chapterId, Long userId, HttpServletRequest request);

    void deleteChapter(Long teacherId, Long courseId, Long chapterId);

    void favorite(Long userId, Long courseId);

    void unfavorite(Long userId, Long courseId);

    Page<CourseVO> pageFavoriteCourses(Long userId, long current, long size);

    Page<RecentLearningRecordVO> pageLearningRecords(Long userId, long current, long size);

    LearningRecord saveRecord(Long userId, LearningRecordRequest request);
}
