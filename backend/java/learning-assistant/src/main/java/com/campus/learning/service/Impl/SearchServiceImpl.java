package com.campus.learning.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.learning.entity.Material;
import com.campus.learning.mapper.KeywordMapper;
import com.campus.learning.mapper.MaterialMapper;
import com.campus.learning.service.SearchService;
import com.campus.learning.utils.HighlightUtils;
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
    public Page<SearchResultVO> searchByKeyword(String keyword, int page, int size, Long userId) {
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
            Page<SearchResultVO> result = materialMapper.searchByFulltext(cleanKeyword, mpPage, userId);
            highlightResults(result.getRecords(), cleanKeyword);
            return result;
        } catch (Exception e) {
            log.error("全文检索失败，降级为 LIKE 模糊匹配: {}", e.getMessage());
            List<SearchResultVO> records = materialMapper.searchByLike(cleanKeyword, userId);
            highlightResults(records, cleanKeyword);
            Page<SearchResultVO> voPage = new Page<>(page + 1, size);
            int total = records.size();
            int pages = (int) Math.ceil((double) total / size);
            voPage.setTotal(total);
            voPage.setPages(pages);
            voPage.setCurrent(page + 1);
            voPage.setSize(size);
            // 内存分页
            int fromIndex = Math.min(page * size, total);
            int toIndex = Math.min(fromIndex + size, total);
            voPage.setRecords(records.subList(fromIndex, toIndex));
            return voPage;
        }
    }

    @Override
    public List<SearchResultVO> searchByKnowledge(String keyword, Long userId) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String cleanKeyword = keyword.trim();
        List<Long> materialIds = keywordMapper.findMaterialIdsByKeyword(cleanKeyword, userId);
        if (materialIds == null || materialIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<SearchResultVO> results = materialMapper.findByIds(materialIds, userId);
        highlightResults(results, cleanKeyword);
        return results;
    }

    @Override
    public Page<SearchResultVO> searchAll(String query, int page, int size, Long userId) {
        // 综合模式：先尝试关键词全文检索，如果结果为空则降级到知识点匹配
        Page<SearchResultVO> result = searchByKeyword(query, page, size, userId);
        if (result == null || result.getRecords() == null || result.getRecords().isEmpty()) {
            List<SearchResultVO> knowledgeResults = searchByKnowledge(query, userId);
            if (knowledgeResults != null && !knowledgeResults.isEmpty()) {
                Page<SearchResultVO> voPage = new Page<>();
                voPage.setRecords(knowledgeResults);
                voPage.setTotal(knowledgeResults.size());
                voPage.setPages(1);
                voPage.setCurrent(page + 1);
                voPage.setSize(size);
                return voPage;
            }
            // 如果知识点多为空，尝试按课程标签匹配
            List<SearchResultVO> courseResults = searchByCourse(query, userId);
            if (courseResults != null && !courseResults.isEmpty()) {
                Page<SearchResultVO> voPage = new Page<>();
                voPage.setRecords(courseResults);
                voPage.setTotal(courseResults.size());
                voPage.setPages(1);
                voPage.setCurrent(page + 1);
                voPage.setSize(size);
                return voPage;
            }
        }
        return result;
    }

    @Override
    public List<SearchResultVO> searchByCourse(String courseTag, Long userId) {
        if (courseTag == null || courseTag.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String cleanKeyword = courseTag.trim();
        QueryWrapper<Material> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.like("course_tag", cleanKeyword);
        wrapper.isNull("deleted_at");
        wrapper.orderByDesc("created_at");
        List<Material> materials = materialMapper.selectList(wrapper);
        List<SearchResultVO> results = materials.stream().map(m -> {
            SearchResultVO vo = new SearchResultVO();
            vo.setId(m.getId());
            vo.setTitle(m.getTitle());
            vo.setCourseTag(m.getCourseTag());
            vo.setSource(m.getSource());
            vo.setCreatedAt(m.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());
        highlightResults(results, cleanKeyword);
        return results;
    }

    private void highlightResults(List<SearchResultVO> records, String keyword) {
        if (records == null || keyword == null) return;
        for (SearchResultVO vo : records) {
            vo.setTitle(HighlightUtils.highlight(vo.getTitle(), keyword));
            vo.setOcrTextSnippet(HighlightUtils.highlight(vo.getOcrTextSnippet(), keyword));
        }
    }
}
