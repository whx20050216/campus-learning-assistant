package com.campus.learning.service;

import com.campus.learning.dto.Result;
import com.campus.learning.vo.DashboardVO;

public interface AnalysisService {

    Result<DashboardVO> getDashboard(Long userId);
}
