package com.edu.live.vo;

import com.edu.live.entity.Chapter;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CourseVO {
    private Long id;
    private String title;
    private String cover;
    private String intro;
    private Long categoryId;
    private String categoryName;
    private Long teacherId;
    private String teacherName;
    private BigDecimal price;
    private String status;
    private Integer favoritesCount;
    private Boolean favorite;
    private Boolean accessible;
    private Boolean purchased;
    private List<Chapter> chapters;
    private LocalDateTime createdAt;
}
