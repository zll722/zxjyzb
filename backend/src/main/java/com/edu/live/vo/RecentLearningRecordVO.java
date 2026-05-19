package com.edu.live.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RecentLearningRecordVO {
    private Long courseId;
    private String courseTitle;
    private Long chapterId;
    private String chapterTitle;
    private Integer progress;
    private LocalDateTime lastWatchTime;
}
