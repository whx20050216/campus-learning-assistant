package com.campus.learning.controller;

import com.campus.learning.dto.Result;
import com.campus.learning.security.CurrentUserUtils;
import com.campus.learning.service.AnalysisService;
import com.campus.learning.vo.DashboardVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    @Autowired
    private AnalysisService analysisService;

    @GetMapping("/dashboard")
    public Result<DashboardVO> getDashboard() {
        Long userId = CurrentUserUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        return analysisService.getDashboard(userId);
    }
}
