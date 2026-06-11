import json
import logging
import os

from zhipuai import ZhipuAI

logger = logging.getLogger(__name__)


class ChatService:
    """AI 问答服务：调用智谱 ChatGLM-4 进行流式对话"""

    def __init__(self):
        self.api_key = os.getenv("ZHIPU_API_KEY", "").strip()
        if not self.api_key:
            logger.error("ZHIPU_API_KEY 未配置，ChatService 不可用")
        self.model = "glm-4"

    def stream_chat(self, messages: list):
        """
        流式对话生成器。
        接收 messages 列表（OpenAI 格式），逐 chunk 返回 {"content": "..."}。
        最后返回 [DONE]。
        """
        if not self.api_key:
            raise RuntimeError("ZHIPU_API_KEY 未配置")

        try:
            client = ZhipuAI(api_key=self.api_key)
            response = client.chat.completions.create(
                model=self.model,
                messages=messages,
                stream=True,
                temperature=0.7,
                max_tokens=2048
            )
            for chunk in response:
                delta = chunk.choices[0].delta
                content = getattr(delta, "content", "")
                if content:
                    yield json.dumps({"content": content}, ensure_ascii=False)
            yield "[DONE]"
        except Exception as e:
            logger.error(f"智谱 API 流式调用失败: {e}")
            raise RuntimeError(f"AI 问答服务异常: {e}")
