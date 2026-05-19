package com.edu.live.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.live.dto.ExamGradeRequest;
import com.edu.live.dto.ExamQuestionRequest;
import com.edu.live.dto.ExamRequest;
import com.edu.live.dto.ExamSubmitRequest;
import com.edu.live.entity.Exam;
import com.edu.live.entity.ExamQuestion;
import com.edu.live.vo.ExamQuestionVO;
import com.edu.live.vo.ExamSubmissionVO;
import com.edu.live.vo.ExamVO;

import java.util.List;

public interface ExamService {
    Page<ExamVO> pageStudent(Long studentId, long current, long size);

    Page<ExamVO> pageTeacher(Long teacherId, long current, long size, Long courseId);

    ExamVO detail(Long userId, String role, Long id);

    Exam create(Long teacherId, ExamRequest request);

    Exam update(Long teacherId, Long id, ExamRequest request);

    void delete(Long teacherId, Long id);

    Exam publish(Long teacherId, Long id);

    Exam close(Long teacherId, Long id);

    List<ExamQuestionVO> questions(Long userId, String role, Long examId);

    ExamQuestion createQuestion(Long teacherId, Long examId, ExamQuestionRequest request);

    ExamQuestion updateQuestion(Long teacherId, Long examId, Long questionId, ExamQuestionRequest request);

    void deleteQuestion(Long teacherId, Long examId, Long questionId);

    ExamSubmissionVO submit(Long studentId, Long examId, ExamSubmitRequest request);

    Page<ExamSubmissionVO> pageSubmissions(Long teacherId, Long examId, long current, long size);

    ExamSubmissionVO submissionDetail(Long userId, String role, Long examId, Long submissionId);

    ExamSubmissionVO myResult(Long studentId, Long examId);

    Page<ExamSubmissionVO> pageMySubmissions(Long studentId, long current, long size);

    ExamSubmissionVO grade(Long teacherId, Long examId, Long submissionId, ExamGradeRequest request);
}
