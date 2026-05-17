package com.campus.learning.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OcrResultVO {
    private Long id;
    private String ocrText;
    private Float confidence;
    private String source;
    private String engine;
    private String summary;
    private Integer processingTimeMs;
    private LocalDateTime createdAt;
}
