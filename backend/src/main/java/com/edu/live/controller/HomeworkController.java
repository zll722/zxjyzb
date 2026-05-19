package com.edu.live.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.live.common.Result;
import com.edu.live.dto.HomeworkGradeRequest;
import com.edu.live.dto.HomeworkRequest;
import com.edu.live.entity.Homework;
import com.edu.live.entity.User;
import com.edu.live.service.HomeworkService;
import com.edu.live.vo.HomeworkSubmissionVO;
import com.edu.live.vo.HomeworkVO;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/homeworks")
@RequiredArgsConstructor
public class HomeworkController {
    private final HomeworkService homeworkService;

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Page<HomeworkVO>> pageStudent(@AuthenticationPrincipal User user,
                                                @RequestParam(defaultValue = "1") long current,
                                                @RequestParam(defaultValue = "20") long size) {
        return Result.success(homeworkService.pageStudent(user.getId(), current, size));
    }

    @GetMapping("/{id}")
    public Result<HomeworkVO> detail(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return Result.success(homeworkService.detail(user.getId(), user.getRole(), id));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<HomeworkSubmissionVO> submit(@AuthenticationPrincipal User user,
                                               @PathVariable Long id,
                                               @RequestParam(required = false) String content,
                                               @RequestPart(value = "file", required = false) MultipartFile file) {
        return Result.success(homeworkService.submit(user.getId(), id, content, file));
    }

    @GetMapping("/submissions/mine")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Page<HomeworkSubmissionVO>> mySubmissions(@AuthenticationPrincipal User user,
                                                            @RequestParam(defaultValue = "1") long current,
                                                            @RequestParam(defaultValue = "20") long size) {
        return Result.success(homeworkService.pageMySubmissions(user.getId(), current, size));
    }

    @GetMapping("/teacher/mine")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<Page<HomeworkVO>> pageTeacher(@AuthenticationPrincipal User user,
                                                @RequestParam(defaultValue = "1") long current,
                                                @RequestParam(defaultValue = "50") long size,
                                                @RequestParam(required = false) Long courseId) {
        return Result.success(homeworkService.pageTeacher(user.getId(), current, size, courseId));
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public Result<Homework> create(@AuthenticationPrincipal User user, @Valid @RequestBody HomeworkRequest request) {
        return Result.success(homeworkService.create(user.getId(), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<Homework> update(@AuthenticationPrincipal User user, @PathVariable Long id, @Valid @RequestBody HomeworkRequest request) {
        return Result.success(homeworkService.update(user.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<Void> delete(@AuthenticationPrincipal User user, @PathVariable Long id) {
        homeworkService.delete(user.getId(), id);
        return Result.success();
    }

    @GetMapping("/{id}/submissions")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<Page<HomeworkSubmissionVO>> submissions(@AuthenticationPrincipal User user,
                                                          @PathVariable Long id,
                                                          @RequestParam(defaultValue = "1") long current,
                                                          @RequestParam(defaultValue = "50") long size) {
        return Result.success(homeworkService.pageSubmissions(user.getId(), id, current, size));
    }

    @PostMapping("/{id}/submissions/{submissionId}/grade")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<HomeworkSubmissionVO> grade(@AuthenticationPrincipal User user,
                                              @PathVariable Long id,
                                              @PathVariable Long submissionId,
                                              @Valid @RequestBody HomeworkGradeRequest request) {
        return Result.success(homeworkService.grade(user.getId(), id, submissionId, request));
    }
}
