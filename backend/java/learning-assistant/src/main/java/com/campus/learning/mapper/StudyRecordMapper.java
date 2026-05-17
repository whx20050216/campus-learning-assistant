package com.campus.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.learning.entity.StudyRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface StudyRecordMapper extends BaseMapper<StudyRecord> {

    List<StudyRecord> findByUserIdAndStudyDateBetween(@Param("userId") Long userId,
                                                       @Param("startDate") LocalDate startDate,
                                                       @Param("endDate") LocalDate endDate);

    List<StudyRecord> findByTaskId(@Param("taskId") Long taskId);
}
