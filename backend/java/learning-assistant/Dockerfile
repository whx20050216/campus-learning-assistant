FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
COPY target/learning-assistant-0.0.1-SNAPSHOT.jar app.jar

ENV TZ=Asia/Shanghai
RUN apk add --no-cache tzdata curl

EXPOSE 8080

ENV JAVA_OPTS="-Xms512m -Xmx1024m -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
