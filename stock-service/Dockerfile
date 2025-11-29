FROM eclipse-temurin:25-jre

# 애플리케이션 디렉토리 생성
WORKDIR /app

# JAR 파일 복사
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

# 포트 노출
EXPOSE 8080

# JVM 옵션 직접 설정 (637MB 메모리에 최적화 - Metaspace 증가 + GC 최적화)
ENV JAVA_OPTS="-Xms200m -Xmx300m -XX:MaxMetaspaceSize=150m -XX:ReservedCodeCacheSize=80m -Xss256k -XX:MaxDirectMemorySize=5m -XX:+UseSerialGC -XX:+UnlockExperimentalVMOptions -XX:+UseStringDeduplication"

# 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
