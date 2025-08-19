# UrbanFlow Docker 部署指南

## 项目概述

UrbanFlow是一个基于Spring Boot的微服务架构项目，包含以下服务模块：

- **user-authentication** (8080): 用户认证服务
- **signal-control** (8081): 信号控制服务  
- **traffic-sensing** (8082): 交通感知服务
- **ai-intelligence** (8083): AI智能服务
- **special-event-handling** (8084): 特殊事件处理服务
- **logging-and-audit** (8085): 日志和审计服务
- **status-sync** (8086): 状态同步服务

## 基础设施服务

- **MySQL** (3306): 主数据库
- **Redis** (6379): 缓存服务
- **InfluxDB** (9999): 时序数据库

## 快速开始

### 1. 环境要求

- Docker 20.10+
- Docker Compose 2.0+
- 至少 4GB 可用内存

### 2. 启动所有服务

```bash
# 构建并启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看服务日志
docker-compose logs -f
```

### 3. 单独构建和启动服务

```bash
# 构建特定服务
docker-compose build user-authentication

# 启动特定服务
docker-compose up -d user-authentication

# 重启特定服务
docker-compose restart user-authentication
```

### 4. 查看服务日志

```bash
# 查看所有服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f user-authentication

# 查看最近100行日志
docker-compose logs --tail=100 user-authentication
```

## 服务端口映射

| 服务 | 容器端口 | 主机端口 | 说明 |
|------|----------|----------|------|
| user-authentication | 8080 | 8080 | 用户认证服务 |
| signal-control | 8081 | 8081 | 信号控制服务 |
| traffic-sensing | 8082 | 8082 | 交通感知服务 |
| ai-intelligence | 8083 | 8083 | AI智能服务 |
| special-event-handling | 8084 | 8084 | 特殊事件处理 |
| logging-and-audit | 8085 | 8085 | 日志和审计 |
| status-sync | 8086 | 8086 | 状态同步 |
| mysql | 3306 | 3306 | MySQL数据库 |
| redis | 6379 | 6379 | Redis缓存 |
| influxdb | 8086 | 9999 | InfluxDB时序数据库 |

## 环境变量配置

### 数据库配置
- `SPRING_DATASOURCE_URL`: MySQL连接URL
- `SPRING_DATASOURCE_USERNAME`: 数据库用户名
- `SPRING_DATASOURCE_PASSWORD`: 数据库密码

### Redis配置
- `SPRING_REDIS_HOST`: Redis主机地址
- `SPRING_REDIS_PORT`: Redis端口

### InfluxDB配置
- `INFLUXDB_URL`: InfluxDB服务地址
- `INFLUXDB_TOKEN`: InfluxDB访问令牌
- `INFLUXDB_ORG`: InfluxDB组织
- `INFLUXDB_BUCKET`: InfluxDB存储桶

## 健康检查

所有服务都配置了健康检查，确保服务依赖关系正确：

```bash
# 查看服务健康状态
docker-compose ps

# 查看特定服务的健康检查
docker inspect urbanflow-user-auth | grep -A 10 "Health"
```

## 数据持久化

- MySQL数据存储在 `db_data` 卷中
- InfluxDB数据存储在 `influxdb_data` 卷中

```bash
# 查看卷信息
docker volume ls

# 备份数据卷
docker run --rm -v urbanflow_db_data:/data -v $(pwd):/backup alpine tar czf /backup/mysql-backup.tar.gz -C /data .
```

## 故障排除

### 1. 服务启动失败

```bash
# 查看详细错误信息
docker-compose logs service-name

# 检查服务依赖
docker-compose ps
```

### 2. 内存不足

如果遇到内存不足问题，可以调整JVM参数：

```yaml
environment:
  - JAVA_OPTS=-Xmx512m -Xms256m
```

### 3. 端口冲突

如果端口被占用，可以修改 `docker-compose.yaml` 中的端口映射：

```yaml
ports:
  - "8087:8080"  # 将主机端口改为8087
```

## 开发环境

### 1. 本地开发

```bash
# 只启动基础设施服务
docker-compose up -d mysql redis influxdb

# 在IDE中运行应用，连接到Docker中的数据库
```

### 2. 热重载开发

```bash
# 使用开发模式启动（需要修改Dockerfile）
docker-compose -f docker-compose.dev.yml up
```

## 生产环境部署

### 1. 安全配置

- 修改默认密码
- 使用环境变量文件
- 配置SSL/TLS
- 设置防火墙规则

### 2. 性能优化

- 调整JVM参数
- 配置数据库连接池
- 启用Redis集群
- 配置负载均衡

### 3. 监控和日志

- 配置日志聚合
- 设置监控告警
- 配置备份策略

## 常用命令

```bash
# 停止所有服务
docker-compose down

# 停止并删除数据卷
docker-compose down -v

# 重新构建所有镜像
docker-compose build --no-cache

# 清理未使用的镜像和容器
docker system prune -a

# 查看资源使用情况
docker stats
```

## 注意事项

1. 首次启动时，数据库初始化可能需要一些时间
2. 确保有足够的磁盘空间用于数据存储
3. 生产环境中请修改默认密码和敏感信息
4. 定期备份重要数据
5. 监控服务资源使用情况 