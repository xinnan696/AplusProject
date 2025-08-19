FROM eclipse-temurin:17-jre-jammy
ARG JAR_FILE=target/special-event-handling-*.jar
WORKDIR /app
COPY ${JAR_FILE} app.jar
EXPOSE 8085
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
