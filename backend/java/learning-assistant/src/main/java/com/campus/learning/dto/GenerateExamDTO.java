package com.campus.learning.dto;

import lombok.Data;
import java.util.List;

@Data
public class GenerateExamDTO {
    private List<Long> materialIds;
    private String courseTag;
    private Integer questionCount = 10;
    private List<String> types;
    private String difficulty = "medium";
    private Long studyPlanId;
}
