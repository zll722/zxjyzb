package com.edu.live.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class AdminTeacherProfileRequest {
    private String bio;
    @Min(value = 0, message = "教学年限不能小于0")
    private Integer teachingYears;
    private String expertise;
}
