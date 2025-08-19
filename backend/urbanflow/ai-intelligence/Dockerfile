FROM eclipse-temurin:17-jre-jammy
ARG JAR_FILE=target/ai-intelligence-*.jar
WORKDIR /app
COPY ${JAR_FILE} app.jar
EXPOSE 8084
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
