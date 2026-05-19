package com.edu.live.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("live_poll_vote")
public class LivePollVote {
    private Long id;
    private Long pollId;
    private Long userId;
    private Integer optionIndex;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
