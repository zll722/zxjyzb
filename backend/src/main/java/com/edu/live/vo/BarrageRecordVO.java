package com.edu.live.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BarrageRecordVO {
    private Long id;
    private Long roomId;
    private String roomTitle;
    private Long userId;
    private String username;
    private String content;
    private LocalDateTime createdAt;
}
