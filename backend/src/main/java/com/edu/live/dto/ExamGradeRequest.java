package com.edu.live.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ExamGradeRequest {
    private String comment;

    @Valid
    private List<AnswerGradeItem> answers;

    @Data
    public static class AnswerGradeItem {
        @NotNull(message = "答题记录不能为空")
        private Long answerId;

        @NotNull(message = "分数不能为空")
        @DecimalMin(value = "0.0", message = "分数不能小于0")
        private BigDecimal score;

        private String comment;
    }
}
