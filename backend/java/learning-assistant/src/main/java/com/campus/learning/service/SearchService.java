package com.campus.learning.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.learning.vo.SearchResultVO;

import java.util.List;

public interface SearchService {

    Page<SearchResultVO> searchByKeyword(String keyword, int page, int size, Long userId);

    List<SearchResultVO> searchByKnowledge(String keyword, Long userId);

    List<SearchResultVO> searchByCourse(String courseTag, Long userId);

    Page<SearchResultVO> searchAll(String query, int page, int size, Long userId);
}
