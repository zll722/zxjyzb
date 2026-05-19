package com.edu.live.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExamRequest {
    @NotNull(message = "课程不能为空")
    private Long courseId;

    @NotBlank(message = "考试标题不能为空")
    private String title;

    private String description;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Min(value = 1, message = "考试时长必须大于0")
    private Integer durationMinutes;
}
