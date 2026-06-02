import logging

from fastapi import APIRouter, HTTPException

from app.models.schemas import EnhanceRequest, EnhanceResponse
from app.services.dual_track import DualTrackDecider

logger = logging.getLogger(__name__)
router = APIRouter()

# 全局单例
decider: DualTrackDecider = None


def get_decider() -> DualTrackDecider:
    global decider
    if decider is None:
        decider = DualTrackDecider()
    return decider


@router.post("/enhance", response_model=EnhanceResponse)
async def ai_enhance(request: EnhanceRequest):
    """
    低置信度时调用智谱 API 进行增强。
    接收 file_path 或 image_base64，返回增强文本及深度摘要。
    """
    d = get_decider()

    try:
        result = d.decide(
            confidence=0.0,  # 强制走增强分支
            file_path=request.file_path,
            image_base64=request.image_base64,
            text=request.text
        )
        return EnhanceResponse(
            source=result["source"],
            text=result.get("text"),
            summary=result.get("summary"),
            engine=result.get("engine")
        )
    except Exception as e:
        logger.exception("AI 增强异常")
        raise HTTPException(status_code=500, detail=f"增强失败: {str(e)}")
