import numpy as np
from PIL import Image, ImageFilter
import cv2


class ImagePreprocessor:
    """
    图像预处理工具：灰度化、去噪、旋转校正
    """

    @staticmethod
    def preprocess(image: np.ndarray) -> np.ndarray:
        """
        对输入图像进行预处理流水线：
        1. 若为多通道，转为灰度图
        2. 轻度高斯去噪
        3. 自适应二值化增强文字对比度（可选）
        """
        # 1. 灰度化
        if len(image.shape) == 3 and image.shape[2] == 3:
            gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        elif len(image.shape) == 3 and image.shape[2] == 4:
            gray = cv2.cvtColor(image, cv2.COLOR_BGRA2GRAY)
        else:
            gray = image

        # 2. 去噪（轻度高斯模糊）
        denoised = cv2.GaussianBlur(gray, (3, 3), 0)

        # 3. 自适应二值化（增强印刷体对比度）
        binary = cv2.adaptiveThreshold(
            denoised, 255,
            cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
            cv2.THRESH_BINARY, 11, 2
        )

        return binary

    @staticmethod
    def correct_rotation(image: np.ndarray) -> np.ndarray:
        """
        简单旋转校正：检测文字方向并旋转（0/90/180/270）
        目前仅返回原图，作为扩展接口保留。
        """
        # 旋转校正可作为后续扩展，目前直接返回
        return image
