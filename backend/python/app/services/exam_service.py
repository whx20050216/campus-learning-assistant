import json
import logging
import os
import re
from typing import List, Optional

import requests

logger = logging.getLogger(__name__)


class ExamService:
    """智能组卷服务：调用智谱 ChatGLM-4 根据资料摘要和知识点生成试题"""

    def __init__(self):
        self.api_key = os.getenv("ZHIPU_API_KEY", "").strip()
        if not self.api_key:
            logger.error("ZHIPU_API_KEY 未配置，ExamService 不可用")
        self.base_url = "https://open.bigmodel.cn/api/paas/v4"
        self.model = "glm-4"
        self.timeout = 60  # 组卷需要更多时间，60秒

    def _headers(self) -> dict:
        return {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json"
        }

    def generate_exam(
        self,
        summaries: List[str],
        knowledge_points: List[str],
        question_count: int,
        types: List[str],
        difficulty: str
    ) -> List[dict]:
        """
        生成试卷题目，返回题目列表。
        每个题目为 dict：{type, content, options, answer, analysis, difficulty, knowledgePoint}
        """
        if not self.api_key:
            raise RuntimeError("ZHIPU_API_KEY 未配置")

        prompt = self._build_prompt(summaries, knowledge_points, question_count, types, difficulty)
        logger.info(f"开始生成试卷，题数={question_count}, 难度={difficulty}, 题型={types}")

        messages = [
            {"role": "system", "content": "你是一位资深的高校教师，擅长根据学习资料设计高质量的考试题目。你必须严格按要求的 JSON 格式输出，不要输出任何额外说明文字。"},
            {"role": "user", "content": prompt}
        ]

        payload = {
            "model": self.model,
            "messages": messages,
            "temperature": 0.3,
            "max_tokens": 4096
        }

        try:
            resp = requests.post(
                f"{self.base_url}/chat/completions",
                headers=self._headers(),
                json=payload,
                timeout=self.timeout
            )
            resp.raise_for_status()
            data = resp.json()
            raw = data["choices"][0]["message"]["content"].strip()
            logger.info(f"智谱返回长度: {len(raw)}")
            return self._parse_questions(raw)
        except requests.Timeout:
            logger.error("智谱 API 组卷超时")
            raise RuntimeError("AI 生成试卷超时，请稍后重试")
        except Exception as e:
            logger.error(f"智谱 API 调用失败: {e}")
            raise RuntimeError(f"AI 生成试卷失败: {e}")

    @staticmethod
    def _build_prompt(
        summaries: List[str],
        knowledge_points: List[str],
        question_count: int,
        types: List[str],
        difficulty: str
    ) -> str:
        difficulty_map = {
            "easy": "简单",
            "medium": "中等",
            "hard": "困难"
        }
        diff_cn = difficulty_map.get(difficulty, "中等")

        type_map = {
            "single": "单选题",
            "multiple": "多选题",
            "judge": "判断题",
            "essay": "简答题"
        }
        types_cn = [type_map.get(t, t) for t in types]

        summaries_text = "\n---\n".join(s[:500] for s in summaries if s.strip())
        kp_text = "\n".join(f"- {kp}" for kp in knowledge_points[:50] if kp.strip())

        types_instruction = """
题型规则：
- 单选题（single）：必须有4个选项（A/B/C/D），答案为单个字母如 "A"
- 多选题（multiple）：必须有4个选项（A/B/C/D），答案为多个字母如 "AB" 或 "ACD"
- 判断题（judge）：无 options 字段，答案为 "true" 或 "false"
- 简答题（essay）：无 options 字段，答案为参考答案要点
""".strip()

        prompt = f"""请根据以下学习资料生成 {question_count} 道 {diff_cn} 难度的考试题目。

【参考资料摘要】
{summaries_text}

【核心知识点】
{kp_text}

【题型要求】
{', '.join(types_cn)}

{types_instruction}

要求：
1. 题目内容必须严格基于参考资料和知识点，不能编造。
2. 每道题必须包含以下字段（严格 JSON 格式）：
   - type: single/multiple/judge/essay
   - content: 题目内容
   - options: 选项列表（单选/多选必填，判断题和简答题为 null）
   - answer: 标准答案
   - analysis: 解析说明
   - difficulty: easy/medium/hard
   - knowledgePoint: 本题对应的知识点名称
3. 单选题和多选题的 options 必须是 ["A. xxx", "B. xxx", "C. xxx", "D. xxx"] 格式。
4. 输出必须是纯 JSON，格式如下，不要包含 Markdown 标记或任何额外说明文字：
{{"questions":[{{"type":"single","content":"...","options":["A. ...","B. ...","C. ...","D. ..."],"answer":"A","analysis":"...","difficulty":"medium","knowledgePoint":"..."}}]}}
"""
        return prompt

    @staticmethod
    def _parse_questions(raw_text: str) -> List[dict]:
        """从 AI 返回的文本中解析 JSON 题目列表，含多重容错"""
        # 尝试提取 ```json ... ``` 代码块
        code_block = re.search(r"```(?:json)?\s*(.*?)\s*```", raw_text, re.DOTALL)
        if code_block:
            json_text = code_block.group(1).strip()
        else:
            json_text = raw_text.strip()

        # 尝试直接解析
        try:
            data = json.loads(json_text)
            questions = data.get("questions", data.get("data", []))
            if isinstance(questions, list) and questions:
                return ExamService._normalize_questions(questions)
        except json.JSONDecodeError:
            pass

        # 尝试正则提取最外层 { ... }
        json_match = re.search(r"(\{{.*\"questions\".*\}})", json_text, re.DOTALL)
        if json_match:
            try:
                data = json.loads(json_match.group(1))
                questions = data.get("questions", [])
                if isinstance(questions, list) and questions:
                    return ExamService._normalize_questions(questions)
            except json.JSONDecodeError:
                pass

        logger.error(f"无法解析 AI 返回的 JSON: {raw_text[:500]}")
        raise RuntimeError("AI 返回格式异常，请重试")

    @staticmethod
    def _normalize_questions(questions: List[dict]) -> List[dict]:
        """规范化题目字段，确保符合预期格式"""
        result = []
        for idx, q in enumerate(questions):
            normalized = {
                "type": q.get("type", "single"),
                "content": q.get("content", q.get("question", q.get("title", ""))),
                "options": q.get("options") if isinstance(q.get("options"), list) else None,
                "answer": str(q.get("answer", "")) if q.get("answer") is not None else None,
                "analysis": q.get("analysis", q.get("解析", "")),
                "difficulty": q.get("difficulty", "medium"),
                "knowledgePoint": q.get("knowledgePoint", q.get("knowledge_point", q.get("knowledge", "")))
            }
            if not normalized["content"]:
                logger.warning(f"第 {idx + 1} 题内容为空，已跳过")
                continue
            result.append(normalized)
        return result
