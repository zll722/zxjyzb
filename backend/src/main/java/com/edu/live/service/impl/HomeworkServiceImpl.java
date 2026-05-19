package com.edu.live.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.live.common.BusinessException;
import com.edu.live.dto.HomeworkGradeRequest;
import com.edu.live.dto.HomeworkRequest;
import com.edu.live.entity.Course;
import com.edu.live.entity.Homework;
import com.edu.live.entity.HomeworkSubmission;
import com.edu.live.entity.User;
import com.edu.live.mapper.CourseMapper;
import com.edu.live.mapper.HomeworkMapper;
import com.edu.live.mapper.HomeworkSubmissionMapper;
import com.edu.live.mapper.UserMapper;
import com.edu.live.service.HomeworkService;
import com.edu.live.vo.HomeworkSubmissionVO;
import com.edu.live.vo.HomeworkVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class HomeworkServiceImpl implements HomeworkService {
    private static final Set<String> ATTACHMENT_TYPES = Set.of("text/plain", "application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/zip", "application/x-zip-compressed", "image/jpeg", "image/png", "image/webp");

    private final HomeworkMapper homeworkMapper;
    private final HomeworkSubmissionMapper submissionMapper;
    private final CourseMapper courseMapper;
    private final UserMapper userMapper;

    @Value("${file.homework-path}")
    private String homeworkPath;

    @Value("${file.homework-max-size}")
    private long homeworkMaxSize;

    @Override
    public Page<HomeworkVO> pageStudent(Long studentId, long current, long size) {
        Page<Homework> page = homeworkMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<Homework>().orderByDesc(Homework::getCreatedAt));
        Page<HomeworkVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(toHomeworkVOList(page.getRecords(), studentId));
        return result;
    }

    @Override
    public Page<HomeworkVO> pageTeacher(Long teacherId, long current, long size, Long courseId) {
        Page<Homework> page = homeworkMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<Homework>()
                .eq(Homework::getTeacherId, teacherId)
                .eq(courseId != null, Homework::getCourseId, courseId)
                .orderByDesc(Homework::getCreatedAt));
        Page<HomeworkVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(toHomeworkVOList(page.getRecords(), null));
        return result;
    }

    @Override
    public HomeworkVO detail(Long userId, String role, Long id) {
        Homework homework = requireHomework(id);
        if ("TEACHER".equals(role) && !Objects.equals(homework.getTeacherId(), userId)) {
            throw new BusinessException(403, "只能查看自己的课程作业");
        }
        return toHomeworkVO(homework, "STUDENT".equals(role) ? userId : null);
    }

    @Override
    public Homework create(Long teacherId, HomeworkRequest request) {
        requireTeacherCourse(teacherId, request.getCourseId());
        Homework homework = new Homework();
        homework.setCourseId(request.getCourseId());
        homework.setTeacherId(teacherId);
        homework.setTitle(request.getTitle());
        homework.setDescription(request.getDescription());
        homework.setDeadline(request.getDeadline());
        homeworkMapper.insert(homework);
        return homework;
    }

    @Override
    public Homework update(Long teacherId, Long id, HomeworkRequest request) {
        Homework homework = requireTeacherHomework(teacherId, id);
        requireTeacherCourse(teacherId, request.getCourseId());
        homework.setCourseId(request.getCourseId());
        homework.setTitle(request.getTitle());
        homework.setDescription(request.getDescription());
        homework.setDeadline(request.getDeadline());
        homeworkMapper.updateById(homework);
        return homework;
    }

    @Override
    @Transactional
    public void delete(Long teacherId, Long id) {
        requireTeacherHomework(teacherId, id);
        submissionMapper.delete(new LambdaQueryWrapper<HomeworkSubmission>().eq(HomeworkSubmission::getHomeworkId, id));
        homeworkMapper.deleteById(id);
    }

    @Override
    @Transactional
    public HomeworkSubmissionVO submit(Long studentId, Long homeworkId, String content, MultipartFile file) {
        Homework homework = requireHomework(homeworkId);
        if (!StringUtils.hasText(content) && (file == null || file.isEmpty())) {
            throw new BusinessException(400, "提交内容和附件不能同时为空");
        }
        HomeworkSubmission submission = submissionMapper.selectOne(new LambdaQueryWrapper<HomeworkSubmission>()
                .eq(HomeworkSubmission::getHomeworkId, homeworkId)
                .eq(HomeworkSubmission::getStudentId, studentId));
        if (submission == null) {
            submission = new HomeworkSubmission();
            submission.setHomeworkId(homeworkId);
            submission.setStudentId(studentId);
        }
        submission.setContent(content);
        if (file != null && !file.isEmpty()) {
            submission.setAttachmentPath(saveAttachment(file));
        }
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setScore(null);
        submission.setComment(null);
        submission.setReviewedAt(null);
        if (submission.getId() == null) {
            submissionMapper.insert(submission);
        } else {
            submissionMapper.updateById(submission);
        }
        return toSubmissionVO(submission, homework);
    }

    @Override
    public Page<HomeworkSubmissionVO> pageSubmissions(Long teacherId, Long homeworkId, long current, long size) {
        Homework homework = requireTeacherHomework(teacherId, homeworkId);
        Page<HomeworkSubmission> page = submissionMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<HomeworkSubmission>()
                .eq(HomeworkSubmission::getHomeworkId, homeworkId)
                .orderByDesc(HomeworkSubmission::getSubmittedAt));
        Page<HomeworkSubmissionVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(submission -> toSubmissionVO(submission, homework)).toList());
        return result;
    }

    @Override
    public HomeworkSubmissionVO grade(Long teacherId, Long homeworkId, Long submissionId, HomeworkGradeRequest request) {
        Homework homework = requireTeacherHomework(teacherId, homeworkId);
        HomeworkSubmission submission = submissionMapper.selectOne(new LambdaQueryWrapper<HomeworkSubmission>()
                .eq(HomeworkSubmission::getId, submissionId)
                .eq(HomeworkSubmission::getHomeworkId, homeworkId));
        if (submission == null) {
            throw new BusinessException(404, "提交记录不存在");
        }
        submission.setScore(request.getScore());
        submission.setComment(request.getComment());
        submission.setReviewedAt(LocalDateTime.now());
        submissionMapper.updateById(submission);
        return toSubmissionVO(submission, homework);
    }

    @Override
    public Page<HomeworkSubmissionVO> pageMySubmissions(Long studentId, long current, long size) {
        Page<HomeworkSubmission> page = submissionMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<HomeworkSubmission>()
                .eq(HomeworkSubmission::getStudentId, studentId)
                .orderByDesc(HomeworkSubmission::getSubmittedAt));
        Page<HomeworkSubmissionVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(submission -> toSubmissionVO(submission, requireHomework(submission.getHomeworkId()))).toList());
        return result;
    }

    private String saveAttachment(MultipartFile file) {
        if (file.getSize() > homeworkMaxSize) {
            throw new BusinessException(400, "附件大小超出限制");
        }
        String contentType = file.getContentType();
        if (!ATTACHMENT_TYPES.contains(contentType)) {
            throw new BusinessException(400, "不支持的附件格式");
        }
        try {
            String original = file.getOriginalFilename();
            String ext = original != null && original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
            String datePath = LocalDate.now().toString().replace("-", "/");
            String fileName = UUID.randomUUID() + ext;
            Path dir = Path.of(homeworkPath, datePath);
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(fileName));
            return datePath + "/" + fileName;
        } catch (IOException e) {
            throw new BusinessException(500, "附件上传失败");
        }
    }

    private Homework requireHomework(Long id) {
        Homework homework = homeworkMapper.selectById(id);
        if (homework == null) {
            throw new BusinessException(404, "作业不存在");
        }
        return homework;
    }

    private Homework requireTeacherHomework(Long teacherId, Long id) {
        Homework homework = requireHomework(id);
        if (!Objects.equals(homework.getTeacherId(), teacherId)) {
            throw new BusinessException(403, "只能操作自己的课程作业");
        }
        return homework;
    }

    private Course requireTeacherCourse(Long teacherId, Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(404, "课程不存在");
        }
        if (!Objects.equals(course.getTeacherId(), teacherId)) {
            throw new BusinessException(403, "只能为自己的课程管理作业");
        }
        return course;
    }

    private List<HomeworkVO> toHomeworkVOList(List<Homework> homeworks, Long studentId) {
        if (homeworks.isEmpty()) {
            return List.of();
        }
        Set<Long> courseIds = homeworks.stream().map(Homework::getCourseId).collect(Collectors.toSet());
        Set<Long> teacherIds = homeworks.stream().map(Homework::getTeacherId).collect(Collectors.toSet());
        Map<Long, Course> courses = courseMapper.selectBatchIds(courseIds).stream().collect(Collectors.toMap(Course::getId, Function.identity()));
        Map<Long, User> teachers = userMapper.selectBatchIds(teacherIds).stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return homeworks.stream().map(homework -> fillHomeworkVO(homework, courses.get(homework.getCourseId()), teachers.get(homework.getTeacherId()), studentId)).toList();
    }

    private HomeworkVO toHomeworkVO(Homework homework, Long studentId) {
        return fillHomeworkVO(homework, courseMapper.selectById(homework.getCourseId()), userMapper.selectById(homework.getTeacherId()), studentId);
    }

    private HomeworkVO fillHomeworkVO(Homework homework, Course course, User teacher, Long studentId) {
        HomeworkVO vo = new HomeworkVO();
        vo.setId(homework.getId());
        vo.setCourseId(homework.getCourseId());
        vo.setCourseTitle(course == null ? null : course.getTitle());
        vo.setTeacherId(homework.getTeacherId());
        vo.setTeacherName(teacher == null ? null : teacher.getUsername());
        vo.setTitle(homework.getTitle());
        vo.setDescription(homework.getDescription());
        vo.setDeadline(homework.getDeadline());
        vo.setCreatedAt(homework.getCreatedAt());
        if (studentId != null) {
            HomeworkSubmission submission = submissionMapper.selectOne(new LambdaQueryWrapper<HomeworkSubmission>()
                    .eq(HomeworkSubmission::getHomeworkId, homework.getId())
                    .eq(HomeworkSubmission::getStudentId, studentId));
            if (submission != null) {
                vo.setSubmissionId(submission.getId());
                vo.setSubmittedAt(submission.getSubmittedAt());
                vo.setReviewedAt(submission.getReviewedAt());
                vo.setSubmissionStatus(submission.getReviewedAt() == null ? "SUBMITTED" : "REVIEWED");
            } else {
                vo.setSubmissionStatus("PENDING");
            }
        }
        return vo;
    }

    private HomeworkSubmissionVO toSubmissionVO(HomeworkSubmission submission, Homework homework) {
        Course course = courseMapper.selectById(homework.getCourseId());
        User student = userMapper.selectById(submission.getStudentId());
        HomeworkSubmissionVO vo = new HomeworkSubmissionVO();
        vo.setId(submission.getId());
        vo.setHomeworkId(submission.getHomeworkId());
        vo.setHomeworkTitle(homework.getTitle());
        vo.setCourseId(homework.getCourseId());
        vo.setCourseTitle(course == null ? null : course.getTitle());
        vo.setStudentId(submission.getStudentId());
        vo.setStudentName(student == null ? null : student.getUsername());
        vo.setContent(submission.getContent());
        vo.setAttachmentPath(submission.getAttachmentPath());
        vo.setAttachmentUrl(StringUtils.hasText(submission.getAttachmentPath()) ? "/uploads/homework/" + submission.getAttachmentPath() : null);
        vo.setScore(submission.getScore());
        vo.setComment(submission.getComment());
        vo.setSubmittedAt(submission.getSubmittedAt());
        vo.setReviewedAt(submission.getReviewedAt());
        return vo;
    }
}
