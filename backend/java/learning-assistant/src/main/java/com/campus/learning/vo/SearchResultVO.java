package com.campus.learning.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SearchResultVO {
    private Long id;
    private String title;
    private String courseTag;
    private String ocrTextSnippet;
    private List<String> keywords;
    private String source;
    private LocalDateTime createdAt;
}
