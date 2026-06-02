package com.campus.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.learning.entity.Material;
import com.campus.learning.vo.SearchResultVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MaterialMapper extends BaseMapper<Material> {

    List<Material> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    Page<SearchResultVO> searchByFulltext(@Param("keyword") String keyword, Page<SearchResultVO> page, @Param("userId") Long userId);

    List<SearchResultVO> searchByLike(@Param("keyword") String keyword, @Param("userId") Long userId);

    List<SearchResultVO> findByIds(@Param("ids") List<Long> ids, @Param("userId") Long userId);
}
