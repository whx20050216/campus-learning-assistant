package com.campus.learning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("exam_questions")
public class ExamQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long paperId;

    private Long materialId;

    private String type;

    private String content;

    private String options;

    private String answer;

    private String analysis;

    private String difficulty;

    private String knowledgePoint;

    private Integer sortOrder;

    private LocalDateTime createdAt;
}
