from fastapi import APIRouter, UploadFile, File, HTTPException
from paddleocr import PaddleOCR
import shutil
import os
import tempfile

router = APIRouter()

# 初始化 PaddleOCR（第一次运行会自动下载模型）
ocr = PaddleOCR(use_angle_cls=True, lang='ch', show_log=False)

@router.post("/ocr")
async def ocr_recognition(file: UploadFile = File(...)):
    """
    接收图片/PDF，返回 OCR 识别结果和置信度
    """
    if not file.content_type.startswith(('image/', 'application/pdf')):
        raise HTTPException(status_code=400, detail="只支持图片或PDF格式")
    
    # 保存临时文件
    with tempfile.NamedTemporaryFile(delete=False, suffix=os.path.splitext(file.filename)[1]) as tmp:
        shutil.copyfileobj(file.file, tmp)
        tmp_path = tmp.name
    
    try:
        # 执行 OCR
        result = ocr.ocr(tmp_path, cls=True)
        
        # 解析结果
        texts = []
        total_confidence = 0
        count = 0
        
        for line in result[0]:
            if line:
                text = line[1][0]
                confidence = line[1][1]
                texts.append(text)
                total_confidence += confidence
                count += 1
        
        # 计算平均置信度
        avg_confidence = round(total_confidence / count, 2) if count > 0 else 0
        
        return {
            "success": True,
            "text": "\n".join(texts),
            "confidence": avg_confidence,
            "source": "local" if avg_confidence > 0.85 else "need_ai_enhance"
        }
        
    finally:
        os.unlink(tmp_path)  # 清理临时文件

# 健康检查
@router.get("/health")
def health_check():
    return {"status": "ok", "service": "ocr"}