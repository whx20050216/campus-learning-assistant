package com.campus.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.learning.entity.ExamQuestion;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ExamQuestionMapper extends BaseMapper<ExamQuestion> {

    @Select("SELECT * FROM exam_questions WHERE paper_id = #{paperId} ORDER BY sort_order ASC")
    List<ExamQuestion> selectByPaperId(@Param("paperId") Long paperId);

    @Delete("DELETE FROM exam_questions WHERE paper_id = #{paperId}")
    int deleteByPaperId(@Param("paperId") Long paperId);
}
