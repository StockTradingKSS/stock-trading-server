FROM openjdk:24-slim

# 타임존 설정
ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 포트 설정
EXPOSE 8080

# JAR 파일 복사
ARG JAR_FILE
COPY ${JAR_FILE} app.jar

# 헬스체크 추가
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8082/actuator/health || exit 1

# 애플리케이션 실행
ENTRYPOINT ["java", "-Xmx512m", "-Xms256m", "-jar", "/app.jar"]
