package com.edu.live.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TeacherCourseStatsVO {
    private Long courseId;
    private String courseTitle;
    private Long studentCount;
    private Long homeworkCount;
    private Long homeworkSubmissionCount;
    private BigDecimal homeworkSubmissionRate;
    private Long examCount;
    private Long examSubmissionCount;
    private BigDecimal examSubmissionRate;
    private BigDecimal examAverageScore;
    private Long liveCount;
    private Long replayCount;
}
