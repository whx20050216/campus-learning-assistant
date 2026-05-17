package com.campus.learning.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.learning.entity.Material;
import com.campus.learning.vo.MaterialDetailVO;
import com.campus.learning.vo.MaterialVO;
import org.springframework.web.multipart.MultipartFile;

public interface MaterialService {

    Material upload(MultipartFile file, Long userId) throws Exception;

    MaterialDetailVO getDetail(Long id);

    Page<MaterialVO> getList(Long userId, String courseTag, int page, int size);

    void delete(Long id, Long userId);

    void processOcr(Long materialId, String fileUrl);
}
