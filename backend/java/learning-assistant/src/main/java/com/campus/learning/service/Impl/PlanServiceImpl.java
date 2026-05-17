package com.campus.learning.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.learning.dto.CheckInDTO;
import com.campus.learning.dto.PlanCreateDTO;
import com.campus.learning.dto.Result;
import com.campus.learning.entity.Material;
import com.campus.learning.entity.StudyPlan;
import com.campus.learning.entity.StudyRecord;
import com.campus.learning.entity.StudyTask;
import com.campus.learning.mapper.MaterialMapper;
import com.campus.learning.mapper.StudyPlanMapper;
import com.campus.learning.mapper.StudyRecordMapper;
import com.campus.learning.mapper.StudyTaskMapper;
import com.campus.learning.service.PlanService;
import com.campus.learning.vo.PlanVO;
import com.campus.learning.vo.TaskVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PlanServiceImpl implements PlanService {

    @Autowired
    private StudyPlanMapper studyPlanMapper;

    @Autowired
    private StudyTaskMapper studyTaskMapper;

    @Autowired
    private StudyRecordMapper studyRecordMapper;

    @Autowired
    private MaterialMapper materialMapper;

    @Transactional
    @Override
    public Result<PlanVO> createPlan(PlanCreateDTO dto, Long userId) {
        // 1. 校验 endDate >= today
        LocalDate today = LocalDate.now();
        if (dto.getEndDate() == null || dto.getEndDate().isBefore(today)) {
            return Result.error("结束日期不能早于今天");
        }
        if (dto.getStartDate() == null || dto.getStartDate().isAfter(dto.getEndDate())) {
            return Result.error("开始日期不能晚于结束日期");
        }
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            return Result.error("计划名称不能为空");
        }
        if (dto.getMaterialIds() == null || dto.getMaterialIds().isEmpty()) {
            return Result.error("请至少选择一项学习资料");
        }

        // 2. 计算可用天数
        long days = ChronoUnit.DAYS.between(dto.getStartDate(), dto.getEndDate()) + 1;
        if (days <= 0) {
            return Result.error("日期范围无效");
        }

        // 3. 计算总页数
        int totalPages = 0;
        List<Material> materials = materialMapper.selectBatchIds(dto.getMaterialIds());
        for (Material material : materials) {
            if (material.getPages() != null && material.getPages() > 0) {
                totalPages += material.getPages();
            } else if (material.getFileSize() != null && material.getFileSize() > 0) {
                // 1MB ≈ 10页
                long mb = material.getFileSize() / (1024 * 1024);
                if (mb == 0) {
                    mb = 1;
                }
                totalPages += (int) (mb * 10);
            } else {
                totalPages += 10;
            }
        }

        // 4. 计算每日任务量（仅用于日志或后续扩展，任务本身使用 dailyHours）
        int dailyPages = (int) Math.ceil((double) totalPages / days);
        log.info("创建计划: userId={}, totalPages={}, days={}, dailyPages={}", userId, totalPages, days, dailyPages);

        // 5. 保存 StudyPlan
        StudyPlan plan = new StudyPlan();
        plan.setUserId(userId);
        plan.setName(dto.getName().trim());
        plan.setDescription(dto.getDescription());
        plan.setStartDate(dto.getStartDate());
        plan.setEndDate(dto.getEndDate());
        plan.setDailyHours(dto.getDailyHours() != null ? dto.getDailyHours() : 1.0f);
        plan.setTotalPages(totalPages);
        plan.setProgress(0.0f);
        plan.setStatus("active");
        plan.setRemindDate(dto.getEndDate().minusDays(3));
        plan.setReminderSent(0);
        plan.setVersion(0);
        plan.setCreatedAt(LocalDateTime.now());
        plan.setUpdatedAt(LocalDateTime.now());
        studyPlanMapper.insert(plan);

        // 6. 生成 StudyTask
        List<StudyTask> tasks = new ArrayList<>();
        LocalDate currentDate = dto.getStartDate();
        int dayIndex = 0;
        while (!currentDate.isAfter(dto.getEndDate())) {
            StudyTask task = new StudyTask();
            task.setPlanId(plan.getId());
            // 关联第一个资料作为代表（或循环分配，这里简化取第一个）
            task.setMaterialId(dto.getMaterialIds().get(dayIndex % dto.getMaterialIds().size()));
            task.setTaskName("第" + dayIndex + "天学习任务");
            task.setTaskDate(currentDate);
            task.setPlannedHours(plan.getDailyHours());
            task.setStatus("pending");
            task.setVersion(0);
            task.setCreatedAt(LocalDateTime.now());
            task.setUpdatedAt(LocalDateTime.now());
            studyTaskMapper.insert(task);
            tasks.add(task);

            currentDate = currentDate.plusDays(1);
            dayIndex++;
        }

        PlanVO vo = convertToPlanVO(plan, tasks);
        return Result.success(vo);
    }

    @Transactional
    @Override
    public Result<Void> checkIn(Long taskId, CheckInDTO dto, Long userId) {
        if (dto.getDuration() == null || dto.getDuration() <= 0) {
            return Result.error("学习时长必须大于0");
        }

        StudyTask task = studyTaskMapper.selectById(taskId);
        if (task == null) {
            return Result.error("任务不存在");
        }

        StudyPlan plan = studyPlanMapper.selectById(task.getPlanId());
        if (plan == null || !plan.getUserId().equals(userId)) {
            return Result.error("无权操作该任务");
        }

        // 创建学习记录
        StudyRecord record = new StudyRecord();
        record.setTaskId(taskId);
        record.setUserId(userId);
        record.setStudyDate(LocalDate.now());
        record.setDuration(dto.getDuration());
        record.setContent(dto.getContent());
        record.setCheckInType("manual");
        record.setVersion(0);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        studyRecordMapper.insert(record);

        // 更新任务状态
        task.setStatus("completed");
        task.setCompletedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        studyTaskMapper.updateById(task);

        // 重新计算进度
        Float progress = calculateProgress(plan.getId());

        // 重新查询 plan，避免 calculateProgress 中更新的 overdue 状态被旧对象覆盖
        plan = studyPlanMapper.selectById(plan.getId());
        if (plan == null) {
            return Result.error("计划不存在");
        }

        if (progress >= 100.0f) {
            plan.setStatus("completed");
            plan.setProgress(100.0f);
        } else {
            plan.setProgress(progress);
        }
        plan.setUpdatedAt(LocalDateTime.now());
        studyPlanMapper.updateById(plan);

        return Result.success();
    }

    @Override
    public Float calculateProgress(Long planId) {
        QueryWrapper<StudyTask> wrapper = new QueryWrapper<>();
        wrapper.eq("plan_id", planId);
        List<StudyTask> tasks = studyTaskMapper.selectList(wrapper);

        if (tasks.isEmpty()) {
            return 0.0f;
        }

        long completedCount = tasks.stream()
                .filter(t -> "completed".equals(t.getStatus()))
                .count();

        float progress = (float) completedCount / tasks.size() * 100;
        progress = Math.round(progress * 10.0f) / 10.0f;

        StudyPlan plan = studyPlanMapper.selectById(planId);
        if (plan != null) {
            LocalDate today = LocalDate.now();
            if (today.isAfter(plan.getEndDate()) && progress < 100.0f) {
                plan.setStatus("overdue");
            }
            plan.setProgress(progress);
            plan.setUpdatedAt(LocalDateTime.now());
            studyPlanMapper.updateById(plan);
        }

        return progress;
    }

    @Override
    public Result<PlanVO> getPlanDetail(Long planId) {
        StudyPlan plan = studyPlanMapper.selectById(planId);
        if (plan == null) {
            return Result.error("计划不存在");
        }

        List<StudyTask> tasks = studyTaskMapper.findByPlanIdOrderByTaskDateAsc(planId);
        PlanVO vo = convertToPlanVO(plan, tasks);
        return Result.success(vo);
    }

    @Override
    public Result<List<PlanVO>> getPlanList(Long userId) {
        List<StudyPlan> plans = studyPlanMapper.findByUserIdOrderByCreatedAtDesc(userId);
        List<PlanVO> vos = plans.stream().map(p -> {
            PlanVO vo = new PlanVO();
            BeanUtils.copyProperties(p, vo);
            return vo;
        }).collect(Collectors.toList());
        return Result.success(vos);
    }

    private PlanVO convertToPlanVO(StudyPlan plan, List<StudyTask> tasks) {
        PlanVO vo = new PlanVO();
        BeanUtils.copyProperties(plan, vo);
        if (tasks != null) {
            List<TaskVO> taskVos = tasks.stream().map(t -> {
                TaskVO tv = new TaskVO();
                BeanUtils.copyProperties(t, tv);
                tv.setCheckedIn("completed".equals(t.getStatus()));
                return tv;
            }).collect(Collectors.toList());
            vo.setTasks(taskVos);
        }
        return vo;
    }
}
