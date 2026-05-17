package com.campus.learning.controller;

import com.campus.learning.dto.CheckInDTO;
import com.campus.learning.dto.PlanCreateDTO;
import com.campus.learning.dto.Result;
import com.campus.learning.security.CurrentUserUtils;
import com.campus.learning.service.PlanService;
import com.campus.learning.vo.PlanVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/plans")
public class PlanController {

    @Autowired
    private PlanService planService;

    @PostMapping
    public Result<PlanVO> createPlan(@RequestBody PlanCreateDTO dto) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        return planService.createPlan(dto, userId);
    }

    @GetMapping("/{id}")
    public Result<PlanVO> getPlanDetail(@PathVariable Long id) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        return planService.getPlanDetail(id);
    }

    @GetMapping
    public Result<List<PlanVO>> getPlanList() {
        Long userId = CurrentUserUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        return planService.getPlanList(userId);
    }

    @PostMapping("/tasks/{taskId}/check-in")
    public Result<Void> checkIn(@PathVariable Long taskId, @RequestBody CheckInDTO dto) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        return planService.checkIn(taskId, dto, userId);
    }

    @GetMapping("/{id}/progress")
    public Result<Float> getProgress(@PathVariable Long id) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        Float progress = planService.calculateProgress(id);
        return Result.success(progress);
    }
}
