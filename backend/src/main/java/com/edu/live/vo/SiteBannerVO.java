package com.edu.live.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SiteBannerVO {
    private Long id;
    private String title;
    private String subtitle;
    private String imageUrl;
    private String linkUrl;
    private Integer sort;
    private String status;
    private Long creatorId;
    private String creatorName;
    private LocalDateTime createdAt;
}
