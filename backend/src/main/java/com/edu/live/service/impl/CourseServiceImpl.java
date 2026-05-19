package com.edu.live.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.live.common.BusinessException;
import com.edu.live.dto.ChapterRequest;
import com.edu.live.dto.CourseRequest;
import com.edu.live.dto.LearningRecordRequest;
import com.edu.live.entity.Category;
import com.edu.live.entity.Chapter;
import com.edu.live.entity.Course;
import com.edu.live.entity.CourseFavorite;
import com.edu.live.entity.LearningRecord;
import com.edu.live.entity.User;
import com.edu.live.enums.CourseStatus;
import com.edu.live.enums.UserRole;
import com.edu.live.mapper.CategoryMapper;
import com.edu.live.mapper.ChapterMapper;
import com.edu.live.mapper.CourseFavoriteMapper;
import com.edu.live.mapper.CourseMapper;
import com.edu.live.mapper.LearningRecordMapper;
import com.edu.live.mapper.UserMapper;
import com.edu.live.service.CourseOrderService;
import com.edu.live.service.CourseService;
import com.edu.live.vo.CourseVO;
import com.edu.live.vo.RecentLearningRecordVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {
    private static final Set<String> VIDEO_TYPES = Set.of("video/mp4", "video/webm", "video/ogg", "video/quicktime", "video/x-msvideo");

    private final CourseMapper courseMapper;
    private final CategoryMapper categoryMapper;
    private final ChapterMapper chapterMapper;
    private final CourseFavoriteMapper favoriteMapper;
    private final LearningRecordMapper learningRecordMapper;
    private final UserMapper userMapper;
    private final CourseOrderService courseOrderService;

    @Value("${file.video-path}")
    private String videoPath;

    @Override
    public Page<CourseVO> pageCourses(long current, long size, Long categoryId, String keyword, String status, Long teacherId, Long userId) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<Course>()
                .eq(categoryId != null, Course::getCategoryId, categoryId)
                .eq(StringUtils.hasText(status), Course::getStatus, status)
                .eq(teacherId != null, Course::getTeacherId, teacherId)
                .like(StringUtils.hasText(keyword), Course::getTitle, keyword)
                .orderByDesc(Course::getCreatedAt);
        Page<Course> page = courseMapper.selectPage(new Page<>(current, size), wrapper);
        Page<CourseVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(toVOList(page.getRecords(), userId, false));
        return result;
    }

    @Override
    public CourseVO detail(Long id, Long userId) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException(404, "课程不存在");
        }
        CourseVO vo = toVO(course, userId, true);
        return vo;
    }

    @Override
    public Course create(Long teacherId, CourseRequest request) {
        Course course = new Course();
        course.setTitle(request.getTitle());
        course.setCover(request.getCover());
        course.setIntro(request.getIntro());
        course.setCategoryId(request.getCategoryId());
        course.setTeacherId(teacherId);
        course.setPrice(request.getPrice());
        course.setStatus(CourseStatus.DRAFT.name());
        course.setFavoritesCount(0);
        courseMapper.insert(course);
        return course;
    }

    @Override
    public Course update(Long teacherId, Long id, CourseRequest request) {
        Course course = requireTeacherCourse(teacherId, id);
        if (!CourseStatus.DRAFT.name().equals(course.getStatus())) {
            throw new BusinessException(400, "仅草稿课程可编辑");
        }
        course.setTitle(request.getTitle());
        course.setCover(request.getCover());
        course.setIntro(request.getIntro());
        course.setCategoryId(request.getCategoryId());
        course.setPrice(request.getPrice());
        courseMapper.updateById(course);
        return course;
    }

    @Override
    public void delete(Long teacherId, Long id) {
        Course course = requireTeacherCourse(teacherId, id);
        if (!CourseStatus.DRAFT.name().equals(course.getStatus())) {
            throw new BusinessException(400, "仅草稿课程可删除");
        }
        courseMapper.deleteById(id);
    }

    @Override
    public void submitReview(Long teacherId, Long id) {
        Course course = requireTeacherCourse(teacherId, id);
        if (!CourseStatus.DRAFT.name().equals(course.getStatus())) {
            throw new BusinessException(400, "仅草稿课程可提交审核");
        }
        course.setStatus(CourseStatus.REVIEWING.name());
        courseMapper.updateById(course);
    }

    @Override
    public void review(Long id, boolean approved) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException(404, "课程不存在");
        }
        course.setStatus(approved ? CourseStatus.PUBLISHED.name() : CourseStatus.OFFLINE.name());
        courseMapper.updateById(course);
    }

    @Override
    public Chapter createChapter(Long teacherId, Long courseId, ChapterRequest request) {
        requireTeacherCourse(teacherId, courseId);
        Chapter chapter = new Chapter();
        chapter.setCourseId(courseId);
        chapter.setTitle(request.getTitle());
        chapter.setVideoPath(request.getVideoPath());
        chapter.setDuration(request.getDuration());
        chapter.setSort(request.getSort());
        chapterMapper.insert(chapter);
        return chapter;
    }


    @Override
    public Chapter uploadChapterVideo(Long teacherId, Long courseId, Long chapterId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "????????");
        }
        if (!VIDEO_TYPES.contains(file.getContentType())) {
            throw new BusinessException(400, "????? MP4?WEBM?OGG?MOV?AVI ??");
        }
        requireTeacherCourse(teacherId, courseId);
        Chapter chapter = requireCourseChapter(courseId, chapterId);
        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.')) : ".mp4";
        String datePath = LocalDate.now().toString().replace("-", "/");
        String fileName = UUID.randomUUID() + ext;
        Path dir = Path.of(videoPath, datePath);
        try {
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(fileName));
        } catch (IOException e) {
            throw new BusinessException("????????");
        }
        chapter.setVideoPath(datePath + "/" + fileName);
        chapterMapper.updateById(chapter);
        return chapter;
    }

    @Override
    public ResponseEntity<Resource> streamChapterVideo(Long courseId, Long chapterId, Long userId, HttpServletRequest request) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(404, "?????");
        }
        if (!isFree(course) && (userId == null || !courseOrderService.hasCourseAccess(userId, courseId))) {
            throw new BusinessException(403, "???????");
        }
        Chapter chapter = requireCourseChapter(courseId, chapterId);
        if (!StringUtils.hasText(chapter.getVideoPath())) {
            throw new BusinessException(404, "???????");
        }
        Path file = Path.of(videoPath, chapter.getVideoPath());
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            throw new BusinessException(404, "???????");
        }
        try {
            long fileSize = Files.size(file);
            String range = request.getHeader(HttpHeaders.RANGE);
            long start = 0;
            long end = fileSize - 1;
            HttpStatus status = HttpStatus.OK;
            if (StringUtils.hasText(range) && range.startsWith("bytes=")) {
                String[] ranges = range.substring(6).split("-", 2);
                start = Long.parseLong(ranges[0]);
                if (ranges.length > 1 && StringUtils.hasText(ranges[1])) {
                    end = Long.parseLong(ranges[1]);
                }
                end = Math.min(end, fileSize - 1);
                status = HttpStatus.PARTIAL_CONTENT;
            }
            if (start > end || start < 0) {
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                        .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize)
                        .build();
            }
            Resource resource = status == HttpStatus.PARTIAL_CONTENT
                    ? new InputStreamResource(openRangeStream(file, start))
                    : new FileSystemResource(file);
            String fileName = URLEncoder.encode(file.getFileName().toString(), StandardCharsets.UTF_8);
            ResponseEntity.BodyBuilder builder = ResponseEntity.status(status)
                    .contentType(MediaType.parseMediaType(resolveContentType(file)))
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + fileName)
                    .contentLength(end - start + 1);
            if (status == HttpStatus.PARTIAL_CONTENT) {
                builder.header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileSize);
            }
            return builder.body(resource);
        } catch (IOException | NumberFormatException e) {
            throw new BusinessException("????????");
        }
    }

    @Override
    public void deleteChapter(Long teacherId, Long courseId, Long chapterId) {
        requireTeacherCourse(teacherId, courseId);
        chapterMapper.delete(new LambdaQueryWrapper<Chapter>().eq(Chapter::getId, chapterId).eq(Chapter::getCourseId, courseId));
    }

    @Override
    @Transactional
    public void favorite(Long userId, Long courseId) {
        boolean exists = favoriteMapper.exists(new LambdaQueryWrapper<CourseFavorite>()
                .eq(CourseFavorite::getUserId, userId)
                .eq(CourseFavorite::getCourseId, courseId));
        if (!exists) {
            CourseFavorite favorite = new CourseFavorite();
            favorite.setUserId(userId);
            favorite.setCourseId(courseId);
            favoriteMapper.insert(favorite);
            Course course = courseMapper.selectById(courseId);
            course.setFavoritesCount(course.getFavoritesCount() + 1);
            courseMapper.updateById(course);
        }
    }

    @Override
    @Transactional
    public void unfavorite(Long userId, Long courseId) {
        int deleted = favoriteMapper.delete(new LambdaQueryWrapper<CourseFavorite>()
                .eq(CourseFavorite::getUserId, userId)
                .eq(CourseFavorite::getCourseId, courseId));
        if (deleted > 0) {
            Course course = courseMapper.selectById(courseId);
            course.setFavoritesCount(Math.max(0, course.getFavoritesCount() - 1));
            courseMapper.updateById(course);
        }
    }

    @Override
    public Page<CourseVO> pageFavoriteCourses(Long userId, long current, long size) {
        Page<CourseFavorite> favoritePage = favoriteMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<CourseFavorite>()
                .eq(CourseFavorite::getUserId, userId)
                .orderByDesc(CourseFavorite::getCreatedAt));
        Page<CourseVO> result = new Page<>(favoritePage.getCurrent(), favoritePage.getSize(), favoritePage.getTotal());
        List<Long> courseIds = favoritePage.getRecords().stream().map(CourseFavorite::getCourseId).toList();
        if (courseIds.isEmpty()) {
            result.setRecords(List.of());
            return result;
        }
        Map<Long, Course> courses = courseMapper.selectBatchIds(courseIds).stream().collect(Collectors.toMap(Course::getId, Function.identity()));
        List<Course> orderedCourses = courseIds.stream().map(courses::get).filter(Objects::nonNull).toList();
        result.setRecords(toVOList(orderedCourses, userId, false));
        return result;
    }

    @Override
    public Page<RecentLearningRecordVO> pageLearningRecords(Long userId, long current, long size) {
        Page<LearningRecord> recordPage = learningRecordMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getUserId, userId)
                .orderByDesc(LearningRecord::getLastWatchTime));
        Page<RecentLearningRecordVO> result = new Page<>(recordPage.getCurrent(), recordPage.getSize(), recordPage.getTotal());
        List<LearningRecord> records = recordPage.getRecords();
        if (records.isEmpty()) {
            result.setRecords(List.of());
            return result;
        }
        Set<Long> courseIds = records.stream().map(LearningRecord::getCourseId).collect(Collectors.toSet());
        Set<Long> chapterIds = records.stream().map(LearningRecord::getChapterId).collect(Collectors.toSet());
        Map<Long, Course> courses = courseMapper.selectBatchIds(courseIds).stream().collect(Collectors.toMap(Course::getId, Function.identity()));
        Map<Long, Chapter> chapters = chapterMapper.selectBatchIds(chapterIds).stream().collect(Collectors.toMap(Chapter::getId, Function.identity()));
        result.setRecords(records.stream().map(record -> {
            RecentLearningRecordVO vo = new RecentLearningRecordVO();
            Course course = courses.get(record.getCourseId());
            Chapter chapter = chapters.get(record.getChapterId());
            vo.setCourseId(record.getCourseId());
            vo.setCourseTitle(course == null ? null : course.getTitle());
            vo.setChapterId(record.getChapterId());
            vo.setChapterTitle(chapter == null ? null : chapter.getTitle());
            vo.setProgress(record.getProgress());
            vo.setLastWatchTime(record.getLastWatchTime());
            return vo;
        }).toList());
        return result;
    }

    @Override
    public LearningRecord saveRecord(Long userId, LearningRecordRequest request) {
        LearningRecord record = learningRecordMapper.selectOne(new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getUserId, userId)
                .eq(LearningRecord::getChapterId, request.getChapterId()));
        if (record == null) {
            record = new LearningRecord();
            record.setUserId(userId);
            record.setCourseId(request.getCourseId());
            record.setChapterId(request.getChapterId());
            record.setProgress(request.getProgress());
            record.setLastWatchTime(LocalDateTime.now());
            learningRecordMapper.insert(record);
        } else {
            record.setProgress(request.getProgress());
            record.setLastWatchTime(LocalDateTime.now());
            learningRecordMapper.updateById(record);
        }
        return record;
    }

    private Course requireTeacherCourse(Long teacherId, Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(404, "课程不存在");
        }
        if (!Objects.equals(course.getTeacherId(), teacherId)) {
            throw new BusinessException(403, "只能操作自己的课程");
        }
        return course;
    }

    private List<CourseVO> toVOList(List<Course> courses, Long userId, boolean withChapters) {
        if (courses.isEmpty()) {
            return List.of();
        }
        Set<Long> categoryIds = courses.stream().map(Course::getCategoryId).collect(Collectors.toSet());
        Set<Long> teacherIds = courses.stream().map(Course::getTeacherId).collect(Collectors.toSet());
        Map<Long, Category> categories = categoryMapper.selectBatchIds(categoryIds).stream().collect(Collectors.toMap(Category::getId, Function.identity()));
        Map<Long, User> teachers = userMapper.selectBatchIds(teacherIds).stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return courses.stream().map(course -> {
            CourseVO vo = fillVO(course, categories.get(course.getCategoryId()), teachers.get(course.getTeacherId()), userId);
            if (withChapters) {
                vo.setChapters(listChapters(course.getId()));
            }
            return vo;
        }).toList();
    }

    private CourseVO toVO(Course course, Long userId, boolean withChapters) {
        Category category = categoryMapper.selectById(course.getCategoryId());
        User teacher = userMapper.selectById(course.getTeacherId());
        CourseVO vo = fillVO(course, category, teacher, userId);
        if (withChapters) {
            vo.setChapters(listChapters(course.getId()));
        }
        return vo;
    }

    private CourseVO fillVO(Course course, Category category, User teacher, Long userId) {
        CourseVO vo = new CourseVO();
        vo.setId(course.getId());
        vo.setTitle(course.getTitle());
        vo.setCover(course.getCover());
        vo.setIntro(course.getIntro());
        vo.setCategoryId(course.getCategoryId());
        vo.setCategoryName(category == null ? null : category.getName());
        vo.setTeacherId(course.getTeacherId());
        vo.setTeacherName(teacher == null ? null : teacher.getUsername());
        vo.setPrice(course.getPrice());
        vo.setStatus(course.getStatus());
        vo.setFavoritesCount(course.getFavoritesCount());
        vo.setCreatedAt(course.getCreatedAt());
        vo.setFavorite(userId != null && favoriteMapper.exists(new LambdaQueryWrapper<CourseFavorite>()
                .eq(CourseFavorite::getUserId, userId)
                .eq(CourseFavorite::getCourseId, course.getId())));
        boolean free = isFree(course);
        boolean purchased = userId != null && !free && courseOrderService.hasCourseAccess(userId, course.getId());
        vo.setPurchased(purchased);
        vo.setAccessible(free || purchased);
        return vo;
    }


    private Chapter requireCourseChapter(Long courseId, Long chapterId) {
        Chapter chapter = chapterMapper.selectOne(new LambdaQueryWrapper<Chapter>()
                .eq(Chapter::getId, chapterId)
                .eq(Chapter::getCourseId, courseId));
        if (chapter == null) {
            throw new BusinessException(404, "?????");
        }
        return chapter;
    }

    private InputStream openRangeStream(Path file, long start) throws IOException {
        InputStream inputStream = Files.newInputStream(file);
        long skipped = inputStream.skip(start);
        if (skipped != start) {
            inputStream.close();
            throw new IOException();
        }
        return inputStream;
    }

    private String resolveContentType(Path file) throws IOException {
        String contentType = Files.probeContentType(file);
        return contentType == null ? "video/mp4" : contentType;
    }

    private boolean isFree(Course course) {
        return course.getPrice() == null || course.getPrice().compareTo(BigDecimal.ZERO) <= 0;
    }

    private List<Chapter> listChapters(Long courseId) {
        return chapterMapper.selectList(new LambdaQueryWrapper<Chapter>().eq(Chapter::getCourseId, courseId).orderByAsc(Chapter::getSort));
    }
}
