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
    public Result<DashboardVO> getDashboard(Long userId, String timeRange) {
        DashboardVO dashboard = new DashboardVO();
        LocalDate today = LocalDate.now();

        // 根据 timeRange 计算当前周期和上一周期的起止日期
        LocalDate currentStart;
        LocalDate prevStart;
        LocalDate prevEnd;

        if ("month".equals(timeRange)) {
            currentStart = today.minusDays(29);  // 近30天（含今天）
            prevStart = today.minusDays(59);     // 上30天起始
            prevEnd = today.minusDays(30);       // 上30天结束
        } else {
            // 默认 week：近7天（含今天）
            currentStart = today.minusDays(6);
            prevStart = today.minusDays(13);
            prevEnd = today.minusDays(7);
        }

        // 1. 课程分布饼图数据 + 总资料数（按时间范围过滤）
        QueryWrapper<Material> materialWrapper = new QueryWrapper<>();
        materialWrapper.eq("user_id", userId);
        materialWrapper.isNull("deleted_at");
        materialWrapper.ge("created_at", currentStart.atStartOfDay());
        List<Material> materials = materialMapper.selectList(materialWrapper);
        dashboard.setMaterialCount(materials.size());

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

        // 2. 当前进行中的计划进度条（与时间维度无关）
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

        // 3. 当前周期 vs 上一周期学习时长
        List<StudyRecord> currentRecords = studyRecordMapper.findByUserIdAndStudyDateBetween(userId, currentStart, today);
        int currentDuration = currentRecords.stream()
                .mapToInt(StudyRecord::getDuration)
                .sum();

        List<StudyRecord> prevRecords = studyRecordMapper.findByUserIdAndStudyDateBetween(userId, prevStart, prevEnd);
        int prevDuration = prevRecords.stream()
                .mapToInt(StudyRecord::getDuration)
                .sum();

        dashboard.setWeeklyDuration(currentDuration);
        dashboard.setLastWeekDuration(prevDuration);

        return Result.success(dashboard);
    }
}
