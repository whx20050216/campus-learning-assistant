package com.campus.learning.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExamPaperVO {
    private Long id;
    private Long userId;
    private Long studyPlanId;
    private String courseTag;
    private String title;
    private String materialIds;
    private Integer questionCount;
    private String difficulty;
    private String status;
    private List<ExamQuestionVO> questions;
    private LocalDateTime createdAt;
}
