package com.edu.live.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LiveRoomVO {
    private Long id;
    private String title;
    private String cover;
    private String intro;
    private Long teacherId;
    private String teacherName;
    private String status;
    private LocalDateTime scheduledTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer onlineCount;
    private String agoraAppId;
    private String agoraChannel;
    private String agoraToken;
}
