package com.edu.live.vo;

import lombok.Data;

@Data
public class PlatformStatsVO {
    private Long userCount;
    private Long teacherCount;
    private Long studentCount;
    private Long courseCount;
    private Long liveCount;
    private Long replayCount;
    private Long homeworkCount;
    private Long examCount;
}
