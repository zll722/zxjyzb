package com.edu.live.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course_favorite")
public class CourseFavorite {
    private Long id;
    private Long userId;
    private Long courseId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
