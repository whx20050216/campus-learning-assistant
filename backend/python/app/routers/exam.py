import logging

from fastapi import APIRouter, HTTPException

from app.models.schemas import ExamGenerateRequest, ExamGenerateResponse, ExamQuestionItem
from app.services.exam_service import ExamService

logger = logging.getLogger(__name__)
router = APIRouter()


@router.post("/generate", response_model=ExamGenerateResponse)
async def generate_exam(request: ExamGenerateRequest):
    """
    根据资料摘要和知识点生成试卷题目。
    请求体包含：summaries, knowledgePoints, questionCount, types, difficulty
    """
    try:
        service = ExamService()
        questions_data = service.generate_exam(
            summaries=request.summaries,
            knowledge_points=request.knowledgePoints,
            question_count=request.questionCount,
            types=request.types,
            difficulty=request.difficulty
        )

        questions = [ExamQuestionItem(**q) for q in questions_data]
        return ExamGenerateResponse(questions=questions)
    except RuntimeError as e:
        logger.error(f"组卷失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))
    except Exception as e:
        logger.error(f"组卷异常: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail="智能组卷服务异常，请稍后重试")
