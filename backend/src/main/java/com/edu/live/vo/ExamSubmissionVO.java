package com.edu.live.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExamSubmissionVO {
    private Long id;
    private Long examId;
    private String examTitle;
    private Long courseId;
    private String courseTitle;
    private Long studentId;
    private String studentName;
    private BigDecimal objectiveScore;
    private BigDecimal subjectiveScore;
    private BigDecimal totalScore;
    private String status;
    private String comment;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private List<ExamAnswerVO> answers;
}
