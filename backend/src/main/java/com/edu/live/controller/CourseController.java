package com.edu.live.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.live.common.BusinessException;
import com.edu.live.common.Result;
import com.edu.live.dto.ChapterRequest;
import com.edu.live.dto.CourseRequest;
import com.edu.live.dto.LearningRecordRequest;
import com.edu.live.entity.Chapter;
import com.edu.live.entity.Course;
import com.edu.live.entity.LearningRecord;
import com.edu.live.entity.User;
import com.edu.live.service.CourseService;
import com.edu.live.vo.CourseVO;
import com.edu.live.vo.RecentLearningRecordVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseController {
    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final CourseService courseService;

    @Value("${file.upload-path}")
    private String uploadPath;

    @PostMapping("/upload-cover")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<String> uploadCover(@RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "封面文件不能为空");
        }
        if (!IMAGE_TYPES.contains(file.getContentType())) {
            throw new BusinessException(400, "封面仅支持 JPG、PNG、WEBP、GIF 格式");
        }
        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.')) : ".jpg";
        String datePath = LocalDate.now().toString().replace("-", "/");
        String fileName = UUID.randomUUID() + ext;
        Path dir = Path.of(uploadPath, "cover", datePath);
        try {
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(fileName));
        } catch (IOException e) {
            throw new BusinessException("封面上传失败");
        }
        return Result.success("/uploads/cover/" + datePath + "/" + fileName);
    }

    @GetMapping
    public Result<Page<CourseVO>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long teacherId,
            @AuthenticationPrincipal User user) {
        Long userId = user == null ? null : user.getId();
        return Result.success(courseService.pageCourses(current, size, categoryId, keyword, status, teacherId, userId));
    }

    @GetMapping("/{id}")
    public Result<CourseVO> detail(@PathVariable Long id, @AuthenticationPrincipal User user) {
        Long userId = user == null ? null : user.getId();
        return Result.success(courseService.detail(id, userId));
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public Result<Course> create(@AuthenticationPrincipal User user, @Valid @RequestBody CourseRequest request) {
        return Result.success(courseService.create(user.getId(), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<Course> update(@AuthenticationPrincipal User user, @PathVariable Long id, @Valid @RequestBody CourseRequest request) {
        return Result.success(courseService.update(user.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<Void> delete(@AuthenticationPrincipal User user, @PathVariable Long id) {
        courseService.delete(user.getId(), id);
        return Result.success();
    }

    @PostMapping("/{id}/submit-review")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<Void> submitReview(@AuthenticationPrincipal User user, @PathVariable Long id) {
        courseService.submitReview(user.getId(), id);
        return Result.success();
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> review(@PathVariable Long id, @RequestParam boolean approved) {
        courseService.review(id, approved);
        return Result.success();
    }

    @PostMapping("/{courseId}/chapters")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<Chapter> createChapter(@AuthenticationPrincipal User user, @PathVariable Long courseId, @Valid @RequestBody ChapterRequest request) {
        return Result.success(courseService.createChapter(user.getId(), courseId, request));
    }

    @PostMapping("/{courseId}/chapters/{chapterId}/video")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<Chapter> uploadChapterVideo(@AuthenticationPrincipal User user, @PathVariable Long courseId, @PathVariable Long chapterId, @RequestPart("file") MultipartFile file, @RequestParam(required = false) Integer duration) {
        return Result.success(courseService.uploadChapterVideo(user.getId(), courseId, chapterId, file, duration));
    }

    @GetMapping("/{courseId}/chapters/{chapterId}/stream")
    public ResponseEntity<Resource> streamChapterVideo(@PathVariable Long courseId, @PathVariable Long chapterId, @AuthenticationPrincipal User user, HttpServletRequest request) {
        Long userId = user == null ? null : user.getId();
        return courseService.streamChapterVideo(courseId, chapterId, userId, request);
    }

    @DeleteMapping("/{courseId}/chapters/{chapterId}")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<Void> deleteChapter(@AuthenticationPrincipal User user, @PathVariable Long courseId, @PathVariable Long chapterId) {
        courseService.deleteChapter(user.getId(), courseId, chapterId);
        return Result.success();
    }

    @PostMapping("/{courseId}/favorite")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Void> favorite(@AuthenticationPrincipal User user, @PathVariable Long courseId) {
        courseService.favorite(user.getId(), courseId);
        return Result.success();
    }

    @DeleteMapping("/{courseId}/favorite")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Void> unfavorite(@AuthenticationPrincipal User user, @PathVariable Long courseId) {
        courseService.unfavorite(user.getId(), courseId);
        return Result.success();
    }

    @GetMapping("/favorites")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Page<CourseVO>> favoriteCourses(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(courseService.pageFavoriteCourses(user.getId(), current, size));
    }

    @GetMapping("/learning-records")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Page<RecentLearningRecordVO>> learningRecords(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(courseService.pageLearningRecords(user.getId(), current, size));
    }

    @PostMapping("/learning-records")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<LearningRecord> saveRecord(@AuthenticationPrincipal User user, @Valid @RequestBody LearningRecordRequest request) {
        return Result.success(courseService.saveRecord(user.getId(), request));
    }
}
