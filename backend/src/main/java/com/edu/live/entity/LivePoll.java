package com.edu.live.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("live_poll")
public class LivePoll {
    private Long id;
    private Long roomId;
    private String question;
    private String options;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
