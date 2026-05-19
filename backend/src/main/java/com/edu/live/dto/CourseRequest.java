package com.edu.live.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CourseRequest {
    @NotBlank(message = "课程名称不能为空")
    private String title;
    private String cover;
    private String intro;
    @NotNull(message = "课程分类不能为空")
    private Long categoryId;
    private BigDecimal price = BigDecimal.ZERO;
}
