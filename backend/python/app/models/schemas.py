from pydantic import BaseModel, Field
from typing import List, Optional


class OcrResponse(BaseModel):
    text: str = Field(default="", description="识别出的完整文本")
    confidence: float = Field(default=0.0, description="整体置信度 0.0-1.0")
    engine: str = Field(default="paddleocr", description="识别引擎")
    pages: int = Field(default=1, description="文件页数")
    source: str = Field(default="local", description="处理来源: local/ai_enhanced/fallback")


class KeywordItem(BaseModel):
    keyword: str
    weight: float
    type: str = Field(default="keyword", description="keyword 或 keypoint")


class KnowledgePointItem(BaseModel):
    content: str
    type: str = Field(default="definition", description="definition|formula|theorem|example")
    position: Optional[str] = Field(default=None, description="在原文中的位置")


class NlpResponse(BaseModel):
    keywords: List[KeywordItem]
    summary: str
    knowledge_points: List[KnowledgePointItem]
    source: str = Field(default="local")


class EnhanceRequest(BaseModel):
    file_path: Optional[str] = Field(default=None, description="本地文件路径")
    image_base64: Optional[str] = Field(default=None, description="图片 Base64")
    text: Optional[str] = Field(default=None, description="已有 OCR 文本")


class EnhanceResponse(BaseModel):
    source: str
    text: Optional[str] = None
    engine: Optional[str] = None


class HealthResponse(BaseModel):
    status: str
    service: str
