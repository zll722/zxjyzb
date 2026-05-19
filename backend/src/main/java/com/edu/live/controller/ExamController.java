package com.edu.live.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.live.common.Result;
import com.edu.live.dto.ExamGradeRequest;
import com.edu.live.dto.ExamQuestionRequest;
import com.edu.live.dto.ExamRequest;
import com.edu.live.dto.ExamSubmitRequest;
import com.edu.live.entity.Exam;
import com.edu.live.entity.ExamQuestion;
import com.edu.live.entity.User;
import com.edu.live.service.ExamService;
import com.edu.live.vo.ExamQuestionVO;
import com.edu.live.vo.ExamSubmissionVO;
import com.edu.live.vo.ExamVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/exams")
@RequiredArgsConstructor
public class ExamController {
    private final ExamService examService;

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Page<ExamVO>> pageStudent(@AuthenticationPrincipal User user,
                                            @RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "20") long size) {
        return Result.success(examService.pageStudent(user.getId(), current, size));
    }

    @GetMapping("/{id}")
    public Result<ExamVO> detail(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return Result.success(examService.detail(user.getId(), user.getRole(), id));
    }

    @GetMapping("/{id}/questions")
    public Result<List<ExamQuestionVO>> questions(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return Result.success(examService.questions(user.getId(), user.getRole(), id));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<ExamSubmissionVO> submit(@AuthenticationPrincipal User user,
                                           @PathVariable Long id,
                                           @Valid @RequestBody ExamSubmitRequest request) {
        return Result.success(examService.submit(user.getId(), id, request));
    }

    @GetMapping("/{id}/result")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<ExamSubmissionVO> myResult(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return Result.success(examService.myResult(user.getId(), id));
    }

    @GetMapping("/submissions/mine")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Page<ExamSubmissionVO>> mySubmissions(@AuthenticationPrincipal User user,
                                                        @RequestParam(defaultValue = "1") long current,
                                                        @RequestParam(defaultValue = "20") long size) {
        return Result.success(examService.pageMySubmissions(user.getId(), current, size));
    }

    @GetMapping("/teacher/mine")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<Page<ExamVO>> pageTeacher(@AuthenticationPrincipal User user,
                                            @RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "50") long size,
                                            @RequestParam(required = false) Long courseId) {
        return Result.success(examService.pageTeacher(user.getId(), current, size, courseId));
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public Result<Exam> create(@AuthenticationPrincipal User user, @Valid @RequestBody ExamRequest request) {
        return Result.success(examService.create(user.getId(), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<Exam> update(@AuthenticationPrincipal User user, @PathVariable Long id, @Valid @RequestBody ExamRequest request) {
        return Result.success(examService.update(user.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<Void> delete(@AuthenticationPrincipal User user, @PathVariable Long id) {
        examService.delete(user.getId(), id);
        return Result.success();
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<Exam> publish(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return Result.success(examService.publish(user.getId(), id));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<Exam> close(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return Result.success(examService.close(user.getId(), id));
    }

    @PostMapping("/{id}/questions")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<ExamQuestion> createQuestion(@AuthenticationPrincipal User user,
                                               @PathVariable Long id,
                                               @Valid @RequestBody ExamQuestionRequest request) {
        return Result.success(examService.createQuestion(user.getId(), id, request));
    }

    @PutMapping("/{id}/questions/{questionId}")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<ExamQuestion> updateQuestion(@AuthenticationPrincipal User user,
                                               @PathVariable Long id,
                                               @PathVariable Long questionId,
                                               @Valid @RequestBody ExamQuestionRequest request) {
        return Result.success(examService.updateQuestion(user.getId(), id, questionId, request));
    }

    @DeleteMapping("/{id}/questions/{questionId}")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<Void> deleteQuestion(@AuthenticationPrincipal User user,
                                       @PathVariable Long id,
                                       @PathVariable Long questionId) {
        examService.deleteQuestion(user.getId(), id, questionId);
        return Result.success();
    }

    @GetMapping("/{id}/submissions")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<Page<ExamSubmissionVO>> submissions(@AuthenticationPrincipal User user,
                                                      @PathVariable Long id,
                                                      @RequestParam(defaultValue = "1") long current,
                                                      @RequestParam(defaultValue = "50") long size) {
        return Result.success(examService.pageSubmissions(user.getId(), id, current, size));
    }

    @GetMapping("/{id}/submissions/{submissionId}")
    public Result<ExamSubmissionVO> submissionDetail(@AuthenticationPrincipal User user,
                                                     @PathVariable Long id,
                                                     @PathVariable Long submissionId) {
        return Result.success(examService.submissionDetail(user.getId(), user.getRole(), id, submissionId));
    }

    @PostMapping("/{id}/submissions/{submissionId}/grade")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<ExamSubmissionVO> grade(@AuthenticationPrincipal User user,
                                          @PathVariable Long id,
                                          @PathVariable Long submissionId,
                                          @Valid @RequestBody ExamGradeRequest request) {
        return Result.success(examService.grade(user.getId(), id, submissionId, request));
    }
}
