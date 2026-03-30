from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.ocr_service import router as ocr_router

app = FastAPI(title="Campus Learning AI Engine")

# CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:5173", "http://localhost:8080"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 注册路由
app.include_router(ocr_router, prefix="/ai", tags=["AI服务"])

@app.get("/")
def root():
    return {"message": "AI Engine is running"}