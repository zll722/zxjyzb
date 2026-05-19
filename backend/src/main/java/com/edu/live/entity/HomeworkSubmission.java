package com.edu.live.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("homework_submission")
public class HomeworkSubmission {
    private Long id;
    private Long homeworkId;
    private Long studentId;
    private String content;
    private String attachmentPath;
    private BigDecimal score;
    private String comment;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
}
