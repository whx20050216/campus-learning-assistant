package com.campus.learning.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TaskVO {

    private Long id;

    private Long planId;

    private Long materialId;

    private String taskName;

    private LocalDate taskDate;

    private Float plannedHours;

    private String status;

    private LocalDateTime completedAt;

    private Boolean checkedIn;

    private Integer version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
