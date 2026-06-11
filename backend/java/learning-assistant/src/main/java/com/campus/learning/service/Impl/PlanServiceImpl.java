package com.campus.learning.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.learning.dto.CheckInDTO;
import com.campus.learning.dto.PlanCreateDTO;
import com.campus.learning.dto.PlanUpdateDTO;
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
import com.campus.learning.vo.MaterialVO;
import com.campus.learning.vo.PlanReminderVO;
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
import java.util.Objects;
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
        List<StudyTask> tasks = generateTasks(plan, dto.getMaterialIds(), dto.getStartDate(), dto.getEndDate());

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

        LocalDate studyDate = dto.getStudyDate() != null ? dto.getStudyDate() : LocalDate.now();
        if (studyDate.isAfter(LocalDate.now())) {
            return Result.error("不能补录未来日期的学习记录");
        }
        if (!studyDate.equals(task.getTaskDate())) {
            return Result.error("打卡日期与任务日期不符");
        }

        // 创建学习记录
        StudyRecord record = new StudyRecord();
        record.setTaskId(taskId);
        record.setUserId(userId);
        record.setStudyDate(studyDate);
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

        // 计算总预设学习时间（小时）
        float totalPlannedHours = 0.0f;
        for (StudyTask task : tasks) {
            if (task.getPlannedHours() != null) {
                totalPlannedHours += task.getPlannedHours();
            }
        }

        // 计算已学习时间（小时）：duration 单位为分钟，需除以 60
        float completedHours = 0.0f;
        for (StudyTask task : tasks) {
            if ("completed".equals(task.getStatus())) {
                List<StudyRecord> records = studyRecordMapper.findByTaskId(task.getId());
                for (StudyRecord record : records) {
                    if (record.getDuration() != null) {
                        completedHours += record.getDuration() / 60.0f;
                    }
                }
            }
        }

        float progress = 0.0f;
        if (totalPlannedHours > 0) {
            progress = completedHours / totalPlannedHours * 100;
        }
        progress = Math.min(100.0f, Math.max(0.0f, progress));
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
            vo.setInterruptionRisk(hasInterruptionRisk(p.getId()) ? 1 : 0);
            return vo;
        }).collect(Collectors.toList());
        return Result.success(vos);
    }

    @Transactional
    @Override
    public Result<Void> deletePlan(Long planId, Long userId) {
        StudyPlan plan = studyPlanMapper.selectById(planId);
        if (plan == null) {
            return Result.error("计划不存在");
        }
        if (!plan.getUserId().equals(userId)) {
            return Result.error(403, "无权删除该计划");
        }
        // 级联删除学习记录
        List<StudyTask> tasks = studyTaskMapper.findByPlanIdOrderByTaskDateAsc(planId);
        for (StudyTask task : tasks) {
            QueryWrapper<StudyRecord> rw = new QueryWrapper<>();
            rw.eq("task_id", task.getId());
            studyRecordMapper.delete(rw);
        }
        // 级联删除学习任务
        QueryWrapper<StudyTask> taskWrapper = new QueryWrapper<>();
        taskWrapper.eq("plan_id", planId);
        studyTaskMapper.delete(taskWrapper);
        // 删除计划
        studyPlanMapper.deleteById(planId);
        return Result.success();
    }

    @Transactional
    @Override
    public Result<PlanVO> updatePlan(Long planId, PlanUpdateDTO dto, Long userId) {
        StudyPlan plan = studyPlanMapper.selectById(planId);
        if (plan == null) {
            return Result.error("计划不存在");
        }
        if (!plan.getUserId().equals(userId)) {
            return Result.error(403, "无权操作该计划");
        }

        boolean shouldRegenerate = false;
        List<Long> materialIds = dto.getMaterialIds();

        // 更新名称
        if (dto.getName() != null) {
            plan.setName(dto.getName().trim());
        }

        // 更新每日时长
        if (dto.getDailyHours() != null) {
            if (dto.getDailyHours() < 0.5f) {
                return Result.error("每日学习时长不能少于0.5小时");
            }
            plan.setDailyHours(dto.getDailyHours());
        }

        // 如果修改了截止日期
        if (dto.getEndDate() != null) {
            LocalDate today = LocalDate.now();
            if (dto.getEndDate().isBefore(today)) {
                return Result.error("结束日期不能早于今天");
            }
            if (plan.getStartDate() != null && plan.getStartDate().isAfter(dto.getEndDate())) {
                return Result.error("开始日期不能晚于结束日期");
            }
            plan.setEndDate(dto.getEndDate());
            plan.setRemindDate(dto.getEndDate().minusDays(3));
            shouldRegenerate = true;
        }

        // 如果传了 materialIds
        if (materialIds != null && !materialIds.isEmpty()) {
            shouldRegenerate = true;
        }

        if (shouldRegenerate) {
            // 确定 materialIds
            if (materialIds == null || materialIds.isEmpty()) {
                // 从旧任务中提取
                List<StudyTask> oldTasks = studyTaskMapper.findByPlanIdOrderByTaskDateAsc(planId);
                materialIds = oldTasks.stream()
                        .map(StudyTask::getMaterialId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .collect(Collectors.toList());
                if (materialIds.isEmpty()) {
                    return Result.error("无法获取原资料信息，请重新选择资料");
                }
            }

            // 重新计算总页数
            int totalPages = 0;
            List<Material> materials = materialMapper.selectBatchIds(materialIds);
            for (Material material : materials) {
                if (material.getPages() != null && material.getPages() > 0) {
                    totalPages += material.getPages();
                } else if (material.getFileSize() != null && material.getFileSize() > 0) {
                    long mb = material.getFileSize() / (1024 * 1024);
                    if (mb == 0) {
                        mb = 1;
                    }
                    totalPages += (int) (mb * 10);
                } else {
                    totalPages += 10;
                }
            }
            plan.setTotalPages(totalPages);

            // 删除旧任务
            QueryWrapper<StudyTask> taskWrapper = new QueryWrapper<>();
            taskWrapper.eq("plan_id", planId);
            studyTaskMapper.delete(taskWrapper);

            // 重新生成任务
            List<StudyTask> tasks = generateTasks(plan, materialIds, plan.getStartDate(), plan.getEndDate());
            plan.setUpdatedAt(LocalDateTime.now());
            studyPlanMapper.updateById(plan);

            PlanVO vo = convertToPlanVO(plan, tasks);
            return Result.success(vo);
        } else {
            // 只更新 plan 字段，如果 dailyHours 变了也更新所有任务的 plannedHours
            if (dto.getDailyHours() != null) {
                QueryWrapper<StudyTask> taskWrapper = new QueryWrapper<>();
                taskWrapper.eq("plan_id", planId);
                List<StudyTask> tasks = studyTaskMapper.selectList(taskWrapper);
                for (StudyTask task : tasks) {
                    task.setPlannedHours(plan.getDailyHours());
                    task.setUpdatedAt(LocalDateTime.now());
                    studyTaskMapper.updateById(task);
                }
            }
            plan.setUpdatedAt(LocalDateTime.now());
            studyPlanMapper.updateById(plan);

            List<StudyTask> tasks = studyTaskMapper.findByPlanIdOrderByTaskDateAsc(planId);
            PlanVO vo = convertToPlanVO(plan, tasks);
            return Result.success(vo);
        }
    }

    private List<StudyTask> generateTasks(StudyPlan plan, List<Long> materialIds, LocalDate startDate, LocalDate endDate) {
        List<StudyTask> tasks = new ArrayList<>();
        LocalDate currentDate = startDate;
        int dayIndex = 0;
        while (!currentDate.isAfter(endDate)) {
            StudyTask task = new StudyTask();
            task.setPlanId(plan.getId());
            task.setMaterialId(materialIds.get(dayIndex % materialIds.size()));
            task.setTaskName("第" + (dayIndex + 1) + "天学习任务");
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
        return tasks;
    }

    private PlanVO convertToPlanVO(StudyPlan plan, List<StudyTask> tasks) {
        PlanVO vo = new PlanVO();
        BeanUtils.copyProperties(plan, vo);
        vo.setInterruptionRisk(hasInterruptionRisk(plan.getId()) ? 1 : 0);
        if (tasks != null) {
            List<TaskVO> taskVos = tasks.stream().map(t -> {
                TaskVO tv = new TaskVO();
                BeanUtils.copyProperties(t, tv);
                tv.setCheckedIn("completed".equals(t.getStatus()));
                return tv;
            }).collect(Collectors.toList());
            vo.setTasks(taskVos);

            // 组装计划包含的资料列表（去重、过滤已删除）
            List<Long> materialIds = tasks.stream()
                    .map(StudyTask::getMaterialId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            if (!materialIds.isEmpty()) {
                List<Material> materials = materialMapper.selectBatchIds(materialIds);
                List<MaterialVO> materialVos = materials.stream()
                        .filter(m -> m.getDeletedAt() == null)
                        .map(m -> {
                            MaterialVO mv = new MaterialVO();
                            BeanUtils.copyProperties(m, mv);
                            return mv;
                        })
                        .collect(Collectors.toList());
                vo.setMaterials(materialVos);
            }
        }
        return vo;
    }

    @Override
    public boolean hasInterruptionRisk(Long planId) {
        LocalDate today = LocalDate.now();

        QueryWrapper<StudyTask> wrapper = new QueryWrapper<>();
        wrapper.eq("plan_id", planId)
               .ge("task_date", today.minusDays(2))
               .le("task_date", today)
               .orderByAsc("task_date");
        List<StudyTask> recentTasks = studyTaskMapper.selectList(wrapper);

        if (recentTasks.isEmpty()) {
            return false;
        }

        for (StudyTask task : recentTasks) {
            QueryWrapper<StudyRecord> recordWrapper = new QueryWrapper<>();
            recordWrapper.eq("task_id", task.getId());
            Long count = studyRecordMapper.selectCount(recordWrapper);
            if (count != null && count > 0) {
                return false;
            }
        }

        return true;
    }

    @Override
    public Result<List<PlanReminderVO>> getReminders(Long userId) {
        List<StudyPlan> plans = studyPlanMapper.findReminders(userId);
        LocalDate today = LocalDate.now();
        List<PlanReminderVO> vos = plans.stream().map(p -> {
            PlanReminderVO vo = new PlanReminderVO();
            vo.setPlanId(p.getId());
            vo.setPlanName(p.getName());
            vo.setRemindDate(p.getRemindDate());
            // daysLeft: 提醒日期到今天的差值（负值表示已过期）
            vo.setDaysLeft((int) ChronoUnit.DAYS.between(today, p.getRemindDate()));
            return vo;
        }).collect(Collectors.toList());
        return Result.success(vos);
    }

    @Transactional
    @Override
    public Result<Void> markReminderRead(Long planId, Long userId) {
        StudyPlan plan = studyPlanMapper.selectById(planId);
        if (plan == null) {
            return Result.error("计划不存在");
        }
        if (!plan.getUserId().equals(userId)) {
            return Result.error(403, "无权操作该计划");
        }
        studyPlanMapper.markReminderRead(planId);
        return Result.success();
    }
}
