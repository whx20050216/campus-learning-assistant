package com.campus.learning.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.campus.learning.entity.Material;
import com.campus.learning.entity.StudyPlan;
import com.campus.learning.mapper.KeywordMapper;
import com.campus.learning.mapper.KnowledgePointMapper;
import com.campus.learning.mapper.MaterialMapper;
import com.campus.learning.mapper.OcrResultMapper;
import com.campus.learning.mapper.StudyPlanMapper;
import com.campus.learning.service.PlanService;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTaskService {

    private final MaterialMapper materialMapper;
    private final StudyPlanMapper studyPlanMapper;
    private final PlanService planService;
    private final OcrResultMapper ocrResultMapper;
    private final KeywordMapper keywordMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    /**
     * 每日凌晨 2:00：清理回收站（30天前用户删除的资料物理删除）
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupRecycleBin() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        QueryWrapper<Material> wrapper = new QueryWrapper<>();
        wrapper.isNotNull("deleted_at")
               .lt("deleted_at", threshold)
               .eq("deleted_by", "user");
        List<Material> list = materialMapper.selectList(wrapper);
        int count = 0;
        for (Material m : list) {
            // 先删 MinIO 物理文件，成功后再删 DB，避免 DB 已删文件残留
            boolean minioDeleted = false;
            try {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(m.getFileUrl())
                                .build()
                );
                minioDeleted = true;
            } catch (Exception e) {
                log.error("MinIO 文件删除失败: materialId={}, fileUrl={}", m.getId(), m.getFileUrl(), e);
            }

            if (!minioDeleted) {
                log.warn("跳过数据库删除，等待下次清理: materialId={}", m.getId());
                continue;
            }

            // 级联删除 ocr_result / keyword / knowledge_point
            QueryWrapper<com.campus.learning.entity.OcrResult> ocrWrapper = new QueryWrapper<>();
            ocrWrapper.eq("material_id", m.getId());
            ocrResultMapper.delete(ocrWrapper);

            QueryWrapper<com.campus.learning.entity.Keyword> kwWrapper = new QueryWrapper<>();
            kwWrapper.eq("material_id", m.getId());
            keywordMapper.delete(kwWrapper);

            QueryWrapper<com.campus.learning.entity.KnowledgePoint> kpWrapper = new QueryWrapper<>();
            kpWrapper.eq("material_id", m.getId());
            knowledgePointMapper.delete(kpWrapper);

            materialMapper.deleteById(m.getId());
            count++;
        }
        log.info("回收站定时清理完成，共清理 {} 条记录", count);
    }

    /**
     * 每日上午 9:00：发送临期学习计划提醒（剩余3天）
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void sendStudyReminders() {
        LocalDate today = LocalDate.now();
        QueryWrapper<StudyPlan> wrapper = new QueryWrapper<>();
        wrapper.eq("remind_date", today)
               .eq("reminder_sent", 0)
               .eq("status", "active");
        List<StudyPlan> plans = studyPlanMapper.selectList(wrapper);
        int count = 0;
        for (StudyPlan plan : plans) {
            UpdateWrapper<StudyPlan> update = new UpdateWrapper<>();
            update.eq("id", plan.getId()).set("reminder_sent", 1);
            studyPlanMapper.update(null, update);
            count++;
        }
        log.info("学习提醒定时任务完成，共提醒 {} 个计划", count);
    }

    /**
     * 每日凌晨 2:00：刷新所有进行中计划的进度，并标记超期
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void updatePlanProgress() {
        QueryWrapper<StudyPlan> wrapper = new QueryWrapper<>();
        wrapper.eq("status", "active");
        List<StudyPlan> plans = studyPlanMapper.selectList(wrapper);
        LocalDate today = LocalDate.now();
        for (StudyPlan plan : plans) {
            try {
                planService.calculateProgress(plan.getId());
                // 学习中断风险检测
                if (planService.hasInterruptionRisk(plan.getId())) {
                    log.warn("[学习中断风险] 计划{}连续3天未打卡", plan.getId());
                }
                // 超期检测
                if (today.isAfter(plan.getEndDate()) && plan.getProgress() < 100.0) {
                    UpdateWrapper<StudyPlan> update = new UpdateWrapper<>();
                    update.eq("id", plan.getId()).set("status", "overdue");
                    studyPlanMapper.update(null, update);
                    log.info("计划 {} 已标记为超期", plan.getId());
                }
            } catch (Exception e) {
                log.error("刷新计划 {} 进度失败: {}", plan.getId(), e.getMessage());
            }
        }
        log.info("计划进度刷新完成，共处理 {} 个计划", plans.size());
    }
}
