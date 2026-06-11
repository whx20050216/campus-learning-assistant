package com.campus.learning.dto;

import lombok.Data;

import java.util.Map;

@Data
public class SubmitAnswersDTO {
    private Map<Long, String> answers;
    private Integer spentTimeSeconds;
}
