package com.campus.learning.service;

import com.campus.learning.entity.Material;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AiEngineService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${python.api.url:http://localhost:8000}")
    private String pythonApiUrl;

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
                    pythonApiUrl + "/ai/ocr",
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
                    pythonApiUrl + "/ai/nlp",
                    request,
                    Map.class
            );

            Map result = response.getBody();

            // 解析关键词列表（Python 返回 keyword 字段）
            List<Map<String, Object>> keywordsRaw = (List<Map<String, Object>>) result.get("keywords");
            List<String> keywords = List.of();
            if (keywordsRaw != null) {
                keywords = keywordsRaw.stream()
                        .map(k -> {
                            Object word = k.get("keyword");
                            if (word == null) {
                                word = k.get("word");
                            }
                            return word != null ? word.toString() : null;
                        })
                        .filter(w -> w != null && !w.isEmpty())
                        .toList();
            }

            String summary = result.get("summary") != null ? result.get("summary").toString() : "";

            return new NlpResult(
                    keywords,
                    summary,
                    List.of(),
                    text != null ? text.length() : 0
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

    // API 增强结果 DTO
    public record ApiEnhanceResult(String text, String summary) {}

    /**
     * 调用 Python /ai/enhance 进行智谱 API 增强
     * @param text OCR 识别出的文本
     * @param fileUrl 文件路径（当前仅传 text）
     * @return ApiEnhanceResult（text 为增强文本，summary 为深度摘要；失败返回 null）
     */
    public ApiEnhanceResult apiEnhance(String text, String fileUrl) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("text", text);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            log.info("调用 Python API 增强服务，文本长度: {}", text != null ? text.length() : 0);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    pythonApiUrl + "/ai/enhance",
                    request,
                    Map.class
            );

            Map result = response.getBody();
            if (result != null && result.get("text") != null) {
                String enhancedText = (String) result.get("text");
                String summary = result.get("summary") != null ? result.get("summary").toString() : null;
                log.info("智谱 API 增强成功，返回文本长度: {}, 摘要长度: {}",
                        enhancedText.length(),
                        summary != null ? summary.length() : 0);
                return new ApiEnhanceResult(enhancedText, summary);
            }
        } catch (Exception e) {
            log.warn("智谱 API 增强调用失败（可能未配置 ZHIPU_API_KEY）: {}", e.getMessage());
        }
        return null; // 降级：返回 null，触发 fallback 本地处理
    }

    /**
     * 调用 Python /ai/summary 生成 AI 深度摘要
     * @param text OCR 识别出的文本
     * @return 摘要字符串；失败返回 null
     */
    public String generateAiSummary(String text) {
        try {
            // 配置超时：连接5秒，读取15秒（匹配Python端摘要生成耗时）
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(5000);
            factory.setReadTimeout(15000);
            RestTemplate rt = new RestTemplate(factory);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("text", text);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            log.info("调用 Python AI 摘要服务，文本长度: {}", text != null ? text.length() : 0);

            ResponseEntity<Map> response = rt.postForEntity(
                    pythonApiUrl + "/ai/summary",
                    request,
                    Map.class
            );

            Map result = response.getBody();
            if (result != null && result.get("summary") != null) {
                String summary = result.get("summary").toString();
                log.info("AI 摘要生成成功，长度: {}", summary.length());
                return summary;
            }
        } catch (Exception e) {
            log.warn("AI 摘要生成失败（可能未配置 ZHIPU_API_KEY 或超时）: {}", e.getMessage());
        }
        return null; // 降级：返回 null，保留本地摘要
    }
}