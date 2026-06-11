package com.campus.learning.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.learning.dto.GenerateExamDTO;
import com.campus.learning.dto.Result;
import com.campus.learning.dto.SubmitAnswersDTO;
import com.campus.learning.security.CurrentUserUtils;
import com.campus.learning.service.ExamService;
import com.campus.learning.vo.ExamPaperVO;
import com.campus.learning.vo.ExamQuestionVO;
import com.campus.learning.vo.ExamRecordVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/exam")
public class ExamController {

    @Autowired
    private ExamService examService;

    @PostMapping("/generate")
    public Result<ExamPaperVO> generate(@RequestBody GenerateExamDTO dto) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        try {
            ExamPaperVO vo = examService.generateExam(dto, userId);
            return Result.success(vo);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/generate-from-plan")
    public Result<ExamPaperVO> generateFromPlan(@RequestBody GenerateExamDTO dto) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        if (dto.getStudyPlanId() == null) {
            return Result.error("学习计划ID不能为空");
        }
        try {
            ExamPaperVO vo = examService.generateExamFromPlan(dto.getStudyPlanId(), dto.getCourseTag(), dto.getQuestionCount(), dto.getDifficulty(), userId);
            return Result.success(vo);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/papers")
    public Result<Page<ExamPaperVO>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        return Result.success(examService.getPaperList(userId, page, size));
    }

    @GetMapping("/papers/{id}")
    public Result<ExamPaperVO> detail(@PathVariable Long id) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        try {
            return Result.success(examService.getPaperDetail(id, userId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/papers/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        try {
            examService.updatePaper(id, userId, body.get("title"), body.get("status"));
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/papers/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        try {
            examService.deletePaper(id, userId);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/papers/{paperId}/regenerate/{questionId}")
    public Result<ExamQuestionVO> regenerate(
            @PathVariable Long paperId,
            @PathVariable Long questionId) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        try {
            return Result.success(examService.regenerateQuestion(paperId, questionId, userId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/papers/{id}/submit")
    public Result<ExamRecordVO> submit(
            @PathVariable Long id,
            @RequestBody SubmitAnswersDTO dto) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        try {
            return Result.success(examService.submitAnswers(id, userId, dto));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/papers/{id}/record")
    public Result<ExamRecordVO> record(@PathVariable Long id) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        try {
            return Result.success(examService.getExamRecord(id, userId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
