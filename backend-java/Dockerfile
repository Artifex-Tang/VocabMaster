# ---- Runtime only (JAR built on host) ----
# 针对 2C2G 服务器优化，JVM 内存上限 384MB
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S vocab && adduser -S vocab -G vocab

COPY target/vocabmaster-backend.jar app.jar

RUN mkdir -p /var/log/vocabmaster && chown -R vocab:vocab /app /var/log/vocabmaster

USER vocab
EXPOSE 8080

# JVM 优化参数：
#   -Xmx384m / -Xms256m   : 堆内存控制在 384MB 以内
#   -XX:+UseSerialGC       : 单线程 GC，减少内存开销（2核够用）
#   -XX:+UseCompressedOops : 压缩对象指针，节省内存
#   -XX:MaxMetaspaceSize=128m : 限制元空间
#   -XX:+UseStringDeduplication : 字符串去重
#   环境变量 JAVA_OPTS 可在 docker-compose 中覆盖
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:--Xmx384m -Xms256m -XX:+UseSerialGC -XX:+UseCompressedOops -XX:MaxMetaspaceSize=128m -XX:+UseStringDeduplication} -jar app.jar"]
