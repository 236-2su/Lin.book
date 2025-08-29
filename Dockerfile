# Python 3.12 slim 이미지
FROM python:3.13.5

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /app

RUN mkdir -p /app/data

# 의존성
COPY requirements.txt /app/
RUN pip install --no-cache-dir -r requirements.txt

# 소스 복사
COPY . /app

# Gunicorn 실행
CMD ["gunicorn", "config.wsgi:application", "--bind", "0.0.0.0:8000", "--workers", "3", "--timeout", "60"]
