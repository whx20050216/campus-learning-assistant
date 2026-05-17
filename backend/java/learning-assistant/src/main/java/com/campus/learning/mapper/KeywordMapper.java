package com.campus.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.learning.entity.Keyword;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KeywordMapper extends BaseMapper<Keyword> {

    List<Keyword> findByMaterialId(@Param("materialId") Long materialId);

    List<Long> findMaterialIdsByKeyword(@Param("keyword") String keyword);
}
