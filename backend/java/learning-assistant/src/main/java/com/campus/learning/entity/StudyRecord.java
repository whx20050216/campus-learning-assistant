package com.campus.learning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("study_record")
public class StudyRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private Long userId;

    private LocalDate studyDate;

    private Integer duration;

    private String content;

    private String checkInType;

    private Integer version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
