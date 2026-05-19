package com.edu.live.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminUserVO {
    private Long id;
    private String username;
    private String email;
    private String role;
    private Integer status;
    private String avatar;
    private String bio;
    private Integer teachingYears;
    private String expertise;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
