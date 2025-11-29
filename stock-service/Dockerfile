FROM eclipse-temurin:25-jre

# curl 설치 (헬스체크용)
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# 타임존 설정
ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 애플리케이션 디렉토리 생성
WORKDIR /app

# JAR 파일 복사
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

# 포트 노출
EXPOSE 8080

# 헬스체크 추가
HEALTHCHECK --interval=30s --timeout=10s --start-period=180s --retries=5 \
  CMD curl -f http://localhost:8080/api/stock-trading/v1/health-check || exit 1

# JVM 옵션 직접 설정 (637MB 메모리에 최적화 - Metaspace 증가 + GC 최적화)
ENV JAVA_OPTS="-Xms200m -Xmx300m -XX:MaxMetaspaceSize=150m -XX:ReservedCodeCacheSize=80m -Xss256k -XX:MaxDirectMemorySize=5m -XX:+UseSerialGC -XX:+UnlockExperimentalVMOptions -XX:+UseStringDeduplication"

# 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
