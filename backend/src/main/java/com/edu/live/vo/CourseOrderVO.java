package com.edu.live.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CourseOrderVO {
    private Long id;
    private String orderNo;
    private Long userId;
    private String username;
    private Long courseId;
    private String courseTitle;
    private Long teacherId;
    private String teacherName;
    private BigDecimal amount;
    private String status;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
