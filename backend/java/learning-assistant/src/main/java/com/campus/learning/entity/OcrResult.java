package com.campus.learning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ocr_result")
public class OcrResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long materialId;

    private String ocrText;

    private Float confidence;

    private String source;

    private String engine;

    private String summary;

    private Integer processingTimeMs;

    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
