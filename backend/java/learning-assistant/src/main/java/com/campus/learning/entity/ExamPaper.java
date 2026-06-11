package com.campus.learning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("exam_papers")
public class ExamPaper {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long studyPlanId;

    private String courseTag;

    private String title;

    private String materialIds;

    private Integer questionCount;

    private String difficulty;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
