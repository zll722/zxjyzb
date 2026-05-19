package com.edu.live.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edu.live.entity.ExamSubmission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

@Mapper
public interface ExamSubmissionMapper extends BaseMapper<ExamSubmission> {
    @Select("select coalesce(round(avg(total_score), 2), 0) from exam_submission where student_id = #{studentId}")
    BigDecimal averageScoreByStudent(Long studentId);

    @Select("select count(*) from exam_submission es join exam e on es.exam_id = e.id where e.teacher_id = #{teacherId}")
    Long countByTeacher(Long teacherId);

    @Select("select count(*) from exam_submission es join exam e on es.exam_id = e.id where e.teacher_id = #{teacherId} and es.status = 'PENDING_REVIEW'")
    Long countPendingByTeacher(Long teacherId);

    @Select("select count(*) from exam_submission es join exam e on es.exam_id = e.id where e.course_id = #{courseId}")
    Long countByCourse(Long courseId);

    @Select("select coalesce(round(avg(es.total_score), 2), 0) from exam_submission es join exam e on es.exam_id = e.id where e.course_id = #{courseId}")
    BigDecimal averageScoreByCourse(Long courseId);
}
