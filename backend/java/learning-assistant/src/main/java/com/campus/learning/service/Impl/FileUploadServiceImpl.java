package com.campus.learning.service.impl;

import com.campus.learning.entity.Material;
import com.campus.learning.repository.MaterialRepository;
import com.campus.learning.service.AiEngineService;
import com.campus.learning.service.FileUploadService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class FileUploadServiceImpl implements FileUploadService {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private AiEngineService aiEngineService;  // 新增：注入 OCR 服务

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Override
    public Material uploadFile(MultipartFile file, Long userId) throws Exception {
        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uniqueFilename = UUID.randomUUID().toString() + extension;

        // 上传到 MinIO
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(uniqueFilename)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );

        // 判断文件类型
        Material.FileType fileType = determineFileType(extension);

        // 保存基础信息到数据库（状态为 processing）
        Material material = new Material();
        material.setUserId(userId);
        material.setFilename(originalFilename);
        material.setFileType(fileType);
        material.setMinioPath(uniqueFilename);
        material.setFileSize(file.getSize());
        material.setStatus(Material.Status.processing);  // 改为 processing
        material.setCreatedAt(LocalDateTime.now());
        material.setUpdatedAt(LocalDateTime.now());

        material = materialRepository.save(material);  // 先保存，拿到 ID

        // 调用 OCR（仅对图片和 PDF）
        if (fileType == Material.FileType.IMAGE || fileType == Material.FileType.PDF) {
            try {
                // 重新读取文件（因为上面 file.getInputStream() 已经读过了）
                byte[] fileBytes = file.getBytes();
                AiEngineService.OcrResult result = aiEngineService.performOcr(fileBytes, originalFilename);

                // 更新 OCR 结果
                material.setOcrText(result.text());
                material.setOcrConfidence(result.confidence().floatValue());  // Double 转 Float
                material.setStatus(result.confidence() > 0.85 ? Material.Status.completed : Material.Status.completed);
                // 注意：即使置信度低，本地 OCR 也完成了，AI 增强是另外的手动触发逻辑

                log.info("OCR 完成: id={}, confidence={}", material.getId(), result.confidence());

            } catch (Exception e) {
                log.error("OCR 失败: {}", e.getMessage());
                material.setOcrText("");
                material.setOcrConfidence(0.0f);
                material.setStatus(Material.Status.completed);  // OCR 失败也算上传完成，只是没识别出文字
            }

            material.setUpdatedAt(LocalDateTime.now());
            materialRepository.save(material);  // 更新
        } else {
            // PPT 不做 OCR
            material.setStatus(Material.Status.completed);
            materialRepository.save(material);
        }

        return material;
    }

    private Material.FileType determineFileType(String extension) {
        String ext = extension.toLowerCase();
        if (ext.equals(".ppt") || ext.equals(".pptx")) {
            return Material.FileType.PPT;
        } else if (ext.equals(".pdf")) {
            return Material.FileType.PDF;
        } else {
            return Material.FileType.IMAGE;
        }
    }
}