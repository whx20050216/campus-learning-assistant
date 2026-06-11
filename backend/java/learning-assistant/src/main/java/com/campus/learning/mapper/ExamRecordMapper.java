package com.campus.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.learning.entity.ExamRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ExamRecordMapper extends BaseMapper<ExamRecord> {

    @Select("SELECT * FROM exam_records WHERE paper_id = #{paperId} AND user_id = #{userId} ORDER BY created_at DESC LIMIT 1")
    ExamRecord findLatestByPaperIdAndUserId(@Param("paperId") Long paperId, @Param("userId") Long userId);
}
