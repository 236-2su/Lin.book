# 2025 shinhan hackathon with SSAFY 6팀
# 돈 아리, 돈 워리 프로젝트
# 통합 동아리 관리 맵

# 백앤드는 master 브랜치에
# 프론트는 Lin_Book_FE 브랜치에 있습니다.
Django 백엔드(+ Kotlin 프론트 분리 배포) 기반의 동아리 장부/보고서 앱입니다.
배포 대상: EC2(Ubuntu).

> 이 문서는 **실행 방법 2가지**를 제공합니다.
>
> 1) **Windows (Docker Desktop 설치됨)** 기준
> 2) **Ubuntu (네이티브/가상환경)** 기준

---

## 0. 프로젝트 개요

- **Backend:** Django REST Framework
- **Frontend:** Kotlin (Android / Jetpack Compose)
- **Deploy:** AWS EC2 (Ubuntu) + Nginx (선택)
- **DB:** 기본 SQLite(개발용)
- **API Docs:** `drf-spectacular`가 생성한 `/api/schema/`(OpenAPI), `/api/docs/`(Swagger UI)

```
.
├─ manage.py
├─ config
├──settings.py  Django 설치 파일
├─ requirements.txt   # Python deps
├─ .env.example       # 환경변수 예시 (없다면 아래 표를 참고해 직접 생성)
└─ Dockerfile         # (선택) Docker 빌드 파일
```

---

## 1) Windows (Docker Desktop 설치됨) – 실행 방법

### 1-1. 사전 준비
- **Docker Desktop** 설치 및 실행
- 프로젝트 루트에 **`.env`** 파일 준비(없다면 새로 작성). 예:
  ```env
  DEBUG=1
  SECRET_KEY=change-me-in-production
  ALLOWED_HOSTS=*
  DJANGO_SETTINGS_MODULE=config.settings  # 프로젝트에 맞게 수정
  ```

### 1-2. Docker 이미지 빌드 (docker build)
PowerShell에서 프로젝트 루트로 이동 후:
```powershell
docker build -t club-ledger:dev .
```

### 1-3. 컨테이너 실행
SQLite(개발용) 기준으로 8000 포트 노출:
```powershell
docker run --name club-ledger \
  --env-file .\.env \
  -p 8000:8000 \
  -v ${PWD}:/app \
  club-ledger:dev \
  sh -c "python manage.py migrate && python manage.py runserver 0.0.0.0:8000"
```
> 첫 실행 시 마이그레이션을 자동 수행합니다.

### 1-4. 관리자 계정 생성(최초 1회)
컨테이너 안에서:
```powershell
docker exec -it club-ledger python manage.py createsuperuser
```

### 1-5. 접속
- 백엔드 API: http://localhost:8000/
- (선택) Swagger UI: http://localhost:8000/api/docs/
- (선택) OpenAPI JSON: http://localhost:8000/api/schema/


> **docker-compose 사용을 선호한다면**
> `docker-compose.yml`이 준비돼 있을 경우:
> ```powershell
> docker compose up --build
> ```
> docker compose exec linbook-container python manage.py createsuperuser`

---

## 2) Ubuntu (네이티브/가상환경) – 실행 방법

### 2-1. 사전 준비
- 시스템 패키지 업데이트:
  ```bash
  sudo apt update && sudo apt -y upgrade
  ```

### 2-2. 프로젝트 클론 & 가상환경
```bash
cd ~
git clone https://github.com/236-2su/Lin.book
cd <project-root>

```

### 2-3. 의존성 설치
```bash
pip install --upgrade pip
pip install -r requirements.txt
```

### 2-4. 환경변수(.env) 준비
루트에 `.env` 파일 생성(예):
DJANGO_SECRET_KEY=django-insecure-...
FINAPI_SECRET=
SQLITE_PATH=/app/data/db.sqlite3
DEBUG=False
GEMINI_API_KEY=
CLOVA_API_URL=
CLOVA_API_SECRET=
ALLOWED_HOSTS=
```

SECRET_KEY가 필요하면:
```bash
python -c "from django.core.management.utils import get_random_secret_key as g; print(g())"
```

### 2-5. 마이그레이션 & 관리자 계정
```bash
python manage.py migrate
python manage.py createsuperuser  # 이메일/비번 입력
```

### 2-6. 개발 서버 실행
```bash
python manage.py runserver 0.0.0.0:8000
```
- 서버 접속: `http://<서버 IP>:8000/` (로컬이면 `http://127.0.0.1:8000/`)


## 공통 유틸 명령어

- 데이터베이스 마이그레이션 생성/적용
  ```bash
  python manage.py makemigrations
  python manage.py migrate
  ```
- Django Admin 계정
  ```bash
  python manage.py createsuperuser
  ```
- 테스트 실행(사용 중이라면)
  ```bash
  python manage.py test
  ```

---
## 사욯가능 계정 ##
- 이메일만 있으면 로그인 가능
1. 동아리 리더 계정
  day2201@naver.com
2. 일반 학생 
  user38@example.com

## 자주 묻는 질문(FAQ)

**Q. Swagger UI가 404/TemplateDoesNotExist 에러가 나요.**
A. `drf-spectacular`가 설치돼 있고, `urlpatterns`에 다음이 등록되어 있는지 확인하세요.
```python
from drf_spectacular.views import SpectacularAPIView, SpectacularSwaggerView

urlpatterns = [
    path("api/schema/", SpectacularAPIView.as_view(), name="schema"),
    path("api/docs/", SpectacularSwaggerView.as_view(url_name="schema"), name="swagger-ui"),
]
```

**Q. 포트 8000이 이미 사용 중이에요.**
A. 다른 포트를 사용하세요. 예: `runserver 0.0.0.0:8080` 또는 Docker `-p 8080:8000`

**Q. SQLite 대신 PostgreSQL을 쓰고 싶어요.**
A. `psycopg[binary]`를 설치하고 `DATABASES` 설정을 PostgreSQL로 바꾸세요. Docker를 쓴다면 `docker-compose`로 DB 서비스를 함께 올리는 것을 권장합니다.

---

