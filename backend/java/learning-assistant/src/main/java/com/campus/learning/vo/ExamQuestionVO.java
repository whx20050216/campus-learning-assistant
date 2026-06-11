package com.campus.learning.vo;

import lombok.Data;

import java.util.List;

@Data
public class ExamQuestionVO {
    private Long id;
    private Long paperId;
    private Long materialId;
    private String type;
    private String content;
    private List<String> options;
    private String answer;
    private String analysis;
    private String difficulty;
    private String knowledgePoint;
    private Integer sortOrder;
}
