package com.edu.live.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("exam_answer")
public class ExamAnswer {
    private Long id;
    private Long submissionId;
    private Long questionId;
    private String answer;
    private Boolean correct;
    private BigDecimal score;
    private String comment;
}
