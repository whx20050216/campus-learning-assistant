package com.campus.learning.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.learning.dto.GenerateExamDTO;
import com.campus.learning.dto.SubmitAnswersDTO;
import com.campus.learning.vo.ExamPaperVO;
import com.campus.learning.vo.ExamQuestionVO;
import com.campus.learning.vo.ExamRecordVO;

public interface ExamService {

    ExamPaperVO generateExam(GenerateExamDTO dto, Long userId);

    ExamPaperVO generateExamFromPlan(Long planId, String courseTag, Integer questionCount, String difficulty, Long userId);

    Page<ExamPaperVO> getPaperList(Long userId, int page, int size);

    ExamPaperVO getPaperDetail(Long id, Long userId);

    void updatePaper(Long id, Long userId, String title, String status);

    void deletePaper(Long id, Long userId);

    ExamQuestionVO regenerateQuestion(Long paperId, Long questionId, Long userId);

    ExamRecordVO submitAnswers(Long paperId, Long userId, SubmitAnswersDTO dto);

    ExamRecordVO getExamRecord(Long paperId, Long userId);
}
