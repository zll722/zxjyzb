package com.edu.live.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LiveReplayVO {
    private Long id;
    private Long roomId;
    private String roomTitle;
    private Long teacherId;
    private String teacherName;
    private String title;
    private String intro;
    private String videoPath;
    private String playUrl;
    private Long fileSize;
    private Integer duration;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
