package com.campus.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.learning.entity.OcrResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OcrResultMapper extends BaseMapper<OcrResult> {

    OcrResult findByMaterialId(@Param("materialId") Long materialId);
}
