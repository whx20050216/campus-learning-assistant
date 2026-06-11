package com.campus.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.learning.entity.ExamPaper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ExamPaperMapper extends BaseMapper<ExamPaper> {

    @Select("SELECT * FROM exam_papers WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<ExamPaper> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM exam_papers WHERE user_id = #{userId} ORDER BY created_at DESC")
    Page<ExamPaper> selectPageByUserId(Page<ExamPaper> page, @Param("userId") Long userId);
}
