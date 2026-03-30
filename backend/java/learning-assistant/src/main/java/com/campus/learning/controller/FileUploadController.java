package com.campus.learning.controller;

import com.campus.learning.entity.Material;
import com.campus.learning.service.FileUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/materials")
@CrossOrigin(origins = "http://localhost:5173")
public class FileUploadController {

    @Autowired
    private FileUploadService fileUploadService;

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
                                    @RequestParam("userId") Long userId) {
        try {
            Material material = fileUploadService.uploadFile(file, userId);

            // 返回结构化数据给前端
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("id", material.getId());
            result.put("filename", material.getFilename());
            result.put("status", material.getStatus());
            result.put("ocrText", material.getOcrText());
            result.put("ocrConfidence", material.getOcrConfidence());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("上传失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}