package com.edu.live.vo;

import lombok.Data;

@Data
public class TeacherStatsVO {
    private Long courseCount;
    private Long liveCount;
    private Long replayCount;
    private Long homeworkCount;
    private Long examCount;
    private Long studentSubmissionCount;
    private Long pendingHomeworkCount;
    private Long pendingExamCount;
}
