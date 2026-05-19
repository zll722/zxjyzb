package com.edu.live.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edu.live.entity.HomeworkSubmission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HomeworkSubmissionMapper extends BaseMapper<HomeworkSubmission> {
    @Select("select count(*) from homework_submission hs join homework h on hs.homework_id = h.id where h.teacher_id = #{teacherId}")
    Long countByTeacher(Long teacherId);

    @Select("select count(*) from homework_submission hs join homework h on hs.homework_id = h.id where h.teacher_id = #{teacherId} and hs.reviewed_at is null")
    Long countPendingByTeacher(Long teacherId);

    @Select("select count(*) from homework_submission hs join homework h on hs.homework_id = h.id where h.course_id = #{courseId}")
    Long countByCourse(Long courseId);
}
