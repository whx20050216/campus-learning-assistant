package com.campus.learning.service;

import com.campus.learning.dto.CheckInDTO;
import com.campus.learning.dto.PlanCreateDTO;
import com.campus.learning.dto.Result;
import com.campus.learning.entity.StudyPlan;
import com.campus.learning.vo.PlanVO;

import java.util.List;

public interface PlanService {

    Result<PlanVO> createPlan(PlanCreateDTO dto, Long userId);

    Result<Void> checkIn(Long taskId, CheckInDTO dto, Long userId);

    Float calculateProgress(Long planId);

    Result<PlanVO> getPlanDetail(Long planId);

    Result<List<PlanVO>> getPlanList(Long userId);
}
