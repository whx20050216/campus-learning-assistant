package com.campus.learning.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.learning.entity.Material;
import com.campus.learning.mapper.KeywordMapper;
import com.campus.learning.mapper.MaterialMapper;
import com.campus.learning.service.SearchService;
import com.campus.learning.vo.SearchResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private MaterialMapper materialMapper;

    @Autowired
    private KeywordMapper keywordMapper;

    @Override
    public Page<SearchResultVO> searchByKeyword(String keyword, int page, int size) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new Page<>();
        }
        String cleanKeyword = keyword.trim();
        if (cleanKeyword.length() > 50) {
            cleanKeyword = cleanKeyword.substring(0, 50);
        }
        // 去除特殊字符，保留中文、英文、数字和空格
        cleanKeyword = cleanKeyword.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9\\s]", " ");
        if (cleanKeyword.isEmpty()) {
            return new Page<>();
        }

        Page<SearchResultVO> mpPage = new Page<>(page + 1, size);
        try {
            return materialMapper.searchByFulltext(cleanKeyword, mpPage);
        } catch (Exception e) {
            log.error("全文检索失败: {}", e.getMessage());
            // 降级为 LIKE 模糊匹配
            Page<Material> materialPage = new Page<>(page + 1, size);
            QueryWrapper<Material> wrapper = new QueryWrapper<>();
            wrapper.like("title", cleanKeyword).or().like("course_tag", cleanKeyword);
            Page<Material> result = materialMapper.selectPage(materialPage, wrapper);
            Page<SearchResultVO> voPage = new Page<>();
            voPage.setTotal(result.getTotal());
            voPage.setPages(result.getPages());
            voPage.setCurrent(result.getCurrent());
            voPage.setSize(result.getSize());
            List<SearchResultVO> records = result.getRecords().stream().map(m -> {
                SearchResultVO vo = new SearchResultVO();
                vo.setId(m.getId());
                vo.setTitle(m.getTitle());
                vo.setCourseTag(m.getCourseTag());
                vo.setSource(m.getSource());
                vo.setCreatedAt(m.getCreatedAt());
                return vo;
            }).collect(Collectors.toList());
            voPage.setRecords(records);
            return voPage;
        }
    }

    @Override
    public List<SearchResultVO> searchByKnowledge(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> materialIds = keywordMapper.findMaterialIdsByKeyword(keyword.trim());
        if (materialIds == null || materialIds.isEmpty()) {
            return Collections.emptyList();
        }
        return materialMapper.findByIds(materialIds);
    }

    @Override
    public List<SearchResultVO> searchByCourse(String courseTag) {
        if (courseTag == null || courseTag.trim().isEmpty()) {
            return Collections.emptyList();
        }
        QueryWrapper<Material> wrapper = new QueryWrapper<>();
        wrapper.eq("course_tag", courseTag.trim());
        wrapper.orderByDesc("created_at");
        List<Material> materials = materialMapper.selectList(wrapper);
        return materials.stream().map(m -> {
            SearchResultVO vo = new SearchResultVO();
            vo.setId(m.getId());
            vo.setTitle(m.getTitle());
            vo.setCourseTag(m.getCourseTag());
            vo.setSource(m.getSource());
            vo.setCreatedAt(m.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());
    }
}
