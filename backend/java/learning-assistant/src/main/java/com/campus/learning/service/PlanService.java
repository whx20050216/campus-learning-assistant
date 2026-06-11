package com.campus.learning.service;

import com.campus.learning.dto.CheckInDTO;
import com.campus.learning.dto.PlanCreateDTO;
import com.campus.learning.dto.PlanUpdateDTO;
import com.campus.learning.dto.Result;
import com.campus.learning.entity.StudyPlan;
import com.campus.learning.vo.PlanReminderVO;
import com.campus.learning.vo.PlanVO;

import java.util.List;

public interface PlanService {

    Result<PlanVO> createPlan(PlanCreateDTO dto, Long userId);

    Result<PlanVO> updatePlan(Long planId, PlanUpdateDTO dto, Long userId);

    Result<Void> checkIn(Long taskId, CheckInDTO dto, Long userId);

    Float calculateProgress(Long planId);

    Result<PlanVO> getPlanDetail(Long planId);

    Result<List<PlanVO>> getPlanList(Long userId);

    Result<Void> deletePlan(Long planId, Long userId);

    /**
     * 获取当前用户的未读学习计划提醒
     */
    Result<List<PlanReminderVO>> getReminders(Long userId);

    /**
     * 标记指定计划的提醒为已读
     */
    Result<Void> markReminderRead(Long planId, Long userId);

    /**
     * 检测学习计划是否存在学习中断风险（连续3天未打卡）
     */
    boolean hasInterruptionRisk(Long planId);
}
