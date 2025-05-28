from fastapi import FastAPI
from pydantic import BaseModel
from transformers import AutoTokenizer, AutoModelForSequenceClassification
import torch
import uvicorn

app = FastAPI()

# 모델 로딩
MODEL_NAME = "nlptown/bert-base-multilingual-uncased-sentiment"
tokenizer = AutoTokenizer.from_pretrained(MODEL_NAME)
model = AutoModelForSequenceClassification.from_pretrained(MODEL_NAME)
model.eval()  # 평가 모드

# 요청 데이터 형식
class ChatRequest(BaseModel):
    messages: list[str]

# 응답 데이터 형식
class ChatResponse(BaseModel):
    score: int  # 누적 감정 점수
    count: int    # 메시지 개수

# 예측 함수
def predict_sentiment(texts: list[str]) -> list[int]:
    inputs = tokenizer(texts, padding=True, truncation=True, return_tensors="pt")
    with torch.no_grad():
        outputs = model(**inputs)
        predictions = torch.argmax(outputs.logits, dim=1) + 1  # 0~4 → 1~5
    return predictions.tolist()

# POST 엔드포인트
@app.post("/predict", response_model=ChatResponse)
async def predict(chat_req: ChatRequest):
    results = predict_sentiment(chat_req.messages)
    return ChatResponse(score=sum(results), count=len(results))
