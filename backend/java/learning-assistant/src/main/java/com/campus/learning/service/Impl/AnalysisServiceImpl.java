package com.campus.learning.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.learning.dto.Result;
import com.campus.learning.entity.Material;
import com.campus.learning.entity.StudyPlan;
import com.campus.learning.entity.StudyRecord;
import com.campus.learning.mapper.MaterialMapper;
import com.campus.learning.mapper.StudyPlanMapper;
import com.campus.learning.mapper.StudyRecordMapper;
import com.campus.learning.service.AnalysisService;
import com.campus.learning.vo.DashboardVO;
import com.campus.learning.vo.PieItem;
import com.campus.learning.vo.ProgressItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AnalysisServiceImpl implements AnalysisService {

    @Autowired
    private MaterialMapper materialMapper;

    @Autowired
    private StudyPlanMapper studyPlanMapper;

    @Autowired
    private StudyRecordMapper studyRecordMapper;

    @Override
    public Result<DashboardVO> getDashboard(Long userId) {
        DashboardVO dashboard = new DashboardVO();

        // 1. 课程分布饼图数据
        QueryWrapper<Material> materialWrapper = new QueryWrapper<>();
        materialWrapper.eq("user_id", userId);
        List<Material> materials = materialMapper.selectList(materialWrapper);

        Map<String, Long> courseGroup = materials.stream()
                .filter(m -> m.getCourseTag() != null && !m.getCourseTag().isEmpty())
                .collect(Collectors.groupingBy(Material::getCourseTag, Collectors.counting()));

        List<PieItem> pieItems = new ArrayList<>();
        courseGroup.forEach((tag, count) -> {
            PieItem item = new PieItem();
            item.setName(tag);
            item.setValue(count.intValue());
            pieItems.add(item);
        });
        dashboard.setCourseDistribution(pieItems);

        // 2. 当前进行中的计划进度条
        QueryWrapper<StudyPlan> planWrapper = new QueryWrapper<>();
        planWrapper.eq("user_id", userId);
        planWrapper.eq("status", "active");
        planWrapper.orderByDesc("created_at");
        List<StudyPlan> activePlans = studyPlanMapper.selectList(planWrapper);

        List<ProgressItem> progressItems = activePlans.stream().map(p -> {
            ProgressItem item = new ProgressItem();
            item.setName(p.getName());
            item.setProgress(p.getProgress() != null ? p.getProgress() : 0.0f);
            return item;
        }).collect(Collectors.toList());
        dashboard.setCurrentProgress(progressItems);

        // 3. 本周 vs 上周学习时长
        LocalDate today = LocalDate.now();
        LocalDate thisMonday = today.with(DayOfWeek.MONDAY);
        LocalDate lastMonday = thisMonday.minusWeeks(1);
        LocalDate lastSunday = thisMonday.minusDays(1);

        List<StudyRecord> thisWeekRecords = studyRecordMapper.findByUserIdAndStudyDateBetween(userId, thisMonday, today);
        int weeklyDuration = thisWeekRecords.stream()
                .mapToInt(StudyRecord::getDuration)
                .sum();

        List<StudyRecord> lastWeekRecords = studyRecordMapper.findByUserIdAndStudyDateBetween(userId, lastMonday, lastSunday);
        int lastWeekDuration = lastWeekRecords.stream()
                .mapToInt(StudyRecord::getDuration)
                .sum();

        dashboard.setWeeklyDuration(weeklyDuration);
        dashboard.setLastWeekDuration(lastWeekDuration);

        return Result.success(dashboard);
    }
}
