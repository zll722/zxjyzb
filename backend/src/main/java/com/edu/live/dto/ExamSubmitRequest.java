package com.edu.live.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ExamSubmitRequest {
    @Valid
    @NotEmpty(message = "答案不能为空")
    private List<AnswerItem> answers;

    @Data
    public static class AnswerItem {
        @NotNull(message = "题目不能为空")
        private Long questionId;

        private String answer;
    }
}
