package com.campus.learning.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PlanUpdateDTO {

    private String name;

    private LocalDate endDate;

    private Float dailyHours;

    private List<Long> materialIds;
}
