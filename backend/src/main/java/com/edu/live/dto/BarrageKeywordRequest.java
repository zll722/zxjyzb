package com.edu.live.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BarrageKeywordRequest {
    @NotBlank(message = "关键词不能为空")
    @Size(max = 50, message = "关键词不能超过50个字符")
    private String keyword;
}
