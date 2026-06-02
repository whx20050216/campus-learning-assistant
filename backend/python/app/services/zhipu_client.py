import os
import base64
import logging
import re
from typing import Optional

import requests

logger = logging.getLogger(__name__)


class ZhipuApiException(Exception):
    """智谱 API 调用异常"""
    pass


class ZhipuClient:
    """
    智谱 API 客户端封装：
    - GLM-4V-Flash：视觉增强（传图片）
    - ChatGLM-4：文本增强（传文本）
    """

    def __init__(self):
        self.api_key = os.getenv("ZHIPU_API_KEY", "").strip()
        if not self.api_key:
            raise ZhipuApiException("ZHIPU_API_KEY 未配置，无法初始化 ZhipuClient")

        self.base_url = "https://open.bigmodel.cn/api/paas/v4"
        self.timeout = 5  # 5 秒超时熔断
        self.model_vision = "glm-4v-flash"
        self.model_chat = "glm-4"

    def _headers(self) -> dict:
        return {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json"
        }

    def enhance(
        self,
        image_base64: Optional[str] = None,
        text: Optional[str] = None
    ) -> dict:
        """
        调用智谱 API 进行增强识别/理解。
        优先使用 GLM-4V-Flash（有图片时），否则使用 ChatGLM-4。
        返回 {"text": 增强后的文本, "summary": 深度摘要或 None}
        """
        try:
            if image_base64:
                raw = self._call_vision(image_base64, text)
            elif text:
                raw = self._call_chat(text)
            else:
                raise ZhipuApiException("enhance 调用必须提供 image_base64 或 text 至少一个")
            return self._parse_response(raw)
        except requests.Timeout:
            logger.warning("Zhipu API 调用超时（5s），触发熔断")
            raise ZhipuApiException("智谱 API 超时")
        except Exception as e:
            logger.warning(f"Zhipu API 调用异常: {e}")
            raise ZhipuApiException(f"智谱 API 异常: {e}")

    def _call_vision(self, image_base64: str, text: Optional[str] = None) -> str:
        """调用 GLM-4V-Flash 视觉模型"""
        prompt = (
            "请对这张图片进行OCR文字识别，输出图片中的全部文字内容，"
            "并生成一段150字左右的深度摘要。"
            "请严格按以下格式输出：\n"
            "【校正文本】\n"
            "（识别出的完整文本）\n"
            "【深度摘要】\n"
            "（150字左右的摘要）"
        )
        if text:
            prompt += f"\n参考文本：{text[:500]}"

        messages = [
            {
                "role": "user",
                "content": [
                    {"type": "text", "text": prompt},
                    {"type": "image_url", "image_url": {"url": f"data:image/png;base64,{image_base64}"}}
                ]
            }
        ]

        payload = {
            "model": self.model_vision,
            "messages": messages,
            "temperature": 0.1,
            "max_tokens": 2048
        }

        resp = requests.post(
            f"{self.base_url}/chat/completions",
            headers=self._headers(),
            json=payload,
            timeout=self.timeout
        )
        resp.raise_for_status()
        data = resp.json()
        return data["choices"][0]["message"]["content"]

    def _call_chat(self, text: str) -> str:
        """调用 ChatGLM-4 文本模型"""
        prompt = (
            "请对以下OCR识别文本进行校正、补全和优化，"
            "并生成一段150字左右的深度摘要。"
            "请严格按以下格式输出：\n"
            "【校正文本】\n"
            "（校正后的完整文本）\n"
            "【深度摘要】\n"
            "（150字左右的摘要）\n\n"
            f"{text[:2000]}"
        )

        messages = [
            {"role": "system", "content": "你是一个专业的OCR文本校正助手。"},
            {"role": "user", "content": prompt}
        ]

        payload = {
            "model": self.model_chat,
            "messages": messages,
            "temperature": 0.1,
            "max_tokens": 2048
        }

        resp = requests.post(
            f"{self.base_url}/chat/completions",
            headers=self._headers(),
            json=payload,
            timeout=self.timeout
        )
        resp.raise_for_status()
        data = resp.json()
        return data["choices"][0]["message"]["content"]

    def generate_summary(self, text: str) -> str:
        """
        调用 ChatGLM-4 生成 150 字深度摘要。
        返回摘要文本；失败或超时返回空字符串，让调用方降级本地摘要。
        """
        try:
            prompt = (
                "请对以下文本生成一段150字左右的深度摘要，"
                "要求准确概括核心内容、语言通顺：\n\n"
                f"{text[:3000]}"
            )

            messages = [
                {"role": "system", "content": "你是一个专业的文本摘要助手。"},
                {"role": "user", "content": prompt}
            ]

            payload = {
                "model": self.model_chat,
                "messages": messages,
                "temperature": 0.1,
                "max_tokens": 512
            }

            resp = requests.post(
                f"{self.base_url}/chat/completions",
                headers=self._headers(),
                json=payload,
                timeout=15
            )
            resp.raise_for_status()
            data = resp.json()
            summary = data["choices"][0]["message"]["content"].strip()
            return summary
        except requests.Timeout:
            logger.warning("Zhipu API 摘要生成超时（15s），降级本地摘要")
            return ""
        except Exception as e:
            logger.warning(f"Zhipu API 摘要生成异常: {e}")
            return ""

    @staticmethod
    def _parse_response(raw_text: str) -> dict:
        """
        解析模型返回的固定格式文本，提取校正文本和深度摘要。
        解析失败则 summary 为 None，text 为全部内容。
        """
        text_match = re.search(r"【校正文本】\s*(.*?)\s*(?=【深度摘要】|$)", raw_text, re.DOTALL)
        summary_match = re.search(r"【深度摘要】\s*(.*)$", raw_text, re.DOTALL)

        corrected_text = text_match.group(1).strip() if text_match else raw_text.strip()
        summary = summary_match.group(1).strip() if summary_match else None

        # 如果摘要为空或仅含空白，视为 None
        if summary is not None and len(summary) == 0:
            summary = None

        return {"text": corrected_text, "summary": summary}
