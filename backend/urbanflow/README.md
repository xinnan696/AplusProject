# Getting Started
## UrbanFlow

A modular, Spring Boot–based microservices system for urban traffic management, including user authentication, signal control, traffic sensing, AI inference, special event handling, logging & audit, and status synchronization.

### Architecture

- Multi-module Maven project (Java 17)
- Independent Spring Boot services (JARs) packaged and run in Docker
- Infrastructure: MySQL, Redis, InfluxDB (time-series)

### Modules

- user-authentication: Authentication and authorization
- signal-control: Traffic signal orchestration
- traffic-sensing: Ingest/processing of traffic sensor data
- ai-intelligence: AI/ML inference for traffic optimization
- special-event-handling: Incident/event workflows
- logging-and-audit: Centralized logging and auditing
- status-sync: Cross-service state synchronization
- common: Shared library (not deployed independently)

### Tech Stack

- Java 17, Spring Boot 3
- Maven
- MyBatis, MySQL
- Redis
- InfluxDB
- Docker & Docker Compose

---

## Prerequisites

- JDK 17+
- Maven 3.9+
- Docker 20.10+ and Docker Compose 2.0+
- 4 GB free memory

---

## Quick Start

### 1) Build all modules

Build at the repository root so each service gets a JAR in its `target/`:

```bash
mvn clean package -DskipTests
```

This is required because service Dockerfiles copy the prebuilt JAR from `target/`.

### 2) Start with Docker Compose

```bash
# Build images and start containers
docker compose up -d --build

# Check containers
docker compose ps

# Follow logs (all or one service)
docker compose logs -f
docker compose logs -f user-authentication
```

To stop:

```bash
docker compose down
```

Note:
- MySQL data persists in the `db_data` volume.
- InfluxDB data persists in the `influxdb_data` volume.

---

## Services and Ports

- user-authentication: 8081
- signal-control: 8082
- traffic-sensing: 8083
- ai-intelligence: 8084
- special-event-handling: 8085
- logging-and-audit: 8086
- status-sync: 8087
- MySQL: 3306 (host)
- Redis: 6379 (host)
- InfluxDB: 9999 (host) → container port 8086

Inside the Docker network, services discover each other by service name (e.g., `mysql`, `redis`, `influxdb`). The internal InfluxDB port is 8086.

---

## Configuration

Most configuration is passed via environment variables in `docker-compose.yaml`. Common variables:

- Database:
    - SPRING_DATASOURCE_URL
    - SPRING_DATASOURCE_USERNAME
    - SPRING_DATASOURCE_PASSWORD
- Redis:
    - SPRING_REDIS_HOST
    - SPRING_REDIS_PORT
- InfluxDB (where applicable):
    - INFLUXDB_URL
    - INFLUXDB_TOKEN
    - INFLUXDB_ORG
    - INFLUXDB_BUCKET
- JVM:
    - JAVA_OPTS (e.g., `-Xmx512m -Xms256m`)

Review and adjust values in `docker-compose.yaml` before deployment.

---

## Project Structure

```text
urbanflow/
├─ pom.xml                     # Parent Maven POM (packaging=pom)
├─ docker-compose.yaml         # Multi-service orchestration
├─ common/                     # Shared library (jar)
├─ user-authentication/        # Spring Boot service + Dockerfile (8081)
├─ signal-control/             # Spring Boot service + Dockerfile (8082)
├─ traffic-sensing/            # Spring Boot service + Dockerfile (8083)
├─ ai-intelligence/            # Spring Boot service + Dockerfile (8084)
├─ special-event-handling/     # Spring Boot service + Dockerfile (8085)
├─ logging-and-audit/          # Spring Boot service + Dockerfile (8086)
└─ status-sync/                # Spring Boot service + Dockerfile (8087)
```

Each service contains a Dockerfile that:
- Uses `eclipse-temurin:17-jre-jammy` (runtime only)
- Copies its prebuilt JAR from `target/`
- Starts with `ENTRYPOINT ["java", "-jar", "/app/app.jar"]`

---

## Local Development (without Docker)

You can run any service locally after provisioning infra (e.g., via Docker):

```bash
# Start infra only
docker compose up -d mysql redis influxdb

# Run a service from IDE or terminal
cd user-authentication
mvn spring-boot:run
```

Ensure environment variables/properties point to:
- MySQL: `jdbc:mysql://localhost:3306/urbanflow`
- Redis: `localhost:6379`
- InfluxDB (host-port): `http://localhost:9999` (container port is 8086)

---

## Testing

```bash
# Run unit/integration tests
mvn test
```

---

## Troubleshooting

- Build fails inside Docker:
    - Ensure you ran `mvn clean package -DskipTests` at the repo root first.
- Services fail to start due to DB:
    - Wait for MySQL readiness (Compose includes healthchecks).
- InfluxDB connectivity:
    - Inside containers use `http://influxdb:8086`.
    - From host use `http://localhost:9999` (mapped to container 8086).
- Health endpoints:
    - If you rely on `/actuator/health`, make sure Spring Boot Actuator is added and exposed.

---

## Security and Production Notes

- Change default credentials and tokens before production
- Externalize secrets with Compose env files or a secret manager
- Consider SSL/TLS, firewalls, and network policies
- Tune JVM memory via `JAVA_OPTS`
- Set up monitoring, centralized logging, and backups

---

## License

Add your chosen license here.