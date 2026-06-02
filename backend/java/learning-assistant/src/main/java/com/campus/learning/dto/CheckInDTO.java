package com.campus.learning.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CheckInDTO {

    private Integer duration;

    private String content;

    private LocalDate studyDate;
}
