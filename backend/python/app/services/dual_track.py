import os
import logging
from typing import Optional

from app.services.zhipu_client import ZhipuClient, ZhipuApiException

logger = logging.getLogger(__name__)


class DualTrackDecider:
    """
    双轨决策器：根据 OCR 置信度决定本地处理或 API 增强。
    """

    def __init__(self):
        self.threshold = float(os.getenv("CONFIDENCE_THRESHOLD", "0.85"))
        try:
            self.zhipu = ZhipuClient()
            self.zhipu_available = True
        except ZhipuApiException as e:
            logger.warning(f"ZhipuClient 初始化失败: {e}，双轨决策将始终走 fallback")
            self.zhipu_available = False
            self.zhipu = None

    def decide(
        self,
        confidence: float,
        file_path: Optional[str] = None,
        image_base64: Optional[str] = None,
        text: Optional[str] = None
    ) -> dict:
        """
        双轨决策主入口。

        Args:
            confidence: OCR 置信度 0.0-1.0
            file_path: 本地文件路径（用于读取图片转 base64）
            image_base64: 图片 base64（优先使用）
            text: 已有 OCR 文本

        Returns:
            {"source": "local" | "ai_enhanced" | "fallback", "text": str | None, "summary": str | None}
        """
        if confidence > self.threshold:
            return {"source": "local", "text": None, "summary": None}

        # 低置信度，尝试 API 增强
        if not self.zhipu_available:
            logger.info("智谱 API 不可用，直接 fallback")
            return {"source": "fallback", "text": None, "summary": None}

        # 若未提供 base64 但有文件路径，则读取并编码
        if not image_base64 and file_path:
            try:
                import base64
                with open(file_path, "rb") as f:
                    image_base64 = base64.b64encode(f.read()).decode("utf-8")
            except Exception as e:
                logger.warning(f"读取文件转 base64 失败: {e}")
                return {"source": "fallback", "text": None, "summary": None}

        try:
            enhanced = self.zhipu.enhance(image_base64=image_base64, text=text)
            return {
                "source": "ai_enhanced",
                "text": enhanced.get("text"),
                "summary": enhanced.get("summary"),
                "engine": "zhipu"
            }
        except ZhipuApiException:
            return {"source": "fallback", "text": None, "summary": None}
