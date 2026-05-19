package com.edu.live.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LearningRecordRequest {
    @NotNull(message = "课程不能为空")
    private Long courseId;
    @NotNull(message = "章节不能为空")
    private Long chapterId;
    private Integer progress = 0;
}
