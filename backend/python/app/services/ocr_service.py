import os
import logging
from typing import List, Tuple

import cv2
import numpy as np
from paddleocr import PaddleOCR

from app.models.schemas import OcrResponse
from app.utils.preprocessor import ImagePreprocessor

logger = logging.getLogger(__name__)


class OcrService:
    """
    OCR 服务类：封装 PaddleOCR 调用，支持多页 PDF。
    """

    def __init__(self):
        self.ocr_engine = None
        self.preprocessor = ImagePreprocessor()
        try:
            self.ocr_engine = PaddleOCR(
                use_angle_cls=True,
                lang='ch',
                show_log=False,
                use_gpu=False
            )
            logger.info("PaddleOCR 初始化成功")
        except Exception as e:
            logger.error(f"PaddleOCR 初始化失败: {e}")

    def recognize(self, file_path: str) -> OcrResponse:
        """
        识别主方法：支持 PDF（多页）和图片。
        """
        if self.ocr_engine is None:
            logger.error("OCR 引擎未初始化，无法识别")
            return OcrResponse(
                text="",
                confidence=0.0,
                engine="paddleocr",
                pages=0,
                source="local"
            )

        ext = os.path.splitext(file_path)[1].lower()

        if ext == ".pdf":
            return self._recognize_pdf(file_path)
        else:
            return self._recognize_image_file(file_path)

    def _recognize_pdf(self, file_path: str) -> OcrResponse:
        """多页 PDF 逐页识别并合并结果。"""
        try:
            from pdf2image import convert_from_path
            poppler_path = os.environ.get("POPPLER_PATH", "/usr/bin")
            images = convert_from_path(file_path, dpi=200, poppler_path=poppler_path)
        except ImportError:
            logger.error("pdf2image 未安装，无法处理 PDF")
            return OcrResponse(
                text="",
                confidence=0.0,
                engine="paddleocr",
                pages=0,
                source="local"
            )
        except Exception as e:
            logger.error(f"PDF 转换失败（请确保已安装 poppler）: {e}")
            return OcrResponse(
                text="",
                confidence=0.0,
                engine="paddleocr",
                pages=0,
                source="local"
            )
        all_texts: List[str] = []
        page_confidences: List[float] = []

        for idx, pil_image in enumerate(images):
            # PIL -> numpy (RGB->BGR)
            np_image = np.array(pil_image)
            np_image = cv2.cvtColor(np_image, cv2.COLOR_RGB2BGR)

            text, conf = self._recognize_single_image(np_image)
            all_texts.append(text)
            page_confidences.append(conf)
            logger.debug(f"PDF 第 {idx+1}/{len(images)} 页置信度: {conf:.4f}")

        # 整体置信度 = 各页平均置信度的平均
        overall_confidence = sum(page_confidences) / len(page_confidences) if page_confidences else 0.0
        full_text = "\n".join(all_texts)

        return OcrResponse(
            text=full_text,
            confidence=round(overall_confidence, 4),
            engine="paddleocr",
            pages=len(images),
            source="local"
        )

    def _recognize_image_file(self, file_path: str) -> OcrResponse:
        """单张图片文件识别。"""
        import cv2
        image = cv2.imread(file_path)
        if image is None:
            logger.error(f"无法读取图片: {file_path}")
            return OcrResponse(
                text="",
                confidence=0.0,
                engine="paddleocr",
                pages=1,
                source="local"
            )

        text, confidence = self._recognize_single_image(image)
        return OcrResponse(
            text=text,
            confidence=round(confidence, 4),
            engine="paddleocr",
            pages=1,
            source="local"
        )

    def _recognize_single_image(self, image: np.ndarray) -> Tuple[str, float]:
        """
        对单张 numpy 图片进行 OCR。

        公式：
        设识别出 M 个文本块，各块置信度为 c₁, c₂, ..., cₘ
        confidence = (Σ cₖ) / M，k = 1..M
        若 M = 0（无文字），confidence = 0.0
        """
        # 预处理
        processed = self.preprocessor.preprocess(image)

        # PaddleOCR 识别（需要 3 通道）
        if len(processed.shape) == 2:
            processed_rgb = cv2.cvtColor(processed, cv2.COLOR_GRAY2BGR)
        else:
            processed_rgb = processed

        result = self.ocr_engine.ocr(processed_rgb, cls=True)

        texts = []
        total_conf = 0.0
        count = 0

        if result and result[0]:
            for line in result[0]:
                if line:
                    text = line[1][0]
                    conf = line[1][1]
                    texts.append(text)
                    total_conf += conf
                    count += 1

        confidence = total_conf / count if count > 0 else 0.0
        return "\n".join(texts), confidence


# 全局单例（避免重复加载模型）
_ocr_service: OcrService = None


def get_ocr_service() -> OcrService:
    global _ocr_service
    if _ocr_service is None:
        _ocr_service = OcrService()
    return _ocr_service
