package com.campus.learning.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExamRecordVO {
    private Long id;
    private Long paperId;
    private Long userId;
    private String answers;
    private Integer score;
    private Integer correctCount;
    private Integer totalCount;
    private Integer spentTimeSeconds;
    private String status;
    private LocalDateTime createdAt;
}
