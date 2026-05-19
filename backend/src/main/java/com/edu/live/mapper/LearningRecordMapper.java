package com.edu.live.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edu.live.entity.LearningRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LearningRecordMapper extends BaseMapper<LearningRecord> {
    @Select("select count(distinct course_id) from learning_record where user_id = #{studentId}")
    Long countDistinctCoursesByStudent(Long studentId);

    @Select("select count(distinct user_id) from learning_record where course_id = #{courseId}")
    Long countDistinctStudentsByCourse(Long courseId);

    @Select("select distinct user_id from learning_record where course_id = #{courseId}")
    List<Long> selectDistinctStudentIdsByCourse(Long courseId);
}
