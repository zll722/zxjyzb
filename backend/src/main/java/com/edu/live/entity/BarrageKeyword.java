package com.edu.live.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("barrage_keyword")
public class BarrageKeyword {
    private Long id;
    private String keyword;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
