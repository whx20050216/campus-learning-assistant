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
    summary: Optional[str] = None
    engine: Optional[str] = None


class SummaryResponse(BaseModel):
    summary: str = Field(default="", description="生成的深度摘要")


class HealthResponse(BaseModel):
    status: str
    service: str


# ========== AI 问答相关模型 ==========

class ChatStreamRequest(BaseModel):
    messages: list = Field(default_factory=list, description="OpenAI 格式的消息列表")


# ========== 智能组卷相关模型 ==========

class ExamGenerateRequest(BaseModel):
    summaries: List[str] = Field(default_factory=list, description="资料摘要列表")
    knowledgePoints: List[str] = Field(default_factory=list, description="知识点列表")
    questionCount: int = Field(default=10, ge=1, le=50, description="题目数量")
    types: List[str] = Field(default_factory=lambda: ["single", "multiple", "judge", "essay"], description="题型列表")
    difficulty: str = Field(default="medium", description="难度: easy/medium/hard")


class ExamQuestionItem(BaseModel):
    type: str = Field(..., description="题型: single/multiple/judge/essay")
    content: str = Field(..., description="题目内容")
    options: Optional[List[str]] = Field(default=None, description="选项列表")
    answer: Optional[str] = Field(default=None, description="标准答案")
    analysis: Optional[str] = Field(default=None, description="解析")
    difficulty: str = Field(default="medium", description="难度")
    knowledgePoint: Optional[str] = Field(default=None, description="知识点")


class ExamGenerateResponse(BaseModel):
    questions: List[ExamQuestionItem] = Field(default_factory=list, description="生成的题目列表")
