package com.edu.live.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("exam_submission")
public class ExamSubmission {
    private Long id;
    private Long examId;
    private Long studentId;
    private BigDecimal objectiveScore;
    private BigDecimal subjectiveScore;
    private BigDecimal totalScore;
    private String status;
    private String comment;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
}
