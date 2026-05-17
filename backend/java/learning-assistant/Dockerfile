# 多阶段构建：先编译，再运行
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /build
COPY pom.xml .
COPY src ./src

# 离线编译（如依赖已下载可加速）
RUN mvn clean package -DskipTests -q

# 运行阶段
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 从构建阶段拷贝 jar
COPY --from=builder /build/target/*.jar app.jar

# 时区
ENV TZ=Asia/Shanghai
RUN apk add --no-cache tzdata curl

EXPOSE 8080

# JVM 参数可通过环境变量覆盖
ENV JAVA_OPTS="-Xms512m -Xmx1024m -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
