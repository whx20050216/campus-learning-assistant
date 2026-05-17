import logging

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel

from app.models.schemas import NlpResponse
from app.services.nlp_service import get_nlp_service

logger = logging.getLogger(__name__)
router = APIRouter()


class NlpRequest(BaseModel):
    text: str


@router.post("/nlp", response_model=NlpResponse)
async def nlp_analysis(request: NlpRequest):
    """
    接收文本，返回关键词、摘要、知识点。
    """
    text = request.text
    if not text or len(text.strip()) == 0:
        raise HTTPException(status_code=400, detail="文本不能为空")

    try:
        service = get_nlp_service()
        result = service.analyze(text)
        return result
    except Exception as e:
        logger.exception("NLP 分析异常")
        raise HTTPException(status_code=500, detail=f"NLP 处理失败: {str(e)}")
