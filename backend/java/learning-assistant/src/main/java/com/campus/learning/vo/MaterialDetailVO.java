package com.campus.learning.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class MaterialDetailVO {
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

    private OcrResultVO ocrResult;
    private List<KeywordVO> keywords;
}
