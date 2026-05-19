package com.edu.live.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.live.common.BusinessException;
import com.edu.live.dto.ExamGradeRequest;
import com.edu.live.dto.ExamQuestionRequest;
import com.edu.live.dto.ExamRequest;
import com.edu.live.dto.ExamSubmitRequest;
import com.edu.live.entity.Course;
import com.edu.live.entity.Exam;
import com.edu.live.entity.ExamAnswer;
import com.edu.live.entity.ExamQuestion;
import com.edu.live.entity.ExamSubmission;
import com.edu.live.entity.User;
import com.edu.live.enums.ExamQuestionType;
import com.edu.live.enums.ExamStatus;
import com.edu.live.mapper.CourseMapper;
import com.edu.live.mapper.ExamAnswerMapper;
import com.edu.live.mapper.ExamMapper;
import com.edu.live.mapper.ExamQuestionMapper;
import com.edu.live.mapper.ExamSubmissionMapper;
import com.edu.live.mapper.UserMapper;
import com.edu.live.service.ExamService;
import com.edu.live.vo.ExamAnswerVO;
import com.edu.live.vo.ExamQuestionVO;
import com.edu.live.vo.ExamSubmissionVO;
import com.edu.live.vo.ExamVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {
    private final ExamMapper examMapper;
    private final ExamQuestionMapper questionMapper;
    private final ExamSubmissionMapper submissionMapper;
    private final ExamAnswerMapper answerMapper;
    private final CourseMapper courseMapper;
    private final UserMapper userMapper;

    @Override
    public Page<ExamVO> pageStudent(Long studentId, long current, long size) {
        Page<Exam> page = examMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<Exam>()
                .eq(Exam::getStatus, ExamStatus.PUBLISHED.name())
                .orderByDesc(Exam::getCreatedAt));
        Page<ExamVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(toExamVOList(page.getRecords(), studentId));
        return result;
    }

    @Override
    public Page<ExamVO> pageTeacher(Long teacherId, long current, long size, Long courseId) {
        Page<Exam> page = examMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<Exam>()
                .eq(Exam::getTeacherId, teacherId)
                .eq(courseId != null, Exam::getCourseId, courseId)
                .orderByDesc(Exam::getCreatedAt));
        Page<ExamVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(toExamVOList(page.getRecords(), null));
        return result;
    }

    @Override
    public ExamVO detail(Long userId, String role, Long id) {
        Exam exam = requireExam(id);
        if ("TEACHER".equals(role) && !Objects.equals(exam.getTeacherId(), userId)) {
            throw new BusinessException(403, "只能查看自己的课程考试");
        }
        if ("STUDENT".equals(role) && !ExamStatus.PUBLISHED.name().equals(exam.getStatus())) {
            ExamSubmission submission = findSubmission(id, userId);
            if (submission == null) {
                throw new BusinessException(403, "考试暂不可访问");
            }
        }
        return toExamVO(exam, "STUDENT".equals(role) ? userId : null);
    }

    @Override
    public Exam create(Long teacherId, ExamRequest request) {
        requireTeacherCourse(teacherId, request.getCourseId());
        Exam exam = new Exam();
        exam.setCourseId(request.getCourseId());
        exam.setTeacherId(teacherId);
        exam.setTitle(request.getTitle());
        exam.setDescription(request.getDescription());
        exam.setStartTime(request.getStartTime());
        exam.setEndTime(request.getEndTime());
        exam.setDurationMinutes(request.getDurationMinutes());
        exam.setTotalScore(BigDecimal.ZERO);
        exam.setStatus(ExamStatus.DRAFT.name());
        examMapper.insert(exam);
        return exam;
    }

    @Override
    public Exam update(Long teacherId, Long id, ExamRequest request) {
        Exam exam = requireTeacherExam(teacherId, id);
        requireTeacherCourse(teacherId, request.getCourseId());
        exam.setCourseId(request.getCourseId());
        exam.setTitle(request.getTitle());
        exam.setDescription(request.getDescription());
        exam.setStartTime(request.getStartTime());
        exam.setEndTime(request.getEndTime());
        exam.setDurationMinutes(request.getDurationMinutes());
        examMapper.updateById(exam);
        return exam;
    }

    @Override
    @Transactional
    public void delete(Long teacherId, Long id) {
        requireTeacherExam(teacherId, id);
        List<ExamSubmission> submissions = submissionMapper.selectList(new LambdaQueryWrapper<ExamSubmission>().eq(ExamSubmission::getExamId, id));
        for (ExamSubmission submission : submissions) {
            answerMapper.delete(new LambdaQueryWrapper<ExamAnswer>().eq(ExamAnswer::getSubmissionId, submission.getId()));
        }
        submissionMapper.delete(new LambdaQueryWrapper<ExamSubmission>().eq(ExamSubmission::getExamId, id));
        questionMapper.delete(new LambdaQueryWrapper<ExamQuestion>().eq(ExamQuestion::getExamId, id));
        examMapper.deleteById(id);
    }

    @Override
    public Exam publish(Long teacherId, Long id) {
        Exam exam = requireTeacherExam(teacherId, id);
        if (questionMapper.selectCount(new LambdaQueryWrapper<ExamQuestion>().eq(ExamQuestion::getExamId, id)) == 0) {
            throw new BusinessException(400, "请先添加题目");
        }
        exam.setStatus(ExamStatus.PUBLISHED.name());
        examMapper.updateById(exam);
        return exam;
    }

    @Override
    public Exam close(Long teacherId, Long id) {
        Exam exam = requireTeacherExam(teacherId, id);
        exam.setStatus(ExamStatus.CLOSED.name());
        examMapper.updateById(exam);
        return exam;
    }

    @Override
    public List<ExamQuestionVO> questions(Long userId, String role, Long examId) {
        Exam exam = requireExam(examId);
        if ("TEACHER".equals(role)) {
            requireTeacherExam(userId, examId);
            return questionList(examId).stream().map(question -> toQuestionVO(question, true)).toList();
        }
        if (!ExamStatus.PUBLISHED.name().equals(exam.getStatus())) {
            throw new BusinessException(403, "考试暂不可答题");
        }
        return questionList(examId).stream().map(question -> toQuestionVO(question, false)).toList();
    }

    @Override
    public ExamQuestion createQuestion(Long teacherId, Long examId, ExamQuestionRequest request) {
        requireTeacherExam(teacherId, examId);
        ExamQuestion question = new ExamQuestion();
        fillQuestion(question, examId, request);
        questionMapper.insert(question);
        refreshTotalScore(examId);
        return question;
    }

    @Override
    public ExamQuestion updateQuestion(Long teacherId, Long examId, Long questionId, ExamQuestionRequest request) {
        requireTeacherExam(teacherId, examId);
        ExamQuestion question = requireQuestion(examId, questionId);
        fillQuestion(question, examId, request);
        questionMapper.updateById(question);
        refreshTotalScore(examId);
        return question;
    }

    @Override
    public void deleteQuestion(Long teacherId, Long examId, Long questionId) {
        requireTeacherExam(teacherId, examId);
        requireQuestion(examId, questionId);
        questionMapper.deleteById(questionId);
        refreshTotalScore(examId);
    }

    @Override
    @Transactional
    public ExamSubmissionVO submit(Long studentId, Long examId, ExamSubmitRequest request) {
        Exam exam = requireExam(examId);
        if (!ExamStatus.PUBLISHED.name().equals(exam.getStatus())) {
            throw new BusinessException(400, "考试未发布或已关闭");
        }
        LocalDateTime now = LocalDateTime.now();
        if (exam.getStartTime() != null && now.isBefore(exam.getStartTime())) {
            throw new BusinessException(400, "考试尚未开始");
        }
        if (exam.getEndTime() != null && now.isAfter(exam.getEndTime())) {
            throw new BusinessException(400, "考试已结束");
        }
        if (findSubmission(examId, studentId) != null) {
            throw new BusinessException(400, "不能重复提交考试");
        }
        List<ExamQuestion> questions = questionList(examId);
        if (questions.isEmpty()) {
            throw new BusinessException(400, "考试暂无题目");
        }
        Map<Long, String> answerMap = request.getAnswers().stream().collect(Collectors.toMap(ExamSubmitRequest.AnswerItem::getQuestionId, item -> item.getAnswer() == null ? "" : item.getAnswer(), (a, b) -> b));
        ExamSubmission submission = new ExamSubmission();
        submission.setExamId(examId);
        submission.setStudentId(studentId);
        submission.setObjectiveScore(BigDecimal.ZERO);
        submission.setSubjectiveScore(BigDecimal.ZERO);
        submission.setTotalScore(BigDecimal.ZERO);
        submission.setStatus("SUBMITTED");
        submission.setSubmittedAt(now);
        submissionMapper.insert(submission);
        BigDecimal objectiveScore = BigDecimal.ZERO;
        boolean hasShort = false;
        for (ExamQuestion question : questions) {
            String answerText = answerMap.getOrDefault(question.getId(), "");
            ExamAnswer answer = new ExamAnswer();
            answer.setSubmissionId(submission.getId());
            answer.setQuestionId(question.getId());
            answer.setAnswer(answerText);
            if (ExamQuestionType.SHORT.name().equals(question.getType())) {
                hasShort = true;
                answer.setCorrect(null);
                answer.setScore(BigDecimal.ZERO);
            } else {
                boolean correct = normalizeAnswer(question.getAnswer()).equals(normalizeAnswer(answerText));
                answer.setCorrect(correct);
                answer.setScore(correct ? question.getScore() : BigDecimal.ZERO);
                objectiveScore = objectiveScore.add(answer.getScore());
            }
            answerMapper.insert(answer);
        }
        submission.setObjectiveScore(objectiveScore);
        submission.setTotalScore(objectiveScore);
        submission.setStatus(hasShort ? "PENDING_REVIEW" : "REVIEWED");
        if (!hasShort) {
            submission.setReviewedAt(now);
        }
        submissionMapper.updateById(submission);
        return toSubmissionVO(submission, true);
    }

    @Override
    public Page<ExamSubmissionVO> pageSubmissions(Long teacherId, Long examId, long current, long size) {
        requireTeacherExam(teacherId, examId);
        Page<ExamSubmission> page = submissionMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<ExamSubmission>()
                .eq(ExamSubmission::getExamId, examId)
                .orderByDesc(ExamSubmission::getSubmittedAt));
        Page<ExamSubmissionVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(submission -> toSubmissionVO(submission, false)).toList());
        return result;
    }

    @Override
    public ExamSubmissionVO submissionDetail(Long userId, String role, Long examId, Long submissionId) {
        Exam exam = requireExam(examId);
        ExamSubmission submission = requireSubmission(examId, submissionId);
        if ("TEACHER".equals(role)) {
            requireTeacherExam(userId, examId);
        } else if (!Objects.equals(submission.getStudentId(), userId)) {
            throw new BusinessException(403, "只能查看自己的答卷");
        } else if (!ExamStatus.PUBLISHED.name().equals(exam.getStatus()) && !ExamStatus.CLOSED.name().equals(exam.getStatus())) {
            throw new BusinessException(403, "考试暂不可查看");
        }
        return toSubmissionVO(submission, true);
    }

    @Override
    public ExamSubmissionVO myResult(Long studentId, Long examId) {
        ExamSubmission submission = findSubmission(examId, studentId);
        if (submission == null) {
            throw new BusinessException(404, "答卷不存在");
        }
        return toSubmissionVO(submission, true);
    }

    @Override
    public Page<ExamSubmissionVO> pageMySubmissions(Long studentId, long current, long size) {
        Page<ExamSubmission> page = submissionMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<ExamSubmission>()
                .eq(ExamSubmission::getStudentId, studentId)
                .orderByDesc(ExamSubmission::getSubmittedAt));
        Page<ExamSubmissionVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(submission -> toSubmissionVO(submission, false)).toList());
        return result;
    }

    @Override
    @Transactional
    public ExamSubmissionVO grade(Long teacherId, Long examId, Long submissionId, ExamGradeRequest request) {
        requireTeacherExam(teacherId, examId);
        ExamSubmission submission = requireSubmission(examId, submissionId);
        BigDecimal subjectiveScore = BigDecimal.ZERO;
        if (request.getAnswers() != null) {
            Map<Long, ExamAnswer> answers = answerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>().eq(ExamAnswer::getSubmissionId, submissionId))
                    .stream().collect(Collectors.toMap(ExamAnswer::getId, Function.identity()));
            Map<Long, ExamQuestion> questions = questionList(examId).stream().collect(Collectors.toMap(ExamQuestion::getId, Function.identity()));
            for (ExamGradeRequest.AnswerGradeItem item : request.getAnswers()) {
                ExamAnswer answer = answers.get(item.getAnswerId());
                if (answer == null) {
                    throw new BusinessException(404, "答题记录不存在");
                }
                ExamQuestion question = questions.get(answer.getQuestionId());
                if (question == null || !ExamQuestionType.SHORT.name().equals(question.getType())) {
                    continue;
                }
                if (item.getScore().compareTo(question.getScore()) > 0) {
                    throw new BusinessException(400, "评分不能超过题目分值");
                }
                answer.setScore(item.getScore());
                answer.setComment(item.getComment());
                answerMapper.updateById(answer);
            }
        }
        List<ExamAnswer> latestAnswers = answerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>().eq(ExamAnswer::getSubmissionId, submissionId));
        Map<Long, ExamQuestion> questions = questionList(examId).stream().collect(Collectors.toMap(ExamQuestion::getId, Function.identity()));
        for (ExamAnswer answer : latestAnswers) {
            ExamQuestion question = questions.get(answer.getQuestionId());
            if (question != null && ExamQuestionType.SHORT.name().equals(question.getType())) {
                subjectiveScore = subjectiveScore.add(answer.getScore() == null ? BigDecimal.ZERO : answer.getScore());
            }
        }
        submission.setSubjectiveScore(subjectiveScore);
        submission.setTotalScore((submission.getObjectiveScore() == null ? BigDecimal.ZERO : submission.getObjectiveScore()).add(subjectiveScore));
        submission.setComment(request.getComment());
        submission.setStatus("REVIEWED");
        submission.setReviewedAt(LocalDateTime.now());
        submissionMapper.updateById(submission);
        return toSubmissionVO(submission, true);
    }

    private void fillQuestion(ExamQuestion question, Long examId, ExamQuestionRequest request) {
        ExamQuestionType type = parseQuestionType(request.getType());
        if (!ExamQuestionType.SHORT.equals(type) && (request.getOptions() == null || request.getOptions().isEmpty())) {
            throw new BusinessException(400, "客观题选项不能为空");
        }
        question.setExamId(examId);
        question.setType(type.name());
        question.setContent(request.getContent());
        question.setOptions(request.getOptions() == null ? null : JSONUtil.toJsonStr(request.getOptions()));
        question.setAnswer(normalizeAnswer(request.getAnswer()));
        question.setScore(request.getScore());
        question.setSort(request.getSort() == null ? 0 : request.getSort());
    }

    private ExamQuestionType parseQuestionType(String type) {
        try {
            return ExamQuestionType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(400, "不支持的题型");
        }
    }

    private String normalizeAnswer(String answer) {
        if (!StringUtils.hasText(answer)) {
            return "";
        }
        return Arrays.stream(answer.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .sorted()
                .collect(Collectors.joining(","));
    }

    private void refreshTotalScore(Long examId) {
        BigDecimal total = questionList(examId).stream().map(ExamQuestion::getScore).reduce(BigDecimal.ZERO, BigDecimal::add);
        Exam exam = requireExam(examId);
        exam.setTotalScore(total);
        examMapper.updateById(exam);
    }

    private Exam requireExam(Long id) {
        Exam exam = examMapper.selectById(id);
        if (exam == null) {
            throw new BusinessException(404, "考试不存在");
        }
        return exam;
    }

    private Exam requireTeacherExam(Long teacherId, Long id) {
        Exam exam = requireExam(id);
        if (!Objects.equals(exam.getTeacherId(), teacherId)) {
            throw new BusinessException(403, "只能操作自己的课程考试");
        }
        return exam;
    }

    private ExamQuestion requireQuestion(Long examId, Long questionId) {
        ExamQuestion question = questionMapper.selectOne(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getId, questionId)
                .eq(ExamQuestion::getExamId, examId));
        if (question == null) {
            throw new BusinessException(404, "题目不存在");
        }
        return question;
    }

    private ExamSubmission requireSubmission(Long examId, Long submissionId) {
        ExamSubmission submission = submissionMapper.selectOne(new LambdaQueryWrapper<ExamSubmission>()
                .eq(ExamSubmission::getId, submissionId)
                .eq(ExamSubmission::getExamId, examId));
        if (submission == null) {
            throw new BusinessException(404, "答卷不存在");
        }
        return submission;
    }

    private ExamSubmission findSubmission(Long examId, Long studentId) {
        return submissionMapper.selectOne(new LambdaQueryWrapper<ExamSubmission>()
                .eq(ExamSubmission::getExamId, examId)
                .eq(ExamSubmission::getStudentId, studentId));
    }

    private Course requireTeacherCourse(Long teacherId, Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(404, "课程不存在");
        }
        if (!Objects.equals(course.getTeacherId(), teacherId)) {
            throw new BusinessException(403, "只能为自己的课程管理考试");
        }
        return course;
    }

    private List<ExamQuestion> questionList(Long examId) {
        return questionMapper.selectList(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getExamId, examId)
                .orderByAsc(ExamQuestion::getSort)
                .orderByAsc(ExamQuestion::getId));
    }

    private List<ExamVO> toExamVOList(List<Exam> exams, Long studentId) {
        if (exams.isEmpty()) {
            return List.of();
        }
        Set<Long> courseIds = exams.stream().map(Exam::getCourseId).collect(Collectors.toSet());
        Set<Long> teacherIds = exams.stream().map(Exam::getTeacherId).collect(Collectors.toSet());
        Map<Long, Course> courses = courseMapper.selectBatchIds(courseIds).stream().collect(Collectors.toMap(Course::getId, Function.identity()));
        Map<Long, User> teachers = userMapper.selectBatchIds(teacherIds).stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return exams.stream().map(exam -> fillExamVO(exam, courses.get(exam.getCourseId()), teachers.get(exam.getTeacherId()), studentId)).toList();
    }

    private ExamVO toExamVO(Exam exam, Long studentId) {
        return fillExamVO(exam, courseMapper.selectById(exam.getCourseId()), userMapper.selectById(exam.getTeacherId()), studentId);
    }

    private ExamVO fillExamVO(Exam exam, Course course, User teacher, Long studentId) {
        ExamVO vo = new ExamVO();
        vo.setId(exam.getId());
        vo.setCourseId(exam.getCourseId());
        vo.setCourseTitle(course == null ? null : course.getTitle());
        vo.setTeacherId(exam.getTeacherId());
        vo.setTeacherName(teacher == null ? null : teacher.getUsername());
        vo.setTitle(exam.getTitle());
        vo.setDescription(exam.getDescription());
        vo.setStartTime(exam.getStartTime());
        vo.setEndTime(exam.getEndTime());
        vo.setDurationMinutes(exam.getDurationMinutes());
        vo.setTotalScore(exam.getTotalScore());
        vo.setStatus(exam.getStatus());
        vo.setCreatedAt(exam.getCreatedAt());
        if (studentId != null) {
            ExamSubmission submission = findSubmission(exam.getId(), studentId);
            if (submission != null) {
                vo.setSubmissionId(submission.getId());
                vo.setSubmissionStatus(submission.getStatus());
                vo.setScore(submission.getTotalScore());
                vo.setSubmittedAt(submission.getSubmittedAt());
                vo.setReviewedAt(submission.getReviewedAt());
            } else {
                vo.setSubmissionStatus("PENDING");
            }
        }
        return vo;
    }

    private ExamQuestionVO toQuestionVO(ExamQuestion question, boolean includeAnswer) {
        ExamQuestionVO vo = new ExamQuestionVO();
        vo.setId(question.getId());
        vo.setExamId(question.getExamId());
        vo.setType(question.getType());
        vo.setContent(question.getContent());
        vo.setOptions(parseOptions(question.getOptions()));
        vo.setAnswer(includeAnswer ? question.getAnswer() : null);
        vo.setScore(question.getScore());
        vo.setSort(question.getSort());
        vo.setCreatedAt(question.getCreatedAt());
        return vo;
    }

    private ExamSubmissionVO toSubmissionVO(ExamSubmission submission, boolean includeAnswers) {
        Exam exam = requireExam(submission.getExamId());
        Course course = courseMapper.selectById(exam.getCourseId());
        User student = userMapper.selectById(submission.getStudentId());
        ExamSubmissionVO vo = new ExamSubmissionVO();
        vo.setId(submission.getId());
        vo.setExamId(submission.getExamId());
        vo.setExamTitle(exam.getTitle());
        vo.setCourseId(exam.getCourseId());
        vo.setCourseTitle(course == null ? null : course.getTitle());
        vo.setStudentId(submission.getStudentId());
        vo.setStudentName(student == null ? null : student.getUsername());
        vo.setObjectiveScore(submission.getObjectiveScore());
        vo.setSubjectiveScore(submission.getSubjectiveScore());
        vo.setTotalScore(submission.getTotalScore());
        vo.setStatus(submission.getStatus());
        vo.setComment(submission.getComment());
        vo.setSubmittedAt(submission.getSubmittedAt());
        vo.setReviewedAt(submission.getReviewedAt());
        if (includeAnswers) {
            vo.setAnswers(toAnswerVOList(submission.getId(), exam.getId()));
        }
        return vo;
    }

    private List<ExamAnswerVO> toAnswerVOList(Long submissionId, Long examId) {
        List<ExamAnswer> answers = answerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>().eq(ExamAnswer::getSubmissionId, submissionId));
        Map<Long, ExamQuestion> questions = questionList(examId).stream().collect(Collectors.toMap(ExamQuestion::getId, Function.identity()));
        return answers.stream().sorted(Comparator.comparing(answer -> questions.get(answer.getQuestionId()).getSort())).map(answer -> {
            ExamQuestion question = questions.get(answer.getQuestionId());
            ExamAnswerVO vo = new ExamAnswerVO();
            vo.setId(answer.getId());
            vo.setSubmissionId(answer.getSubmissionId());
            vo.setQuestionId(answer.getQuestionId());
            vo.setQuestionType(question.getType());
            vo.setQuestionContent(question.getContent());
            vo.setOptions(parseOptions(question.getOptions()));
            vo.setCorrectAnswer(question.getAnswer());
            vo.setAnswer(answer.getAnswer());
            vo.setCorrect(answer.getCorrect());
            vo.setScore(answer.getScore());
            vo.setQuestionScore(question.getScore());
            vo.setComment(answer.getComment());
            return vo;
        }).toList();
    }

    private List<String> parseOptions(String options) {
        if (!StringUtils.hasText(options)) {
            return List.of();
        }
        return JSONUtil.toList(options, String.class);
    }
}
