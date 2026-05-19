package com.edu.live.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChapterRequest {
    @NotBlank(message = "章节标题不能为空")
    private String title;
    private String videoPath;
    private Integer duration = 0;
    private Integer sort = 0;
}
