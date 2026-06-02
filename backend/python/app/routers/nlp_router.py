import logging

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel

from app.models.schemas import NlpResponse, SummaryResponse
from app.services.nlp_service import get_nlp_service
from app.services.zhipu_client import ZhipuClient, ZhipuApiException

logger = logging.getLogger(__name__)
router = APIRouter()


class NlpRequest(BaseModel):
    text: str


class SummaryRequest(BaseModel):
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


@router.post("/summary", response_model=SummaryResponse)
async def ai_summary(request: SummaryRequest):
    """
    调用智谱 API 生成 150 字深度摘要。
    """
    text = request.text
    if not text or len(text.strip()) == 0:
        raise HTTPException(status_code=400, detail="文本不能为空")

    try:
        client = ZhipuClient()
        summary = client.generate_summary(text)
        return SummaryResponse(summary=summary)
    except ZhipuApiException as e:
        logger.warning(f"AI 摘要生成失败: {e}")
        raise HTTPException(status_code=503, detail=f"摘要生成失败: {str(e)}")
    except Exception as e:
        logger.exception("AI 摘要异常")
        raise HTTPException(status_code=500, detail=f"摘要处理失败: {str(e)}")
