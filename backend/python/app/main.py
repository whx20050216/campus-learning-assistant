import os

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.routers import ocr_router, nlp_router, enhance_router, health_router

app = FastAPI(title="Campus Learning AI Engine")

# CORS 配置从环境变量读取，适配多环境部署
_allowed_origins = os.environ.get(
    "CORS_ALLOWED_ORIGINS",
    "http://localhost:5173,http://localhost:8080,http://localhost"
)
allow_origins = [o.strip() for o in _allowed_origins.split(",") if o.strip()]

app.add_middleware(
    CORSMiddleware,
    allow_origins=allow_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 注册路由
app.include_router(ocr_router.router, prefix="/ai", tags=["OCR"])
app.include_router(nlp_router.router, prefix="/ai", tags=["NLP"])
app.include_router(enhance_router.router, prefix="/ai", tags=["Enhance"])
app.include_router(health_router.router, tags=["Health"])


@app.get("/")
def root():
    return {
        "message": "AI Engine is running",
        "services": ["ocr", "nlp", "enhance", "health"]
    }
