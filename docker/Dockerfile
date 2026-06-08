FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app
COPY pom.xml .
COPY campus-common/pom.xml campus-common/
COPY campus-security/pom.xml campus-security/
COPY campus-user/pom.xml campus-user/
COPY campus-chat/pom.xml campus-chat/
COPY campus-rag/pom.xml campus-rag/
COPY campus-knowledge/pom.xml campus-knowledge/
COPY campus-file/pom.xml campus-file/
COPY campus-feedback/pom.xml campus-feedback/
COPY campus-app/pom.xml campus-app/

RUN mvn dependency:go-offline -B

COPY . .
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=builder /app/campus-app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
