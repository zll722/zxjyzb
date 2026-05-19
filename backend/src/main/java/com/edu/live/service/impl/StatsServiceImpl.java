package com.edu.live.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.live.common.BusinessException;
import com.edu.live.entity.Chapter;
import com.edu.live.entity.Course;
import com.edu.live.entity.CourseFavorite;
import com.edu.live.entity.Exam;
import com.edu.live.entity.ExamSubmission;
import com.edu.live.entity.Homework;
import com.edu.live.entity.HomeworkSubmission;
import com.edu.live.entity.LearningRecord;
import com.edu.live.entity.LiveReplay;
import com.edu.live.entity.LiveRoom;
import com.edu.live.entity.User;
import com.edu.live.mapper.ChapterMapper;
import com.edu.live.mapper.CourseFavoriteMapper;
import com.edu.live.mapper.CourseMapper;
import com.edu.live.mapper.ExamMapper;
import com.edu.live.mapper.ExamSubmissionMapper;
import com.edu.live.mapper.HomeworkMapper;
import com.edu.live.mapper.HomeworkSubmissionMapper;
import com.edu.live.mapper.LearningRecordMapper;
import com.edu.live.mapper.LiveReplayMapper;
import com.edu.live.mapper.LiveRoomMapper;
import com.edu.live.mapper.UserMapper;
import com.edu.live.service.StatsService;
import com.edu.live.vo.PlatformStatsVO;
import com.edu.live.vo.RecentLearningRecordVO;
import com.edu.live.vo.StudentStatsVO;
import com.edu.live.vo.TeacherCourseStatsVO;
import com.edu.live.vo.TeacherStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {
    private final UserMapper userMapper;
    private final CourseMapper courseMapper;
    private final CourseFavoriteMapper courseFavoriteMapper;
    private final LearningRecordMapper learningRecordMapper;
    private final ChapterMapper chapterMapper;
    private final LiveRoomMapper liveRoomMapper;
    private final LiveReplayMapper liveReplayMapper;
    private final HomeworkMapper homeworkMapper;
    private final HomeworkSubmissionMapper homeworkSubmissionMapper;
    private final ExamMapper examMapper;
    private final ExamSubmissionMapper examSubmissionMapper;

    @Override
    public StudentStatsVO studentStats(Long studentId) {
        StudentStatsVO vo = new StudentStatsVO();
        vo.setLearnedCourseCount(learningRecordMapper.countDistinctCoursesByStudent(studentId));
        vo.setFavoriteCount(courseFavoriteMapper.selectCount(new LambdaQueryWrapper<CourseFavorite>().eq(CourseFavorite::getUserId, studentId)));
        vo.setHomeworkSubmissionCount(homeworkSubmissionMapper.selectCount(new LambdaQueryWrapper<HomeworkSubmission>().eq(HomeworkSubmission::getStudentId, studentId)));
        vo.setExamParticipationCount(examSubmissionMapper.selectCount(new LambdaQueryWrapper<ExamSubmission>().eq(ExamSubmission::getStudentId, studentId)));
        vo.setAverageScore(examSubmissionMapper.averageScoreByStudent(studentId));
        vo.setRecentRecords(recentRecords(studentId));
        return vo;
    }

    @Override
    public TeacherStatsVO teacherStats(Long teacherId) {
        TeacherStatsVO vo = new TeacherStatsVO();
        vo.setCourseCount(courseMapper.selectCount(new LambdaQueryWrapper<Course>().eq(Course::getTeacherId, teacherId)));
        vo.setLiveCount(liveRoomMapper.selectCount(new LambdaQueryWrapper<LiveRoom>().eq(LiveRoom::getTeacherId, teacherId)));
        vo.setReplayCount(liveReplayMapper.selectCount(new LambdaQueryWrapper<LiveReplay>().eq(LiveReplay::getTeacherId, teacherId)));
        vo.setHomeworkCount(homeworkMapper.selectCount(new LambdaQueryWrapper<Homework>().eq(Homework::getTeacherId, teacherId)));
        vo.setExamCount(examMapper.selectCount(new LambdaQueryWrapper<Exam>().eq(Exam::getTeacherId, teacherId)));
        vo.setStudentSubmissionCount(homeworkSubmissionMapper.countByTeacher(teacherId) + examSubmissionMapper.countByTeacher(teacherId));
        vo.setPendingHomeworkCount(homeworkSubmissionMapper.countPendingByTeacher(teacherId));
        vo.setPendingExamCount(examSubmissionMapper.countPendingByTeacher(teacherId));
        return vo;
    }

    @Override
    public List<TeacherCourseStatsVO> teacherCourseStats(Long teacherId, Long courseId) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<Course>()
                .eq(Course::getTeacherId, teacherId)
                .eq(courseId != null, Course::getId, courseId)
                .orderByDesc(Course::getCreatedAt);
        List<Course> courses = courseMapper.selectList(wrapper);
        if (courseId != null && courses.isEmpty()) {
            throw new BusinessException(403, "只能查看自己的课程统计");
        }
        Long liveCount = liveRoomMapper.selectCount(new LambdaQueryWrapper<LiveRoom>().eq(LiveRoom::getTeacherId, teacherId));
        Long replayCount = liveReplayMapper.selectCount(new LambdaQueryWrapper<LiveReplay>().eq(LiveReplay::getTeacherId, teacherId));
        List<TeacherCourseStatsVO> result = new ArrayList<>();
        for (Course course : courses) {
            TeacherCourseStatsVO vo = new TeacherCourseStatsVO();
            Long students = learningRecordMapper.countDistinctStudentsByCourse(course.getId());
            Long homeworks = homeworkMapper.selectCount(new LambdaQueryWrapper<Homework>().eq(Homework::getCourseId, course.getId()));
            Long homeworkSubmissions = homeworkSubmissionMapper.countByCourse(course.getId());
            Long exams = examMapper.selectCount(new LambdaQueryWrapper<Exam>().eq(Exam::getCourseId, course.getId()));
            Long examSubmissions = examSubmissionMapper.countByCourse(course.getId());
            vo.setCourseId(course.getId());
            vo.setCourseTitle(course.getTitle());
            vo.setStudentCount(students);
            vo.setHomeworkCount(homeworks);
            vo.setHomeworkSubmissionCount(homeworkSubmissions);
            vo.setHomeworkSubmissionRate(rate(homeworkSubmissions, homeworks * students));
            vo.setExamCount(exams);
            vo.setExamSubmissionCount(examSubmissions);
            vo.setExamSubmissionRate(rate(examSubmissions, exams * students));
            vo.setExamAverageScore(examSubmissionMapper.averageScoreByCourse(course.getId()));
            vo.setLiveCount(liveCount);
            vo.setReplayCount(replayCount);
            result.add(vo);
        }
        return result;
    }

    @Override
    public PlatformStatsVO platformStats() {
        PlatformStatsVO vo = new PlatformStatsVO();
        vo.setUserCount(userMapper.selectCount(new LambdaQueryWrapper<User>()));
        vo.setTeacherCount(userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRole, "TEACHER")));
        vo.setStudentCount(userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRole, "STUDENT")));
        vo.setCourseCount(courseMapper.selectCount(new LambdaQueryWrapper<Course>()));
        vo.setLiveCount(liveRoomMapper.selectCount(new LambdaQueryWrapper<LiveRoom>()));
        vo.setReplayCount(liveReplayMapper.selectCount(new LambdaQueryWrapper<LiveReplay>()));
        vo.setHomeworkCount(homeworkMapper.selectCount(new LambdaQueryWrapper<Homework>()));
        vo.setExamCount(examMapper.selectCount(new LambdaQueryWrapper<Exam>()));
        return vo;
    }

    private List<RecentLearningRecordVO> recentRecords(Long studentId) {
        List<LearningRecord> records = learningRecordMapper.selectList(new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getUserId, studentId)
                .orderByDesc(LearningRecord::getLastWatchTime)
                .last("limit 8"));
        if (records.isEmpty()) {
            return List.of();
        }
        Map<Long, Course> courses = courseMapper.selectBatchIds(records.stream().map(LearningRecord::getCourseId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Course::getId, Function.identity()));
        Map<Long, Chapter> chapters = chapterMapper.selectBatchIds(records.stream().map(LearningRecord::getChapterId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Chapter::getId, Function.identity()));
        return records.stream().map(record -> {
            RecentLearningRecordVO vo = new RecentLearningRecordVO();
            vo.setCourseId(record.getCourseId());
            vo.setCourseTitle(courses.get(record.getCourseId()) == null ? null : courses.get(record.getCourseId()).getTitle());
            vo.setChapterId(record.getChapterId());
            vo.setChapterTitle(chapters.get(record.getChapterId()) == null ? null : chapters.get(record.getChapterId()).getTitle());
            vo.setProgress(record.getProgress());
            vo.setLastWatchTime(record.getLastWatchTime());
            return vo;
        }).toList();
    }

    private BigDecimal rate(Long numerator, Long denominator) {
        if (denominator == null || denominator == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }
}
