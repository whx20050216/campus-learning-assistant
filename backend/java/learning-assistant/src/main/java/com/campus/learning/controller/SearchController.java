package com.campus.learning.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.learning.dto.Result;
import com.campus.learning.security.CurrentUserUtils;
import com.campus.learning.service.SearchService;
import com.campus.learning.vo.SearchResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    @Autowired
    private SearchService searchService;

    @GetMapping
    public Result<?> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "keyword") String mode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Long userId = CurrentUserUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }

        switch (mode) {
            case "keyword": {
                Page<SearchResultVO> result = searchService.searchByKeyword(query, page, size);
                return Result.success(result);
            }
            case "knowledge": {
                List<SearchResultVO> result = searchService.searchByKnowledge(query);
                return Result.success(result);
            }
            case "course": {
                List<SearchResultVO> result = searchService.searchByCourse(query);
                return Result.success(result);
            }
            default:
                return Result.error("非法搜索模式，支持 keyword/knowledge/course");
        }
    }
}
