package com.edu.live.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class StudentStatsVO {
    private Long learnedCourseCount;
    private Long favoriteCount;
    private Long homeworkSubmissionCount;
    private Long examParticipationCount;
    private BigDecimal averageScore;
    private List<RecentLearningRecordVO> recentRecords;
}
