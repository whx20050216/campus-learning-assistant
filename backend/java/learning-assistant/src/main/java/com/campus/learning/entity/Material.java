package com.campus.learning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("materials")
public class Material {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String fileUrl;

    private String fileType;

    private Long fileSize;

    private Integer pages;

    private String md5;

    private String courseTag;

    private String status;

    private String source;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
