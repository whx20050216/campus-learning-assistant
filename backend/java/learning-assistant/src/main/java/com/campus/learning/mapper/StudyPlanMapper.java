package com.campus.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.learning.entity.StudyPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StudyPlanMapper extends BaseMapper<StudyPlan> {

    List<StudyPlan> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
}
