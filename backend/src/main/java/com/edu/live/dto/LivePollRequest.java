package com.edu.live.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class LivePollRequest {
    @NotBlank(message = "投票题目不能为空")
    private String question;
    @NotEmpty(message = "投票选项不能为空")
    private List<String> options;
}
