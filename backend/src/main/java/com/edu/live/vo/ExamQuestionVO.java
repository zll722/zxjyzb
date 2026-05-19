package com.edu.live.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExamQuestionVO {
    private Long id;
    private Long examId;
    private String type;
    private String content;
    private List<String> options;
    private String answer;
    private BigDecimal score;
    private Integer sort;
    private LocalDateTime createdAt;
}
