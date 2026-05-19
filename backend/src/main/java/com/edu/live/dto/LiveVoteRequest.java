package com.edu.live.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LiveVoteRequest {
    @NotNull(message = "选项不能为空")
    private Integer optionIndex;
}
