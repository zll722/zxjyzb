package com.edu.live.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HomeworkVO {
    private Long id;
    private Long courseId;
    private String courseTitle;
    private Long teacherId;
    private String teacherName;
    private String title;
    private String description;
    private LocalDateTime deadline;
    private Long submissionId;
    private String submissionStatus;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
}
