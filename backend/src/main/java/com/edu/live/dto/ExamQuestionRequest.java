package com.edu.live.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ExamQuestionRequest {
    @NotBlank(message = "题型不能为空")
    private String type;

    @NotBlank(message = "题干不能为空")
    private String content;

    private List<String> options;

    @NotBlank(message = "答案不能为空")
    private String answer;

    @NotNull(message = "分值不能为空")
    @DecimalMin(value = "0.1", message = "分值必须大于0")
    private BigDecimal score;

    @Min(value = 0, message = "排序不能小于0")
    private Integer sort;
}
