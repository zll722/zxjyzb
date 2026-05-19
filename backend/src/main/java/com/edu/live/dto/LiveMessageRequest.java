package com.edu.live.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LiveMessageRequest {
    @NotBlank(message = "内容不能为空")
    private String content;
}
