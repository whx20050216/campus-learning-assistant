package com.campus.learning.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.learning.entity.Material;
import com.campus.learning.vo.MaterialDetailVO;
import com.campus.learning.vo.MaterialVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

public interface MaterialService {

    Material upload(MultipartFile file, Long userId, String courseTag, Integer pages) throws Exception;

    MaterialDetailVO getDetail(Long id);

    MaterialDetailVO getDetailForAdmin(Long id);

    Page<MaterialVO> getList(Long userId, String courseTag, int page, int size);

    void delete(Long id, Long userId);

    void deleteMaterial(Long id, Long userId, boolean permanent);

    void restoreMaterial(Long id, Long userId);

    void processOcr(Long materialId, String fileUrl);

    Material getMaterialById(Long id);

    String getPreviewUrl(Long id, Long userId);

    InputStream getFileStream(String fileUrl);

    void updateKeywords(Long id, Long userId, List<String> keywords);

    void updateMaterialInfo(Long id, Long userId, String title, String courseTag, Integer pages);
}
