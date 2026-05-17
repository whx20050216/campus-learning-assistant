package com.campus.learning.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.learning.entity.Keyword;
import com.campus.learning.entity.KnowledgePoint;
import com.campus.learning.entity.Material;
import com.campus.learning.entity.OcrResult;
import com.campus.learning.mapper.KeywordMapper;
import com.campus.learning.mapper.KnowledgePointMapper;
import com.campus.learning.mapper.MaterialMapper;
import com.campus.learning.mapper.OcrResultMapper;
import com.campus.learning.service.AiEngineService;
import com.campus.learning.service.MaterialService;
import com.campus.learning.vo.KeywordVO;
import com.campus.learning.vo.MaterialDetailVO;
import com.campus.learning.vo.MaterialVO;
import com.campus.learning.vo.OcrResultVO;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MaterialServiceImpl implements MaterialService {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MaterialMapper materialMapper;

    @Autowired
    private OcrResultMapper ocrResultMapper;

    @Autowired
    private KeywordMapper keywordMapper;

    @Autowired
    private KnowledgePointMapper knowledgePointMapper;

    @Autowired
    private AiEngineService aiEngineService;

    @Autowired
    @Lazy
    private MaterialService self;

    @Value("${minio.bucket-name}")
    private String bucketName;

    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024; // 50MB
    private static final double CONFIDENCE_THRESHOLD = 0.85;
    private static final long PROCESS_TIMEOUT_MS = 30000; // 30 seconds

    @Override
    public Material upload(MultipartFile file, Long userId) throws Exception {
        // 1. 校验文件格式
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("文件名格式不正确");
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!"pdf".equals(extension) && !"jpg".equals(extension) && !"jpeg".equals(extension) && !"png".equals(extension)) {
            throw new IllegalArgumentException("仅支持 PDF、JPG、PNG 格式");
        }
        String normalizedType = switch (extension) {
            case "pdf" -> "PDF";
            case "jpeg", "jpg", "png" -> "IMAGE";
            default -> "UNKNOWN";
        };

        // 2. 校验文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("单文件大小不能超过50MB");
        }

        // 3. 计算 MD5
        String md5 = calculateMd5(file.getBytes());

        // 4. MD5 查重（当前用户）
        QueryWrapper<Material> md5Wrapper = new QueryWrapper<>();
        md5Wrapper.eq("user_id", userId);
        md5Wrapper.eq("md5", md5);
        Material existing = materialMapper.selectOne(md5Wrapper);
        if (existing != null) {
            log.info("MD5 重复，直接返回已有资料: materialId={}", existing.getId());
            return existing;
        }

        // 5. MinIO 上传
        String uniqueFilename = UUID.randomUUID().toString() + "." + extension;
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(uniqueFilename)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );

        // 6. 保存 Material
        Material material = new Material();
        material.setUserId(userId);
        material.setTitle(originalFilename);
        material.setFileType(normalizedType);
        material.setFileUrl(uniqueFilename);
        material.setFileSize(file.getSize());
        material.setMd5(md5);
        material.setStatus("processing");
        material.setSource("local");
        material.setCreatedAt(LocalDateTime.now());
        material.setUpdatedAt(LocalDateTime.now());
        materialMapper.insert(material);

        // 7. 同步处理 OCR（30秒总超时保护）
        long startTime = System.currentTimeMillis();
        try {
            self.processOcr(material.getId(), uniqueFilename);
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("OCR 总耗时: {}ms, materialId={}", elapsed, material.getId());
            if (elapsed > PROCESS_TIMEOUT_MS) {
                throw new RuntimeException("处理超时，建议压缩后重试");
            }
        } catch (Exception e) {
            log.error("OCR 处理异常: materialId={}, error={}", material.getId(), e.getMessage());
            Material update = new Material();
            update.setId(material.getId());
            update.setStatus("failed");
            update.setUpdatedAt(LocalDateTime.now());
            materialMapper.updateById(update);
            if (e.getMessage() != null && e.getMessage().contains("超时")) {
                throw new RuntimeException("处理超时，建议压缩后重试");
            }
            throw new RuntimeException("处理失败: " + e.getMessage());
        }

        return materialMapper.selectById(material.getId());
    }

    @Transactional
    @Override
    public void processOcr(Long materialId, String fileUrl) {
        long startTime = System.currentTimeMillis();
        Material material = materialMapper.selectById(materialId);
        if (material == null) {
            throw new IllegalArgumentException("资料不存在");
        }

        try {
            // 从 MinIO 下载文件
            byte[] fileBytes;
            try (InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder().bucket(bucketName).object(fileUrl).build())) {
                fileBytes = stream.readAllBytes();
            }

            // 检查总时间
            if (System.currentTimeMillis() - startTime > PROCESS_TIMEOUT_MS) {
                throw new RuntimeException("处理超时");
            }

            // 调用 Python OCR
            AiEngineService.OcrResult ocrResult = aiEngineService.performOcr(fileBytes, material.getTitle());
            String ocrText = ocrResult.text();
            double confidence = ocrResult.confidence() != null ? ocrResult.confidence() : 0.0;

            log.info("OCR 完成: materialId={}, confidence={}", materialId, confidence);

            if (System.currentTimeMillis() - startTime > PROCESS_TIMEOUT_MS) {
                throw new RuntimeException("处理超时");
            }

            // 双轨决策
            boolean useLocalNlp = confidence > CONFIDENCE_THRESHOLD;
            String source = "local";
            String engine = "paddleocr";
            String summary = "";
            List<Keyword> keywords = new ArrayList<>();
            List<KnowledgePoint> knowledgePoints = new ArrayList<>();

            if (useLocalNlp) {
                // 本地 NLP 处理
                AiEngineService.NlpResult nlpResult = aiEngineService.analyzeText(ocrText);
                summary = nlpResult.summary();
                keywords = nlpResult.keywords().stream().map(kw -> {
                    Keyword k = new Keyword();
                    k.setMaterialId(materialId);
                    k.setKeyword(kw);
                    k.setWeight(1.0f);
                    return k;
                }).collect(Collectors.toList());
                knowledgePoints = extractKnowledgePoints(ocrText, materialId);

            } else {
                // 低置信度：尝试智谱 API 增强（mock，后续联调）
                try {
                    String enhanced = apiEnhance(ocrText);
                    if (enhanced != null && !enhanced.isEmpty()) {
                        ocrText = enhanced;
                        source = "ai_enhanced";
                        engine = "zhipu";
                    }
                    AiEngineService.NlpResult nlpResult = aiEngineService.analyzeText(ocrText);
                    summary = nlpResult.summary();
                    keywords = nlpResult.keywords().stream().map(kw -> {
                        Keyword k = new Keyword();
                        k.setMaterialId(materialId);
                        k.setKeyword(kw);
                        k.setWeight(1.0f);
                        return k;
                    }).collect(Collectors.toList());
                    knowledgePoints = extractKnowledgePoints(ocrText, materialId);
                } catch (Exception e) {
                    log.warn("智谱 API 增强失败，降级本地处理: {}", e.getMessage());
                    AiEngineService.NlpResult nlpResult = aiEngineService.analyzeText(ocrText);
                    summary = nlpResult.summary();
                    keywords = nlpResult.keywords().stream().map(kw -> {
                        Keyword k = new Keyword();
                        k.setMaterialId(materialId);
                        k.setKeyword(kw);
                        k.setWeight(1.0f);
                        return k;
                    }).collect(Collectors.toList());
                    knowledgePoints = extractKnowledgePoints(ocrText, materialId);
                    source = "local_fallback";
                }
            }

            if (System.currentTimeMillis() - startTime > PROCESS_TIMEOUT_MS) {
                throw new RuntimeException("处理超时");
            }

            int processingTimeMs = (int) (System.currentTimeMillis() - startTime);

            // 保存 OCR 结果
            OcrResult result = new OcrResult();
            result.setMaterialId(materialId);
            result.setOcrText(ocrText);
            result.setConfidence((float) confidence);
            result.setSource(source);
            result.setEngine(engine);
            result.setSummary(summary);
            result.setProcessingTimeMs(processingTimeMs);
            result.setCreatedAt(LocalDateTime.now());
            ocrResultMapper.insert(result);

            // 批量保存关键词
            if (!keywords.isEmpty()) {
                for (Keyword k : keywords) {
                    keywordMapper.insert(k);
                }
            }

            // 批量保存知识点
            if (!knowledgePoints.isEmpty()) {
                for (KnowledgePoint kp : knowledgePoints) {
                    knowledgePointMapper.insert(kp);
                }
            }

            // 更新 Material 状态
            Material update = new Material();
            update.setId(materialId);
            update.setStatus("completed");
            update.setSource(source);
            update.setUpdatedAt(LocalDateTime.now());
            materialMapper.updateById(update);

            log.info("OCR 处理完成: materialId={}, source={}, keywords={}, knowledgePoints={}",
                    materialId, source, keywords.size(), knowledgePoints.size());

        } catch (Exception e) {
            log.error("processOcr 异常: materialId={}", materialId, e);
            throw new RuntimeException("OCR 处理失败: " + e.getMessage(), e);
        }
    }

    @Override
    public MaterialDetailVO getDetail(Long id) {
        Material material = materialMapper.selectById(id);
        if (material == null) {
            return null;
        }

        MaterialDetailVO vo = new MaterialDetailVO();
        BeanUtils.copyProperties(material, vo);

        OcrResult ocr = ocrResultMapper.findByMaterialId(id);
        if (ocr != null) {
            OcrResultVO ocrVo = new OcrResultVO();
            BeanUtils.copyProperties(ocr, ocrVo);
            vo.setOcrResult(ocrVo);
        }

        List<Keyword> keywords = keywordMapper.findByMaterialId(id);
        List<KeywordVO> keywordVos = keywords.stream().map(k -> {
            KeywordVO kv = new KeywordVO();
            kv.setKeyword(k.getKeyword());
            kv.setWeight(k.getWeight());
            return kv;
        }).collect(Collectors.toList());
        vo.setKeywords(keywordVos);

        return vo;
    }

    @Override
    public Page<MaterialVO> getList(Long userId, String courseTag, int page, int size) {
        Page<Material> mpPage = new Page<>(page + 1, size);
        QueryWrapper<Material> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        if (courseTag != null && !courseTag.isEmpty()) {
            wrapper.eq("course_tag", courseTag);
        }
        wrapper.orderByDesc("created_at");
        Page<Material> result = materialMapper.selectPage(mpPage, wrapper);

        Page<MaterialVO> voPage = new Page<>();
        voPage.setTotal(result.getTotal());
        voPage.setPages(result.getPages());
        voPage.setCurrent(result.getCurrent());
        voPage.setSize(result.getSize());
        List<MaterialVO> records = result.getRecords().stream().map(m -> {
            MaterialVO vo = new MaterialVO();
            BeanUtils.copyProperties(m, vo);
            return vo;
        }).collect(Collectors.toList());
        voPage.setRecords(records);
        return voPage;
    }

    @Transactional
    @Override
    public void delete(Long id, Long userId) {
        Material material = materialMapper.selectById(id);
        if (material == null) {
            throw new IllegalArgumentException("资料不存在");
        }
        if (!material.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权删除该资料");
        }

        QueryWrapper<OcrResult> ocrWrapper = new QueryWrapper<>();
        ocrWrapper.eq("material_id", id);
        ocrResultMapper.delete(ocrWrapper);

        QueryWrapper<Keyword> kwWrapper = new QueryWrapper<>();
        kwWrapper.eq("material_id", id);
        keywordMapper.delete(kwWrapper);

        QueryWrapper<KnowledgePoint> kpWrapper = new QueryWrapper<>();
        kpWrapper.eq("material_id", id);
        knowledgePointMapper.delete(kpWrapper);

        materialMapper.deleteById(id);

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(material.getFileUrl())
                            .build()
            );
        } catch (Exception e) {
            log.error("MinIO 文件删除失败: materialId={}, fileUrl={}", id, material.getFileUrl(), e);
        }
    }

    private String apiEnhance(String text) throws Exception {
        log.info("智谱 API 增强（mock，待批次3联调）");
        return null;
    }

    private List<KnowledgePoint> extractKnowledgePoints(String text, Long materialId) {
        List<KnowledgePoint> points = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return points;
        }

        Pattern definitionPattern = Pattern.compile("([^。！？\\n]{2,30}?(?:是指|的定义为|是：|意味着))");
        Matcher dm = definitionPattern.matcher(text);
        while (dm.find()) {
            KnowledgePoint kp = new KnowledgePoint();
            kp.setMaterialId(materialId);
            kp.setContent(dm.group());
            kp.setType("definition");
            points.add(kp);
        }

        Pattern formulaPattern = Pattern.compile("([A-Za-z0-9\\s]*?[∑∫=\\+\\-\\*/^]{1,}[A-Za-z0-9\\s\\(\\)]{2,50})");
        Matcher fm = formulaPattern.matcher(text);
        while (fm.find()) {
            KnowledgePoint kp = new KnowledgePoint();
            kp.setMaterialId(materialId);
            kp.setContent(fm.group());
            kp.setType("formula");
            points.add(kp);
        }

        Pattern theoremPattern = Pattern.compile("([^。！？\\n]{2,30}?(?:定理|引理|推论))");
        Matcher tm = theoremPattern.matcher(text);
        while (tm.find()) {
            KnowledgePoint kp = new KnowledgePoint();
            kp.setMaterialId(materialId);
            kp.setContent(tm.group());
            kp.setType("theorem");
            points.add(kp);
        }

        return points.stream().limit(20).collect(Collectors.toList());
    }

    private String calculateMd5(byte[] bytes) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
