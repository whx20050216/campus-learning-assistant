package com.campus.learning.service;

import com.campus.learning.entity.Material;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AiEngineService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String PYTHON_API = "http://localhost:8000";

    /**
     * 调用 Python OCR 服务
     * @param fileBytes 文件字节数组
     * @param filename 文件名
     * @return OCR结果（文本、置信度、来源）
     */
    public OcrResult performOcr(byte[] fileBytes, String filename) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // 创建 MultipartFile 资源
            ByteArrayResource resource = new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", resource);

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            log.info("调用 Python OCR 服务，文件: {}", filename);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    PYTHON_API + "/ai/ocr",
                    request,
                    Map.class
            );

            Map result = response.getBody();

            String text = (String) result.getOrDefault("text", "");
            Double confidence = Double.valueOf(result.getOrDefault("confidence", 0.0).toString());
            String source = (String) result.getOrDefault("source", "unknown");

            log.info("OCR 完成，置信度: {}, 来源: {}", confidence, source);

            return new OcrResult(text, confidence, source);

        } catch (Exception e) {
            log.error("OCR 调用失败: {}", e.getMessage());
            // 降级：返回空结果，不阻断主流程
            return new OcrResult("", 0.0, "error");
        }
    }

    // OCR 结果 DTO
    public record OcrResult(String text, Double confidence, String source) {}

    /**
     * 调用 Python NLP 服务进行文本分析
     * @param text OCR 识别出的文本
     * @return NLP分析结果（关键词、摘要等）
     */
    public NlpResult analyzeText(String text) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 构建 JSON 请求体
            Map<String, String> body = new HashMap<>();
            body.put("text", text);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            log.info("调用 Python NLP 服务，文本长度: {}", text.length());

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    PYTHON_API + "/ai/nlp",
                    request,
                    Map.class
            );

            Map result = response.getBody();
            Map stats = (Map) result.get("stats");

            // 解析关键词列表
            List<Map<String, Object>> keywordsRaw = (List<Map<String, Object>>) result.get("keywords");
            List<String> keywords = keywordsRaw.stream()
                    .map(k -> (String) k.get("word"))
                    .toList();

            return new NlpResult(
                    keywords,
                    (String) result.get("summary"),
                    (List<String>) result.get("key_sentences"),
                    Integer.valueOf(stats.get("char_count").toString())
            );

        } catch (Exception e) {
            log.error("NLP 调用失败: {}", e.getMessage());
            // 降级返回空结果
            return new NlpResult(List.of(), "", List.of(), 0);
        }
    }

    // NLP 结果 DTO
    public record NlpResult(
            List<String> keywords,
            String summary,
            List<String> keySentences,
            int charCount
    ) {}
}