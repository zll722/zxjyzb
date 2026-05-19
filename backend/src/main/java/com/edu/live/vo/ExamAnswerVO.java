package com.edu.live.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ExamAnswerVO {
    private Long id;
    private Long submissionId;
    private Long questionId;
    private String questionType;
    private String questionContent;
    private List<String> options;
    private String correctAnswer;
    private String answer;
    private Boolean correct;
    private BigDecimal score;
    private BigDecimal questionScore;
    private String comment;
}
