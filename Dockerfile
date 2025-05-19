# 1단계: 빌드 스테이지
FROM gradle:8.7-jdk21 AS builder

# 필요한 소스코드 전체 복사
WORKDIR /build
COPY . .

# Gradle로 jar 빌드 (bootJar task)
RUN gradle bootJar --no-daemon

# 2단계: 실행 스테이지
FROM eclipse-temurin:21-jdk

# vi 설치 (옵션)
RUN apt-get update && apt-get install -y vim && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 빌드 스테이지에서 생성된 jar 파일 복사
COPY --from=builder /build/build/libs/*.jar app.jar

RUN chmod 777 /app/app.jar
ARG PROFILE
ENV PROFILE=${PROFILE}

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]