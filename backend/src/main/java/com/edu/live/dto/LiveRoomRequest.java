package com.edu.live.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LiveRoomRequest {
    @NotBlank(message = "直播标题不能为空")
    private String title;
    private String cover;
    private String intro;
    private LocalDateTime scheduledTime;
}
