package com.campus.learning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("study_task")
public class StudyTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long planId;

    private Long materialId;

    private String taskName;

    private LocalDate taskDate;

    private Float plannedHours;

    private String status;

    private LocalDateTime completedAt;

    private Integer version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
