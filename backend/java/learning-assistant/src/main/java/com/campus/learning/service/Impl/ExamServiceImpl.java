package com.campus.learning.service.Impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.learning.dto.GenerateExamDTO;
import com.campus.learning.dto.SubmitAnswersDTO;
import com.campus.learning.entity.*;
import com.campus.learning.mapper.*;
import com.campus.learning.service.ExamService;
import com.campus.learning.vo.ExamPaperVO;
import com.campus.learning.vo.ExamQuestionVO;
import com.campus.learning.vo.ExamRecordVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ExamServiceImpl implements ExamService {

    @Autowired
    private ExamPaperMapper examPaperMapper;

    @Autowired
    private ExamQuestionMapper examQuestionMapper;

    @Autowired
    private MaterialMapper materialMapper;

    @Autowired
    private OcrResultMapper ocrResultMapper;

    @Autowired
    private KnowledgePointMapper knowledgePointMapper;

    @Autowired
    private StudyPlanMapper studyPlanMapper;

    @Autowired
    private StudyTaskMapper studyTaskMapper;

    @Autowired
    private ExamRecordMapper examRecordMapper;

    @Value("${python.api.url:http://localhost:8000}")
    private String pythonApiUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public ExamPaperVO generateExam(GenerateExamDTO dto, Long userId) {
        // 1. 参数校验
        if (dto.getMaterialIds() == null || dto.getMaterialIds().isEmpty()) {
            throw new RuntimeException("请至少选择一份资料");
        }
        if (dto.getMaterialIds().size() > 5) {
            throw new RuntimeException("最多选择5份资料");
        }
        if (dto.getCourseTag() == null || dto.getCourseTag().trim().isEmpty()) {
            throw new RuntimeException("课程标签不能为空");
        }
        if (dto.getQuestionCount() == null || dto.getQuestionCount() < 1 || dto.getQuestionCount() > 50) {
            throw new RuntimeException("题目数量需在1-50之间");
        }

        // 2. 确认资料存在且属于当前用户
        List<Material> materials = materialMapper.selectBatchIds(dto.getMaterialIds());
        if (materials.size() != dto.getMaterialIds().size()) {
            throw new RuntimeException("部分资料不存在");
        }
        for (Material m : materials) {
            if (!m.getUserId().equals(userId)) {
                throw new RuntimeException("无权使用资料: " + m.getTitle());
            }
        }

        // 3. 获取资料摘要和知识点
        List<String> summaries = new ArrayList<>();
        List<String> knowledgePoints = new ArrayList<>();
        for (Long materialId : dto.getMaterialIds()) {
            OcrResult ocr = ocrResultMapper.findByMaterialId(materialId);
            if (ocr != null && ocr.getSummary() != null && !ocr.getSummary().isEmpty()) {
                summaries.add(ocr.getSummary());
            }
            List<KnowledgePoint> kps = knowledgePointMapper.findByMaterialId(materialId);
            if (kps != null) {
                for (KnowledgePoint kp : kps) {
                    if (kp.getContent() != null) {
                        knowledgePoints.add(kp.getContent());
                    }
                }
            }
        }

        if (summaries.isEmpty() && knowledgePoints.isEmpty()) {
            throw new RuntimeException("资料内容不足，无法生成题目");
        }

        // 4. 调用 Python AI 生成试卷
        List<Map<String, Object>> questions = callPythonGenerateExam(
                summaries, knowledgePoints, dto.getQuestionCount(), dto.getTypes(), dto.getDifficulty()
        );

        if (questions == null || questions.isEmpty()) {
            throw new RuntimeException("题目生成失败，请稍后重试");
        }

        // 5. 保存试卷
        ExamPaper paper = new ExamPaper();
        paper.setUserId(userId);
        paper.setStudyPlanId(dto.getStudyPlanId());
        paper.setCourseTag(dto.getCourseTag().trim());
        paper.setTitle(dto.getCourseTag().trim() + " 智能组卷");
        paper.setMaterialIds(dto.getMaterialIds().stream().map(String::valueOf).collect(Collectors.joining(",")));
        paper.setQuestionCount(questions.size());
        paper.setDifficulty(dto.getDifficulty());
        paper.setStatus("published");
        examPaperMapper.insert(paper);

        // 6. 保存题目
        int sortOrder = 0;
        for (Map<String, Object> q : questions) {
            ExamQuestion question = new ExamQuestion();
            question.setPaperId(paper.getId());
            question.setType((String) q.get("type"));
            question.setContent((String) q.get("content"));
            Object optionsObj = q.get("options");
            if (optionsObj != null) {
                try {
                    question.setOptions(objectMapper.writeValueAsString(optionsObj));
                } catch (Exception e) {
                    question.setOptions(optionsObj.toString());
                }
            }
            question.setAnswer((String) q.get("answer"));
            question.setAnalysis((String) q.get("analysis"));
            question.setDifficulty((String) q.get("difficulty"));
            question.setKnowledgePoint((String) q.get("knowledgePoint"));
            question.setSortOrder(sortOrder++);
            examQuestionMapper.insert(question);
        }

        return getPaperDetail(paper.getId(), userId);
    }

    @Override
    @Transactional
    public ExamPaperVO generateExamFromPlan(Long planId, String courseTag, Integer questionCount, String difficulty, Long userId) {
        // 1. 验证计划
        StudyPlan plan = studyPlanMapper.selectById(planId);
        if (plan == null || !plan.getUserId().equals(userId)) {
            throw new RuntimeException("学习计划不存在或无权访问");
        }

        // 2. 获取计划关联的资料
        List<Long> materialIds = getMaterialIdsByPlanId(planId);
        if (materialIds.isEmpty()) {
            throw new RuntimeException("该学习计划没有关联资料");
        }

        // 3. 推断 course_tag
        String inferredTag = inferCourseTag(materialIds);
        if (courseTag != null && !courseTag.isEmpty()) {
            inferredTag = courseTag;
        }
        if (inferredTag == null || inferredTag.isEmpty()) {
            throw new RuntimeException("无法推断课程标签，请手动选择");
        }

        // 4. 构建 DTO 调用通用组卷
        GenerateExamDTO dto = new GenerateExamDTO();
        dto.setMaterialIds(materialIds);
        dto.setCourseTag(inferredTag);
        dto.setQuestionCount(questionCount != null && questionCount > 0 ? questionCount : 10);
        dto.setTypes(Arrays.asList("single", "multiple", "judge", "essay"));
        dto.setDifficulty(difficulty != null && !difficulty.isEmpty() ? difficulty : "medium");
        dto.setStudyPlanId(planId);

        return generateExam(dto, userId);
    }

    @Override
    public Page<ExamPaperVO> getPaperList(Long userId, int page, int size) {
        Page<ExamPaper> mpPage = new Page<>(page + 1, size);
        // 使用 MyBatis-Plus QueryWrapper 分页查询
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ExamPaper> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.orderByDesc("created_at");
        Page<ExamPaper> result = examPaperMapper.selectPage(mpPage, wrapper);

        Page<ExamPaperVO> voPage = new Page<>(result.getCurrent(), result.getSize());
        voPage.setTotal(result.getTotal());
        voPage.setPages(result.getPages());
        voPage.setRecords(result.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public ExamPaperVO getPaperDetail(Long id, Long userId) {
        ExamPaper paper = examPaperMapper.selectById(id);
        if (paper == null || !paper.getUserId().equals(userId)) {
            throw new RuntimeException("试卷不存在或无权访问");
        }
        ExamPaperVO vo = convertToVO(paper);
        List<ExamQuestion> questions = examQuestionMapper.selectByPaperId(id);
        vo.setQuestions(questions.stream().map(this::convertQuestionToVO).collect(Collectors.toList()));
        return vo;
    }

    @Override
    @Transactional
    public void updatePaper(Long id, Long userId, String title, String status) {
        ExamPaper paper = examPaperMapper.selectById(id);
        if (paper == null || !paper.getUserId().equals(userId)) {
            throw new RuntimeException("试卷不存在或无权访问");
        }
        if (title != null && !title.trim().isEmpty()) {
            paper.setTitle(title.trim());
        }
        if (status != null && !status.trim().isEmpty()) {
            paper.setStatus(status.trim());
        }
        examPaperMapper.updateById(paper);
    }

    @Override
    @Transactional
    public void deletePaper(Long id, Long userId) {
        ExamPaper paper = examPaperMapper.selectById(id);
        if (paper == null || !paper.getUserId().equals(userId)) {
            throw new RuntimeException("试卷不存在或无权访问");
        }
        examQuestionMapper.deleteByPaperId(id);
        examPaperMapper.deleteById(id);
    }

    @Override
    @Transactional
    public ExamQuestionVO regenerateQuestion(Long paperId, Long questionId, Long userId) {
        ExamPaper paper = examPaperMapper.selectById(paperId);
        if (paper == null || !paper.getUserId().equals(userId)) {
            throw new RuntimeException("试卷不存在或无权访问");
        }

        ExamQuestion oldQuestion = examQuestionMapper.selectById(questionId);
        if (oldQuestion == null || !oldQuestion.getPaperId().equals(paperId)) {
            throw new RuntimeException("题目不存在");
        }

        // 重新生成该题目
        List<Long> materialIdList = Arrays.stream(paper.getMaterialIds().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toList());

        List<String> summaries = new ArrayList<>();
        List<String> knowledgePoints = new ArrayList<>();
        for (Long materialId : materialIdList) {
            OcrResult ocr = ocrResultMapper.findByMaterialId(materialId);
            if (ocr != null && ocr.getSummary() != null) {
                summaries.add(ocr.getSummary());
            }
            List<KnowledgePoint> kps = knowledgePointMapper.findByMaterialId(materialId);
            if (kps != null) {
                for (KnowledgePoint kp : kps) {
                    if (kp.getContent() != null) {
                        knowledgePoints.add(kp.getContent());
                    }
                }
            }
        }

        // 调用 Python 只生成 1 道题
        List<Map<String, Object>> questions = callPythonGenerateExam(
                summaries, knowledgePoints, 1,
                Collections.singletonList(oldQuestion.getType()),
                oldQuestion.getDifficulty()
        );

        if (questions == null || questions.isEmpty()) {
            throw new RuntimeException("重新生成题目失败");
        }

        Map<String, Object> q = questions.get(0);
        oldQuestion.setContent((String) q.get("content"));
        Object optionsObj = q.get("options");
        if (optionsObj != null) {
            try {
                oldQuestion.setOptions(objectMapper.writeValueAsString(optionsObj));
            } catch (Exception e) {
                oldQuestion.setOptions(optionsObj.toString());
            }
        } else {
            oldQuestion.setOptions(null);
        }
        oldQuestion.setAnswer((String) q.get("answer"));
        oldQuestion.setAnalysis((String) q.get("analysis"));
        oldQuestion.setDifficulty((String) q.get("difficulty"));
        oldQuestion.setKnowledgePoint((String) q.get("knowledgePoint"));
        examQuestionMapper.updateById(oldQuestion);

        return convertQuestionToVO(oldQuestion);
    }

    @Override
    @Transactional
    public ExamRecordVO submitAnswers(Long paperId, Long userId, SubmitAnswersDTO dto) {
        ExamPaper paper = examPaperMapper.selectById(paperId);
        if (paper == null || !paper.getUserId().equals(userId)) {
            throw new RuntimeException("试卷不存在或无权访问");
        }

        List<ExamQuestion> questions = examQuestionMapper.selectByPaperId(paperId);
        if (questions.isEmpty()) {
            throw new RuntimeException("试卷中没有题目");
        }

        Map<Long, String> answers = dto.getAnswers();
        int correctCount = 0;
        int totalScorable = 0;

        for (ExamQuestion q : questions) {
            String userAnswer = answers != null ? answers.get(q.getId()) : null;
            if (userAnswer == null || userAnswer.trim().isEmpty()) {
                continue;
            }
            if ("essay".equals(q.getType())) {
                continue; // 简答题不自动评分
            }
            totalScorable++;
            String correctAnswer = q.getAnswer();
            if (correctAnswer == null) continue;

            boolean isCorrect = false;
            if ("multiple".equals(q.getType())) {
                // 多选：排序后比对
                String u = userAnswer.chars().sorted().collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
                String c = correctAnswer.chars().sorted().collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
                isCorrect = u.equalsIgnoreCase(c);
            } else {
                isCorrect = userAnswer.trim().equalsIgnoreCase(correctAnswer.trim());
            }
            if (isCorrect) correctCount++;
        }

        // 百分制得分（只算可自动评分的题）
        int score = totalScorable > 0 ? Math.round(correctCount * 100.0f / totalScorable) : 0;

        ExamRecord record = new ExamRecord();
        record.setPaperId(paperId);
        record.setUserId(userId);
        try {
            record.setAnswers(answers != null ? new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(answers) : "{}");
        } catch (Exception e) {
            record.setAnswers("{}");
        }
        record.setScore(score);
        record.setCorrectCount(correctCount);
        record.setTotalCount(questions.size());
        record.setSpentTimeSeconds(dto.getSpentTimeSeconds() != null ? dto.getSpentTimeSeconds() : 0);
        record.setStatus("submitted");
        examRecordMapper.insert(record);

        // 更新试卷状态
        paper.setStatus("completed");
        examPaperMapper.updateById(paper);

        return convertRecordToVO(record);
    }

    @Override
    public ExamRecordVO getExamRecord(Long paperId, Long userId) {
        ExamPaper paper = examPaperMapper.selectById(paperId);
        if (paper == null || !paper.getUserId().equals(userId)) {
            throw new RuntimeException("试卷不存在或无权访问");
        }
        ExamRecord record = examRecordMapper.findLatestByPaperIdAndUserId(paperId, userId);
        if (record == null) {
            throw new RuntimeException("暂无答题记录");
        }
        return convertRecordToVO(record);
    }

    // ========== 私有方法 ==========

    private List<Map<String, Object>> callPythonGenerateExam(
            List<String> summaries, List<String> knowledgePoints,
            int questionCount, List<String> types, String difficulty) {
        try {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(5000);
            factory.setReadTimeout(70000); // 70秒超时，覆盖Python的60秒
            RestTemplate rt = new RestTemplate(factory);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("summaries", summaries);
            body.put("knowledgePoints", knowledgePoints);
            body.put("questionCount", questionCount);
            body.put("types", types);
            body.put("difficulty", difficulty);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            log.info("调用 Python 组卷服务，资料摘要数: {}, 知识点数: {}, 题目数: {}",
                    summaries.size(), knowledgePoints.size(), questionCount);

            ResponseEntity<Map> response = rt.postForEntity(
                    pythonApiUrl + "/ai/generate",
                    request,
                    Map.class
            );

            Map result = response.getBody();
            if (result != null && result.get("questions") != null) {
                return (List<Map<String, Object>>) result.get("questions");
            }
        } catch (Exception e) {
            log.error("Python 组卷调用失败: {}", e.getMessage());
        }
        return null;
    }

    private List<Long> getMaterialIdsByPlanId(Long planId) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<StudyTask> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.eq("plan_id", planId);
        wrapper.isNotNull("material_id");
        List<StudyTask> tasks = studyTaskMapper.selectList(wrapper);
        return tasks.stream()
                .map(StudyTask::getMaterialId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private String inferCourseTag(List<Long> materialIds) {
        // 获取所有资料的 course_tag，取最频繁的
        Map<String, Integer> tagCount = new HashMap<>();
        for (Long id : materialIds) {
            Material m = materialMapper.selectById(id);
            if (m != null && m.getCourseTag() != null && !m.getCourseTag().isEmpty()) {
                tagCount.merge(m.getCourseTag(), 1, Integer::sum);
            }
        }
        return tagCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private ExamPaperVO convertToVO(ExamPaper paper) {
        ExamPaperVO vo = new ExamPaperVO();
        vo.setId(paper.getId());
        vo.setUserId(paper.getUserId());
        vo.setStudyPlanId(paper.getStudyPlanId());
        vo.setCourseTag(paper.getCourseTag());
        vo.setTitle(paper.getTitle());
        vo.setMaterialIds(paper.getMaterialIds());
        vo.setQuestionCount(paper.getQuestionCount());
        vo.setDifficulty(paper.getDifficulty());
        vo.setStatus(paper.getStatus());
        vo.setCreatedAt(paper.getCreatedAt());
        return vo;
    }

    private ExamQuestionVO convertQuestionToVO(ExamQuestion q) {
        ExamQuestionVO vo = new ExamQuestionVO();
        vo.setId(q.getId());
        vo.setPaperId(q.getPaperId());
        vo.setMaterialId(q.getMaterialId());
        vo.setType(q.getType());
        vo.setContent(q.getContent());
        String optionsJson = q.getOptions();
        if (optionsJson != null && !optionsJson.isEmpty()) {
            try {
                vo.setOptions(objectMapper.readValue(optionsJson, new TypeReference<java.util.List<String>>() {}));
            } catch (Exception e) {
                log.warn("解析 options JSON 失败: {}", optionsJson);
                vo.setOptions(null);
            }
        }
        vo.setAnswer(q.getAnswer());
        vo.setAnalysis(q.getAnalysis());
        vo.setDifficulty(q.getDifficulty());
        vo.setKnowledgePoint(q.getKnowledgePoint());
        vo.setSortOrder(q.getSortOrder());
        return vo;
    }

    private ExamRecordVO convertRecordToVO(ExamRecord r) {
        ExamRecordVO vo = new ExamRecordVO();
        vo.setId(r.getId());
        vo.setPaperId(r.getPaperId());
        vo.setUserId(r.getUserId());
        vo.setAnswers(r.getAnswers());
        vo.setScore(r.getScore());
        vo.setCorrectCount(r.getCorrectCount());
        vo.setTotalCount(r.getTotalCount());
        vo.setSpentTimeSeconds(r.getSpentTimeSeconds());
        vo.setStatus(r.getStatus());
        vo.setCreatedAt(r.getCreatedAt());
        return vo;
    }
}
