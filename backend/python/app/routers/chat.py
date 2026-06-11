import json
import logging

from fastapi import APIRouter, HTTPException
from fastapi.responses import StreamingResponse

from app.models.schemas import ChatStreamRequest
from app.services.chat_service import ChatService

logger = logging.getLogger(__name__)
router = APIRouter()


def _sse_stream(generator):
    """将生成器包装为 SSE 格式"""
    for chunk in generator:
        if chunk == "[DONE]":
            yield f"data: [DONE]\n\n"
            break
        yield f"data: {chunk}\n\n"


@router.post("/stream")
async def chat_stream(request: ChatStreamRequest):
    """
    流式 AI 问答接口。
    请求体包含 messages 列表（OpenAI 格式）。
    返回 text/event-stream，逐 chunk 输出 AI 回复。
    """
    try:
        service = ChatService()
        generator = service.stream_chat(request.messages)
        return StreamingResponse(
            _sse_stream(generator),
            media_type="text/event-stream"
        )
    except RuntimeError as e:
        logger.error(f"问答失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))
    except Exception as e:
        logger.error(f"问答异常: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail="AI 问答服务异常，请稍后重试")
