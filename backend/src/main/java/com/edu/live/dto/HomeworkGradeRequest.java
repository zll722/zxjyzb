package com.edu.live.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class HomeworkGradeRequest {
    @NotNull(message = "分数不能为空")
    @DecimalMin(value = "0.00", message = "分数不能小于0")
    @DecimalMax(value = "100.00", message = "分数不能大于100")
    private BigDecimal score;

    private String comment;
}
