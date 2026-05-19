package com.edu.live.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LiveReplayRequest {
    private Long roomId;

    @NotBlank(message = "回放标题不能为空")
    @Size(max = 100, message = "回放标题最多100个字符")
    private String title;

    private String intro;
    private String status;
}
