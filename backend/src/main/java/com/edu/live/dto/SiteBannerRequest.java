package com.edu.live.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SiteBannerRequest {
    @NotBlank(message = "轮播标题不能为空")
    private String title;

    private String subtitle;

    @NotBlank(message = "轮播图片不能为空")
    private String imageUrl;

    private String linkUrl;

    @NotNull(message = "排序不能为空")
    private Integer sort;

    @NotBlank(message = "轮播状态不能为空")
    private String status;
}
