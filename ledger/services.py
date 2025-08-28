import json
import os
import time
import uuid

import requests

from ledger_report.services import get_gemini_llm


def process_ocr_text(text):
    system_prompt = "당신은 전문적인 OCR 텍스트 판독가입니다. 당신은 처리된 OCR 텍스트로부터 항목과 금액을 추론해내 JSON 형태로 응답해야 합니다."

    prompt = f"""
    {system_prompt}

    이것은 영수증에서 추출해낸 OCR 데이터입니다. 이것을 판독하여 JSON 데이터 형식으로 작성해 주세요.
    {text}

    [요청 사항]
    데이터를 심층적으로 판독하여 다음 사항을 추론해 주세요.
    구매 금액의 총액
    각각의 구매 항목과 그 금액
    가게 이름
    당신의 추론 결과는 API 응답으로 제공되어야 하기 때문에 반드시 다음 형식을 따라야 합니다.
    {{
        "amount" : 구매 금액의 총액,
        "vendor" : 가게 이름,
        "details" : {{
            구매 항목 : 구매 금액,
            구매 항목 : 구매 금액
        }}
    }}
    """

    llm = get_gemini_llm()
    result = llm(prompt)

    return result


INVOKE_URL = os.getenv("CLOVA_API_URL")
X_OCR_SECRET = os.getenv("CLOVA_API_SECRET")


def ocr_from_file(image_file, lang="ko"):
    if not INVOKE_URL:
        return

    message = {
        "version": "V1",
        "requestId": str(uuid.uuid4()),
        "timestamp": int(time.time() * 1000),
        "lang": lang,
        "images": [{"format": "png", "name": "sample"}],  # format and name might need adjustment
    }

    headers = {"X-OCR-SECRET": X_OCR_SECRET}
    files = {"message": (None, json.dumps(message), "application/json"), "file": image_file}

    resp = requests.post(INVOKE_URL, headers=headers, files=files, timeout=30)
    resp.raise_for_status()
    res_text = []
    for text_json in resp.json()["images"][0]["fields"]:
        res_text.append(text_json["inferText"])

    raw_text = "".join(res_text)
    processed_data = process_ocr_text(raw_text)

    return {"raw_text": raw_text, "processed_data": processed_data}
