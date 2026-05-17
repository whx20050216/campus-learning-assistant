package com.campus.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.learning.entity.StudyTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StudyTaskMapper extends BaseMapper<StudyTask> {

    List<StudyTask> findByPlanIdOrderByTaskDateAsc(@Param("planId") Long planId);
}
