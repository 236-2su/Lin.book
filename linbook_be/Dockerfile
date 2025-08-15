# Multi-stage build for Spring Boot
FROM gradle:8.5-jdk21 AS builder

WORKDIR /app
COPY . .

# Gradle 빌드
RUN gradle build -x test --no-daemon

# Runtime stage
FROM openjdk:21-jre-slim

# 메타데이터
LABEL maintainer="LinBook Team"
LABEL version="1.0"
LABEL description="LinBook Backend Service"

# 보안: root가 아닌 사용자로 실행
RUN groupadd -r linbook && useradd -r -g linbook linbook

WORKDIR /app

# JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 권한 설정
RUN chown -R linbook:linbook /app
USER linbook

# JVM 옵션
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseContainerSupport"

# 헬스체크
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# 포트 노출
EXPOSE 8080

# 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
