package com.campus.learning.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.learning.vo.SearchResultVO;

import java.util.List;

public interface SearchService {

    Page<SearchResultVO> searchByKeyword(String keyword, int page, int size);

    List<SearchResultVO> searchByKnowledge(String keyword);

    List<SearchResultVO> searchByCourse(String courseTag);
}
