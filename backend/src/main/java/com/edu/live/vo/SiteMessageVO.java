package com.edu.live.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SiteMessageVO {
    private Long id;
    private Long senderId;
    private String senderName;
    private Long receiverId;
    private String receiverName;
    private String title;
    private String content;
    private Integer readStatus;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
