package com.campus.learning.service;

import com.campus.learning.entity.Material;
import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {
    Material uploadFile(MultipartFile file, Long userId) throws Exception;
}