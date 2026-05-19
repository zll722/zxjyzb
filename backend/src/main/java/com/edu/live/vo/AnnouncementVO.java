package com.edu.live.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnnouncementVO {
    private Long id;
    private String title;
    private String content;
    private String type;
    private String status;
    private Integer pinned;
    private Long creatorId;
    private String creatorName;
    private LocalDateTime publishTime;
    private LocalDateTime createdAt;
}
