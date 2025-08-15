# LinBook Project

React Native와 Spring Boot를 사용한 LinBook 애플리케이션입니다.

## 🏗️ 프로젝트 구조

```
linbook/
├── linbook_be/                    # Spring Boot 백엔드
│   ├── Dockerfile
│   ├── src/
│   ├── build.gradle
│   └── gradlew
├── linbook_fe/                    # React Native 프론트엔드
│   ├── Dockerfile
│   ├── package.json
│   ├── src/
│   └── nginx.conf
├── compose.yml                     # 기본 Docker Compose
├── compose.ci.yml                 # CI/CD용 오버라이드
├── env.example                    # 환경 변수 템플릿
├── Jenkinsfile                    # CI/CD 파이프라인
└── README.md
```

## 🚀 빠른 시작

### 1. 환경 변수 설정
```bash
cp env.example .env
# .env 파일을 편집하여 실제 값으로 수정
```

### 2. 개발 환경 실행
```bash
docker compose -f compose.yml -f compose.ci.yml up -d
```

### 3. 프로덕션 환경 실행
```bash
docker compose -f compose.yml -f compose.ci.yml -f compose.prod.yml up -d
```

## 🔧 기술 스택

### 백엔드
- **Spring Boot 3.x**
- **Java 21**
- **Gradle 8.5**
- **MySQL 8.0**

### 프론트엔드
- **React Native Web**
- **Node.js 18**
- **Nginx**

### 인프라
- **Docker & Docker Compose**
- **Jenkins CI/CD**

## 📦 Docker 이미지

- **백엔드**: `linbook/be:dev`
- **프론트엔드**: `linbook/fe:dev`

## 🌐 서비스 포트

- **프론트엔드**: `80` (HTTP)
- **백엔드**: `8082` (EC2) → `8080` (컨테이너)
- **Jenkins**: `8081`
- **MySQL**: 내부 네트워크만 (SSH 터널로 접속)

## 🔒 보안

- root가 아닌 사용자로 컨테이너 실행
- 헬스체크를 통한 컨테이너 상태 모니터링
- 보안 헤더 설정
- 환경 변수를 통한 민감 정보 관리

## 📋 CI/CD 파이프라인

Jenkins를 통한 자동화된 배포:

1. **Sync**: 코드 동기화
2. **Build**: Docker 이미지 빌드
3. **Deploy**: 서비스 배포
4. **Health Check**: 서비스 상태 확인

## 🛠️ 개발 가이드

### 백엔드 개발
```bash
cd linbook_be
./gradlew bootRun
```

### 프론트엔드 개발
```bash
cd linbook_fe
npm install
npm start
```

## 📝 환경 변수

필수 환경 변수:
- `SPRING_DATASOURCE_URL`: MySQL 연결 문자열
- `SPRING_DATASOURCE_USERNAME`: 데이터베이스 사용자명
- `SPRING_DATASOURCE_PASSWORD`: 데이터베이스 비밀번호
- `JWT_SECRET`: JWT 서명 키
- `BE_IMAGE`: 백엔드 이미지 태그
- `FE_IMAGE`: 프론트엔드 이미지 태그

## 🤝 기여하기

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다.
