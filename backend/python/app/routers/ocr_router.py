import os
import shutil
import tempfile
import logging

from fastapi import APIRouter, UploadFile, File, HTTPException

from app.models.schemas import OcrResponse
from app.services.ocr_service import get_ocr_service

logger = logging.getLogger(__name__)
router = APIRouter()


@router.post("/ocr", response_model=OcrResponse)
async def ocr_recognition(file: UploadFile = File(...)):
    """
    接收图片/PDF，返回 OCR 识别结果和置信度。
    """
    if not file.content_type or not file.content_type.startswith(('image/', 'application/pdf')):
        raise HTTPException(status_code=400, detail="只支持图片或 PDF 格式")

    suffix = os.path.splitext(file.filename or ".tmp")[1]
    with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
        shutil.copyfileobj(file.file, tmp)
        tmp_path = tmp.name

    try:
        service = get_ocr_service()
        result = service.recognize(tmp_path)
        return result
    except Exception as e:
        logger.exception("OCR 识别异常")
        raise HTTPException(status_code=500, detail=f"OCR 处理失败: {str(e)}")
    finally:
        try:
            os.unlink(tmp_path)
        except Exception:
            pass
