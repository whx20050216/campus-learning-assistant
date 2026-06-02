package com.campus.learning.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PlanVO {

    private Long id;

    private Long userId;

    private String name;

    private String description;

    private LocalDate startDate;

    private LocalDate endDate;

    private Float dailyHours;

    private Integer totalPages;

    private Float progress;

    private String status;

    private LocalDate remindDate;

    private Integer reminderSent;

    private Integer version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<TaskVO> tasks;

    private List<MaterialVO> materials;
}
