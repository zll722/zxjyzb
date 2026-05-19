package com.edu.live.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class LivePollVO {
    private Long id;
    private Long roomId;
    private String question;
    private List<String> options;
    private Integer selectedOptionIndex;
    private List<PollOptionResultVO> results;
    private Integer totalVotes;
    private LocalDateTime createdAt;

    @Data
    public static class PollOptionResultVO {
        private Integer optionIndex;
        private String optionText;
        private Long count;
    }
}
