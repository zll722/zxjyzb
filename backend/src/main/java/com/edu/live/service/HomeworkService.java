package com.edu.live.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.live.dto.HomeworkGradeRequest;
import com.edu.live.dto.HomeworkRequest;
import com.edu.live.entity.Homework;
import com.edu.live.vo.HomeworkSubmissionVO;
import com.edu.live.vo.HomeworkVO;
import org.springframework.web.multipart.MultipartFile;

public interface HomeworkService {
    Page<HomeworkVO> pageStudent(Long studentId, long current, long size);

    Page<HomeworkVO> pageTeacher(Long teacherId, long current, long size, Long courseId);

    HomeworkVO detail(Long userId, String role, Long id);

    Homework create(Long teacherId, HomeworkRequest request);

    Homework update(Long teacherId, Long id, HomeworkRequest request);

    void delete(Long teacherId, Long id);

    HomeworkSubmissionVO submit(Long studentId, Long homeworkId, String content, MultipartFile file);

    Page<HomeworkSubmissionVO> pageSubmissions(Long teacherId, Long homeworkId, long current, long size);

    HomeworkSubmissionVO grade(Long teacherId, Long homeworkId, Long submissionId, HomeworkGradeRequest request);

    Page<HomeworkSubmissionVO> pageMySubmissions(Long studentId, long current, long size);
}
