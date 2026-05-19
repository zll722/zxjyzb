package com.edu.live.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class HomeworkSubmissionVO {
    private Long id;
    private Long homeworkId;
    private String homeworkTitle;
    private Long courseId;
    private String courseTitle;
    private Long studentId;
    private String studentName;
    private String content;
    private String attachmentPath;
    private String attachmentUrl;
    private BigDecimal score;
    private String comment;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
}
