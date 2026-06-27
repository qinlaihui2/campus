# ============================================
# 校圈 CampusHub - 多阶段构建
# ============================================

# --- Stage 1: Maven 编译 ---
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# 先复制父 POM + 所有模块 POM，利用 Docker 层缓存加速依赖下载
COPY pom.xml .
COPY campus-common/pom.xml campus-common/
COPY campus-security/pom.xml campus-security/
COPY campus-user/pom.xml campus-user/
COPY campus-rag/pom.xml campus-rag/
COPY campus-chat/pom.xml campus-chat/
COPY campus-knowledge/pom.xml campus-knowledge/
COPY campus-file/pom.xml campus-file/
COPY campus-feedback/pom.xml campus-feedback/
COPY campus-course/pom.xml campus-course/
COPY campus-square/pom.xml campus-square/
COPY campus-message/pom.xml campus-message/
COPY campus-lost-found/pom.xml campus-lost-found/
COPY campus-announcement/pom.xml campus-announcement/
COPY campus-my/pom.xml campus-my/
COPY campus-market/pom.xml campus-market/
COPY campus-notification/pom.xml campus-notification/
COPY campus-app/pom.xml campus-app/

RUN mvn dependency:go-offline -B -q

# 复制全部源码并编译
COPY . .
RUN mvn clean package -DskipTests -B -q

# --- Stage 2: JRE 运行镜像 ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 创建日志目录
RUN mkdir -p /app/logs

COPY --from=builder /app/campus-app/target/*.jar app.jar

EXPOSE 8080

# JVM 参数（生产环境可调整）
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
