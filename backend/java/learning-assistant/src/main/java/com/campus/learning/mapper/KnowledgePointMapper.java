package com.campus.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.learning.entity.KnowledgePoint;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KnowledgePointMapper extends BaseMapper<KnowledgePoint> {

    List<KnowledgePoint> findByMaterialId(@Param("materialId") Long materialId);
}
