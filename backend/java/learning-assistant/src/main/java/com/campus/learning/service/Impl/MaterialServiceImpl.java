package com.campus.learning.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.learning.entity.Keyword;
import com.campus.learning.entity.KnowledgePoint;
import com.campus.learning.entity.Material;
import com.campus.learning.entity.OcrResult;
import com.campus.learning.entity.StudyTask;
import com.campus.learning.entity.User;
import com.campus.learning.mapper.KeywordMapper;
import com.campus.learning.mapper.KnowledgePointMapper;
import com.campus.learning.mapper.MaterialMapper;
import com.campus.learning.mapper.OcrResultMapper;
import com.campus.learning.mapper.StudyTaskMapper;
import com.campus.learning.mapper.UserMapper;
import com.campus.learning.service.AiEngineService;
import com.campus.learning.service.MaterialService;
import com.campus.learning.service.SensitiveWordService;
import com.campus.learning.vo.KeywordVO;
import com.campus.learning.vo.KnowledgePointVO;
import com.campus.learning.vo.MaterialDetailVO;
import com.campus.learning.vo.MaterialVO;
import com.campus.learning.vo.OcrResultVO;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
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
    private StudyTaskMapper studyTaskMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SensitiveWordService sensitiveWordService;

    @Autowired
    private AiEngineService aiEngineService;

    @Autowired
    @Lazy
    private MaterialService self;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.external-endpoint:}")
    private String minioExternalEndpoint;

    @Value("${minio.access-key}")
    private String minioAccessKey;

    @Value("${minio.secret-key}")
    private String minioSecretKey;

    @Value("${python.api.url:http://localhost:8000}")
    private String pythonApiUrl;

    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024; // 50MB
    private static final long PROCESS_TIMEOUT_MS = 30000; // 30 seconds

    @Value("${ocr.confidence.threshold:0.85}")
    private Float confidenceThreshold;

    @Override
    public Material upload(MultipartFile file, Long userId, String courseTag, Integer pages) throws Exception {
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

        // 3. 计算 MD5（流式，避免大文件全量读入内存）
        String md5 = calculateMd5(file.getInputStream());

        // 4. MD5 查重（当前用户）
        QueryWrapper<Material> md5Wrapper = new QueryWrapper<>();
        md5Wrapper.eq("user_id", userId);
        md5Wrapper.eq("md5", md5);
        md5Wrapper.isNull("deleted_at");
        Material existing = materialMapper.selectOne(md5Wrapper);
        if (existing != null) {
            log.info("MD5 重复，拒绝上传: materialId={}", existing.getId());
            throw new IllegalArgumentException("该文件已存在于您的资料库中");
        }

        // 5. 存储配额校验
        User user = userMapper.selectById(userId);
        if (user != null) {
            long usedStorage = user.getUsedStorage() != null ? user.getUsedStorage() : 0L;
            long quota = user.getStorageQuota() != null ? user.getStorageQuota() : 0L;
            if (usedStorage + file.getSize() > quota) {
                throw new IllegalArgumentException("存储空间已满，请删除旧资料后重试");
            }
        }

        // 6. MinIO 上传
        String uniqueFilename = UUID.randomUUID().toString() + "." + extension;
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(uniqueFilename)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );

        // 7. 保存 Material
        Material material = new Material();
        material.setUserId(userId);
        material.setTitle(originalFilename);
        material.setFileType(normalizedType);
        material.setFileUrl(uniqueFilename);
        material.setFileSize(file.getSize());
        material.setMd5(md5);
        material.setStatus("processing");
        material.setSource("local");
        material.setCourseTag(courseTag);
        if (pages != null && pages > 0) {
            material.setPages(pages);
        }
        material.setCreatedAt(LocalDateTime.now());
        material.setUpdatedAt(LocalDateTime.now());
        materialMapper.insert(material);

        // 更新用户已用存储
        if (user != null) {
            user.setUsedStorage(user.getUsedStorage() + file.getSize());
            userMapper.updateById(user);
        }

        // 8. 同步处理 OCR（30秒总超时保护）
        try {
            java.util.concurrent.CompletableFuture<Void> future = java.util.concurrent.CompletableFuture.runAsync(() -> {
                self.processOcr(material.getId(), uniqueFilename);
            });
            future.get(PROCESS_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
            log.info("OCR 处理完成: materialId={}", material.getId());
        } catch (java.util.concurrent.TimeoutException e) {
            log.error("OCR 处理超时: materialId={}", material.getId());
            Material update = new Material();
            update.setId(material.getId());
            update.setStatus("failed");
            update.setUpdatedAt(LocalDateTime.now());
            materialMapper.updateById(update);
        } catch (Exception e) {
            log.error("OCR 处理异常: materialId={}, error={}", material.getId(), e.getMessage());
            Material update = new Material();
            update.setId(material.getId());
            update.setStatus("failed");
            update.setUpdatedAt(LocalDateTime.now());
            materialMapper.updateById(update);
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
            float threshold = confidenceThreshold != null ? confidenceThreshold : 0.85f;
            boolean useLocalNlp = confidence > threshold;
            String source;
            String engine = "paddleocr";
            String summary = "";
            List<Keyword> keywords = new ArrayList<>();
            List<KnowledgePoint> knowledgePoints = new ArrayList<>();

            if (useLocalNlp) {
                // 高置信度：本地 NLP 处理，不再自动调用智谱 API（用户可在详情页手动触发 AI 增强摘要）
                source = "local";
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
                // 低置信度：尝试智谱 API 增强
                source = "fallback";
                boolean summaryFromApi = false;
                try {
                    AiEngineService.ApiEnhanceResult enhanceResult = aiEngineService.apiEnhance(ocrText, fileUrl);
                    if (enhanceResult != null && enhanceResult.text() != null && !enhanceResult.text().isEmpty()) {
                        ocrText = enhanceResult.text();
                        source = "ai_enhanced";
                        engine = "zhipu";
                        if (enhanceResult.summary() != null && !enhanceResult.summary().isEmpty()) {
                            summary = enhanceResult.summary();
                            summaryFromApi = true;
                        }
                    }
                    // 关键词和知识点仍需 NLP 分析
                    AiEngineService.NlpResult nlpResult = aiEngineService.analyzeText(ocrText);
                    if (!summaryFromApi) {
                        summary = nlpResult.summary();
                        // 兜底：调用 AI 摘要生成覆盖本地 TextRank
                        try {
                            String aiSummary = aiEngineService.generateAiSummary(ocrText);
                            if (aiSummary != null && !aiSummary.isEmpty()) {
                                summary = aiSummary;
                                source = "ai_enhanced";
                            }
                        } catch (Exception e) {
                            log.warn("低置信度资料 AI 摘要兜底失败，保留本地 TextRank: {}", e.getMessage());
                        }
                    }
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
                    source = "fallback";
                }
            }

            if (System.currentTimeMillis() - startTime > PROCESS_TIMEOUT_MS) {
                throw new RuntimeException("处理超时");
            }

            int processingTimeMs = (int) (System.currentTimeMillis() - startTime);

            // 敏感词检测
            boolean hasSensitiveWord = sensitiveWordService.containsSensitiveWord(ocrText);
            String auditStatus = hasSensitiveWord ? "pending" : "approved";
            if (hasSensitiveWord) {
                log.warn("资料 {} 命中敏感词，已标记为待审核", materialId);
            }

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
            update.setAuditStatus(auditStatus);
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
        // 过滤已删除（回收站）资料
        if (material.getDeletedAt() != null || "deleted".equals(material.getStatus())) {
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

        List<KnowledgePoint> knowledgePoints = knowledgePointMapper.findByMaterialId(id);
        List<KnowledgePointVO> knowledgePointVos = knowledgePoints.stream().map(kp -> {
            KnowledgePointVO kpv = new KnowledgePointVO();
            kpv.setContent(kp.getContent());
            kpv.setType(kp.getType());
            return kpv;
        }).collect(Collectors.toList());
        vo.setKnowledgePoints(knowledgePointVos);

        return vo;
    }

    @Override
    public MaterialDetailVO getDetailForAdmin(Long id) {
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

        List<KnowledgePoint> knowledgePoints = knowledgePointMapper.findByMaterialId(id);
        List<KnowledgePointVO> knowledgePointVos = knowledgePoints.stream().map(kp -> {
            KnowledgePointVO kpv = new KnowledgePointVO();
            kpv.setContent(kp.getContent());
            kpv.setType(kp.getType());
            return kpv;
        }).collect(Collectors.toList());
        vo.setKnowledgePoints(knowledgePointVos);

        return vo;
    }

    @Override
    public Material getMaterialById(Long id) {
        return materialMapper.selectById(id);
    }

    @Override
    public Page<MaterialVO> getList(Long userId, String courseTag, int page, int size) {
        Page<Material> mpPage = new Page<>(page + 1, size);
        QueryWrapper<Material> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.isNull("deleted_at"); // 过滤回收站资料
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
        // 兼容旧调用：默认物理删除
        deleteMaterial(id, userId, true);
    }

    @Transactional
    @Override
    public void deleteMaterial(Long id, Long userId, boolean permanent) {
        Material material = materialMapper.selectById(id);
        if (material == null) {
            throw new IllegalArgumentException("资料不存在");
        }
        if (!material.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权删除该资料");
        }

        if (permanent) {
            // 永久删除前：先删 MinIO 文件，失败则抛异常回滚事务，避免 DB 已删文件残留
            try {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(material.getFileUrl())
                                .build()
                );
            } catch (Exception e) {
                log.error("MinIO 文件删除失败: materialId={}, fileUrl={}", id, material.getFileUrl(), e);
                throw new RuntimeException("文件删除失败，请稍后重试");
            }

            // 解除与学习任务的关联（防御性修复）
            com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<StudyTask> taskWrapper = new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
            taskWrapper.eq("material_id", id);
            taskWrapper.set("material_id", null);
            studyTaskMapper.update(null, taskWrapper);

            // 级联删除 DB 记录
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

            // 回减用户已用存储
            User user = userMapper.selectById(userId);
            if (user != null && material.getFileSize() != null) {
                long usedStorage = user.getUsedStorage() != null ? user.getUsedStorage() : 0L;
                user.setUsedStorage(Math.max(0, usedStorage - material.getFileSize()));
                userMapper.updateById(user);
            }
        } else {
            // 逻辑删除：移至回收站
            Material update = new Material();
            update.setId(id);
            update.setDeletedAt(LocalDateTime.now());
            update.setDeletedBy("user");
            update.setStatus("deleted");
            materialMapper.updateById(update);
        }
    }

    @Override
    public void restoreMaterial(Long id, Long userId) {
        Material material = materialMapper.selectById(id);
        if (material == null) {
            throw new IllegalArgumentException("资料不存在");
        }
        if (!material.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权操作该资料");
        }
        if ("admin".equals(material.getDeletedBy())) {
            throw new IllegalArgumentException("管理员删除的资料不可恢复");
        }
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Material> wrapper = new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        wrapper.eq("id", id);
        wrapper.set("deleted_at", null);
        wrapper.set("deleted_by", null);
        wrapper.set("status", "completed");
        wrapper.set("updated_at", java.time.LocalDateTime.now());
        materialMapper.update(null, wrapper);
    }

    private String getExternalPresignedUrl(String bucket, String object, int expirySeconds) {
        try {
            MinioClient externalClient = MinioClient.builder()
                    .endpoint(minioExternalEndpoint)
                    .credentials(minioAccessKey, minioSecretKey)
                    .build();
            return externalClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(object)
                            .expiry(expirySeconds)
                            .build()
            );
        } catch (Exception e) {
            log.error("生成外部预签名 URL 失败: bucket={}, object={}", bucket, object, e);
            throw new RuntimeException("文件预览链接生成失败");
        }
    }

    @Override
    public InputStream getFileStream(String fileUrl) {
        try {
            return minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileUrl)
                    .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("文件读取失败：" + e.getMessage(), e);
        }
    }

    @Override
    public String getPreviewUrl(Long id, Long userId) {
        Material material = materialMapper.selectById(id);
        if (material == null) {
            throw new IllegalArgumentException("资料不存在");
        }
        if (!material.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权访问该资料");
        }
        if (material.getDeletedAt() != null || "admin".equals(material.getDeletedBy())) {
            throw new IllegalArgumentException("资料已被删除或不可访问");
        }
        if (minioExternalEndpoint == null || minioExternalEndpoint.isBlank()) {
            // 未配置外部地址时，回退到内部 client（本地开发场景）
            try {
                return minioClient.getPresignedObjectUrl(
                        GetPresignedObjectUrlArgs.builder()
                                .method(Method.GET)
                                .bucket(bucketName)
                                .object(material.getFileUrl())
                                .expiry(300)
                                .build()
                );
            } catch (Exception e) {
                log.error("生成预签名 URL 失败: materialId={}", id, e);
                throw new RuntimeException("生成预览链接失败: " + e.getMessage());
            }
        }
        return getExternalPresignedUrl(bucketName, material.getFileUrl(), 300);
    }

    @Override
    @Transactional
    public void updateMaterialInfo(Long id, Long userId, String title, String courseTag, Integer pages) {
        Material material = materialMapper.selectById(id);
        if (material == null) {
            throw new IllegalArgumentException("资料不存在");
        }
        if (!material.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权操作该资料");
        }
        if (material.getDeletedAt() != null || "admin".equals(material.getDeletedBy())) {
            throw new IllegalArgumentException("资料已被删除或不可访问");
        }
        if (title != null) {
            material.setTitle(title);
        }
        if (courseTag != null) {
            material.setCourseTag(courseTag);
        }
        if (pages != null && pages > 0) {
            material.setPages(pages);
        }
        material.setUpdatedAt(LocalDateTime.now());
        materialMapper.updateById(material);
    }

    @Override
    @Transactional
    public void updateKeywords(Long id, Long userId, List<String> keywords) {
        Material material = materialMapper.selectById(id);
        if (material == null) {
            throw new IllegalArgumentException("资料不存在");
        }
        if (!material.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权操作该资料");
        }
        if (material.getDeletedAt() != null || "admin".equals(material.getDeletedBy())) {
            throw new IllegalArgumentException("资料已被删除或不可访问");
        }
        // 去重、截断
        List<String> clean = keywords.stream()
                .filter(k -> k != null && !k.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .limit(20)
                .filter(k -> k.length() <= 100)
                .collect(Collectors.toList());
        // 删除旧关键词
        QueryWrapper<Keyword> delWrapper = new QueryWrapper<>();
        delWrapper.eq("material_id", id);
        keywordMapper.delete(delWrapper);
        // 批量插入新关键词
        for (String kw : clean) {
            Keyword k = new Keyword();
            k.setMaterialId(id);
            k.setKeyword(kw);
            k.setWeight(1.0f);
            k.setType("keyword");
            keywordMapper.insert(k);
        }
    }

    // apiEnhance 已迁移至 AiEngineService，本类直接调用 aiEngineService.apiEnhance()

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
            String matched = fm.group();
            // 过滤：包含过多中文字符的匹配项视为普通文本而非公式
            long chineseCount = matched.codePoints().filter(c -> c >= 0x4e00 && c <= 0x9fff).count();
            if (chineseCount > 3) {
                continue;
            }
            KnowledgePoint kp = new KnowledgePoint();
            kp.setMaterialId(materialId);
            kp.setContent(matched);
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

    private String calculateMd5(java.io.InputStream inputStream) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (java.io.InputStream is = inputStream) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                md.update(buffer, 0, len);
            }
        }
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
