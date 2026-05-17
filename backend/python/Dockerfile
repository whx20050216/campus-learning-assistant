FROM python:3.9-slim

WORKDIR /app

# 安装系统依赖：poppler-utils(PDF转图片)、libgl1(OpenCV依赖)、libglib2.0-0等
RUN apt-get update && apt-get install -y --no-install-recommends \
    poppler-utils \
    libgl1 \
    libglib2.0-0 \
    libsm6 \
    libxext6 \
    libxrender-dev \
    libgomp1 \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 复制依赖清单并安装
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 复制应用代码
COPY app ./app

# 环境变量默认值（生产环境应在 docker-compose 或 .env 中覆盖）
ENV PYTHONUNBUFFERED=1
ENV POPPLER_PATH=/usr/bin
ENV CONFIDENCE_THRESHOLD=0.85
ENV APP_PORT=8000

EXPOSE 8000

# 启动命令
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
