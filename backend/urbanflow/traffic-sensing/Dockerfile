FROM openjdk:17-jdk-slim

WORKDIR /app

ENV JAVA_OPTS="-Xmx512m -Xms256m"

COPY pom.xml .
COPY ../pom.xml ../pom.xml
COPY ../common/pom.xml ../common/pom.xml

COPY src ./src
COPY ../common/src ../common/src

RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

RUN mvn clean package -DskipTests

RUN groupadd -r appuser && useradd -r -g appuser appuser

RUN chown -R appuser:appuser /app
USER appuser

EXPOSE 8083

CMD ["java", "-jar", "target/traffic-sensing-0.0.1-SNAPSHOT.jar"]