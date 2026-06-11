package com.campus.learning.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.learning.dto.Result;
import com.campus.learning.dto.UpdateOcrDTO;
import com.campus.learning.entity.Material;
import com.campus.learning.entity.OcrResult;
import com.campus.learning.mapper.MaterialMapper;
import com.campus.learning.mapper.OcrResultMapper;
import com.campus.learning.security.CurrentUserUtils;
import com.campus.learning.service.AiEngineService;
import com.campus.learning.service.MaterialService;
import com.campus.learning.vo.MaterialDetailVO;
import com.campus.learning.vo.MaterialVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/materials")
public class MaterialController {

    @Autowired
    private MaterialService materialService;

    @Autowired
    private MaterialMapper materialMapper;

    @Autowired
    private OcrResultMapper ocrResultMapper;

    @Autowired
    private AiEngineService aiEngineService;

    @PostMapping("/upload")
    public Result<MaterialDetailVO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String courseTag,
            @RequestParam(required = false) Integer pages) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        if (courseTag != null && courseTag.length() > 20) {
            return Result.error("标签长度不能超过20字");
        }
        if (pages != null && pages <= 0) {
            return Result.error("页数必须大于0");
        }
        try {
            Material material = materialService.upload(file, userId, courseTag, pages);
            // 返回完整详情（含OCR结果），使前端上传页可直接展示
            MaterialDetailVO detail = materialService.getDetail(material.getId());
            if (detail == null) {
                // 兜底：若 getDetail 过滤返回 null，构造简化响应
                detail = new MaterialDetailVO();
                detail.setId(material.getId());
                detail.setTitle(material.getTitle());
                detail.setStatus(material.getStatus());
                detail.setSource(material.getSource());
                detail.setCreatedAt(material.getCreatedAt());
            }
            return Result.success(detail);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("超时")) {
                return Result.error(408, e.getMessage());
            }
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("上传失败", e);
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public Result<MaterialDetailVO> getDetail(@PathVariable Long id) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        MaterialDetailVO detail = materialService.getDetail(id);
        if (detail == null) {
            return Result.error("资料不存在");
        }
        // 权限校验：只能查看自己的资料（管理员除外）
        if (!detail.getUserId().equals(userId)) {
            return Result.error(403, "无权访问该资料");
        }
        return Result.success(detail);
    }

    @GetMapping
    public Result<Page<MaterialVO>> getList(
            @RequestParam(required = false) String courseTag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        Page<MaterialVO> result = materialService.getList(userId, courseTag, page, size);
        return Result.success(result);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id,
                                @RequestParam(defaultValue = "false") boolean permanent) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        try {
            materialService.deleteMaterial(id, userId, permanent);
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("删除失败", e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/restore")
    public Result<Void> restore(@PathVariable Long id) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        try {
            materialService.restoreMaterial(id, userId);
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("恢复失败", e);
            return Result.error("恢复失败: " + e.getMessage());
        }
    }

    @GetMapping("/trash")
    public Result<Page<MaterialVO>> getTrashList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        try {
            Page<Material> mpPage = new Page<>(page + 1, size);
            QueryWrapper<Material> wrapper = new QueryWrapper<>();
            wrapper.eq("user_id", userId);
            wrapper.isNotNull("deleted_at");
            wrapper.orderByDesc("deleted_at");
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
            return Result.success(voPage);
        } catch (Exception e) {
            log.error("查询回收站失败", e);
            return Result.error("查询回收站失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> downloadMaterial(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean preview) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        if (userId == null) {
            throw new RuntimeException("未登录");
        }

        // 1. 校验资料存在，且未被管理员删除
        Material material = materialService.getMaterialById(id);
        if (material == null) {
            throw new RuntimeException("资料不存在");
        }
        // 管理员可访问任意资料，普通用户只能访问自己的
        String role = CurrentUserUtils.getCurrentUserRole();
        boolean isAdmin = "ADMIN".equals(role);
        if (!isAdmin && !material.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问该资料");
        }
        if (material.getDeletedAt() != null || "admin".equals(material.getDeletedBy())) {
            throw new RuntimeException("资料已被删除或不可访问");
        }

        // 2. 从 MinIO 获取文件流
        InputStream stream = materialService.getFileStream(material.getFileUrl());

        // 3. 根据文件类型设置 Content-Type
        String contentType = switch (material.getFileType().toUpperCase()) {
            case "PDF" -> "application/pdf";
            case "JPG", "JPEG" -> "image/jpeg";
            case "PNG" -> "image/png";
            default -> "application/octet-stream";
        };

        // 4. 构建响应头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));

        String filename = URLEncoder.encode(material.getTitle(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        if (preview) {
            headers.add("Content-Disposition", "inline; filename=\"" + filename + "\"");
        } else {
            headers.add("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        }

        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(stream));
    }

    @PutMapping("/{id}/keywords")
    public Result<Void> updateKeywords(@PathVariable Long id, @RequestBody List<String> keywords) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        try {
            materialService.updateKeywords(id, userId, keywords);
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            log.error("更新关键词失败", e);
            return Result.error("更新关键词失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/ocr")
    public Result<Void> updateOcrText(@PathVariable Long id, @RequestBody UpdateOcrDTO dto) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }

        if (dto.getOcrText() == null || dto.getOcrText().trim().isEmpty()) {
            return Result.error("文本不能为空");
        }
        if (dto.getOcrText().length() > 5000) {
            return Result.error("单次编辑不能超过5000字");
        }

        Material material = materialService.getMaterialById(id);
        if (material == null || !material.getUserId().equals(userId)) {
            return Result.error(403, "无权操作");
        }

        UpdateWrapper<OcrResult> wrapper = new UpdateWrapper<>();
        wrapper.eq("material_id", id);
        wrapper.set("ocr_text", dto.getOcrText());
        wrapper.set("source", "manual_corrected");
        wrapper.set("updated_at", LocalDateTime.now());
        ocrResultMapper.update(null, wrapper);

        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> updateMaterialInfo(
            @PathVariable Long id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String courseTag,
            @RequestParam(required = false) Integer pages) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        if (courseTag != null && courseTag.length() > 20) {
            return Result.error("标签长度不能超过20字");
        }
        try {
            materialService.updateMaterialInfo(id, userId, title, courseTag, pages);
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            log.error("更新资料信息失败", e);
            return Result.error("更新资料信息失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/ai-summary")
    public Result<String> aiEnhanceSummary(@PathVariable Long id) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }

        Material material = materialService.getMaterialById(id);
        if (material == null) {
            return Result.error(404, "资料不存在");
        }
        if (!material.getUserId().equals(userId)) {
            return Result.error(403, "无权操作该资料");
        }

        OcrResult ocrResult = ocrResultMapper.findByMaterialId(id);
        if (ocrResult == null || ocrResult.getOcrText() == null || ocrResult.getOcrText().isEmpty()) {
            return Result.error(400, "该资料暂无 OCR 文本，无法生成 AI 摘要");
        }

        String aiSummary = aiEngineService.generateAiSummary(ocrResult.getOcrText());
        if (aiSummary == null || aiSummary.isEmpty()) {
            return Result.error(500, "AI 摘要生成失败，请稍后重试");
        }

        UpdateWrapper<OcrResult> wrapper = new UpdateWrapper<>();
        wrapper.eq("material_id", id);
        wrapper.set("summary", aiSummary);
        wrapper.set("source", "ai_enhanced_summary");
        wrapper.set("updated_at", LocalDateTime.now());
        ocrResultMapper.update(null, wrapper);

        return Result.success(aiSummary);
    }
}
