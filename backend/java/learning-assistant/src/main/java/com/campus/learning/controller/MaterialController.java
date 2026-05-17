package com.campus.learning.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.learning.dto.Result;
import com.campus.learning.entity.Material;
import com.campus.learning.security.CurrentUserUtils;
import com.campus.learning.service.MaterialService;
import com.campus.learning.vo.MaterialDetailVO;
import com.campus.learning.vo.MaterialVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/materials")
public class MaterialController {

    @Autowired
    private MaterialService materialService;

    @PostMapping("/upload")
    public Result<MaterialVO> upload(@RequestParam("file") MultipartFile file) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        try {
            Material material = materialService.upload(file, userId);
            MaterialVO vo = new MaterialVO();
            vo.setId(material.getId());
            vo.setTitle(material.getTitle());
            vo.setStatus(material.getStatus());
            vo.setSource(material.getSource());
            vo.setCreatedAt(material.getCreatedAt());
            return Result.success(vo);
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
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        try {
            materialService.delete(id, userId);
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("删除失败", e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }
}
