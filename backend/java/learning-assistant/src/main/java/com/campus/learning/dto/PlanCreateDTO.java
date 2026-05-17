package com.campus.learning.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PlanCreateDTO {

    private String name;

    private String description;

    private LocalDate startDate;

    private LocalDate endDate;

    private Float dailyHours;

    private List<Long> materialIds;
}
