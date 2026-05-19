package com.edu.live.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnnouncementRequest {
    @NotBlank(message = "公告标题不能为空")
    private String title;

    @NotBlank(message = "公告内容不能为空")
    private String content;

    @NotBlank(message = "公告类型不能为空")
    private String type;

    @NotBlank(message = "公告状态不能为空")
    private String status;

    @NotNull(message = "置顶排序不能为空")
    private Integer pinned;

    private LocalDateTime publishTime;

    private Boolean syncMessage;
}
