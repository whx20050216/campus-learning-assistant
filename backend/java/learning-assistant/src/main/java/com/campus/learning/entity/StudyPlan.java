package com.campus.learning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("study_plan")
public class StudyPlan {

    @TableId(type = IdType.AUTO)
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
}
