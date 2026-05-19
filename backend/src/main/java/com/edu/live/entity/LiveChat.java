package com.edu.live.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("live_chat")
public class LiveChat {
    private Long id;
    private Long roomId;
    private Long userId;
    private String message;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
