package com.edu.live.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LiveMicRequestVO {
    private Long id;
    private Long roomId;
    private Long userId;
    private String username;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
