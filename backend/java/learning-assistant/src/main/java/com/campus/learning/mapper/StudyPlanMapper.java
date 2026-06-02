package com.campus.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.learning.entity.StudyPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StudyPlanMapper extends BaseMapper<StudyPlan> {

    List<StudyPlan> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    /**
     * 查询当前用户未读的学习计划提醒
     * 条件：进行中、提醒日期已到、未标记已读
     */
    List<StudyPlan> findReminders(@Param("userId") Long userId);

    /**
     * 将指定计划的提醒标记为已读
     */
    int markReminderRead(@Param("id") Long id);
}
