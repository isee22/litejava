#!/bin/bash
# LiteJava 轻量版启动脚本 (共享宿主机 JDK)

echo "=== LiteJava Microservice (Lite Mode) ==="
echo

# 检查 JAVA_HOME
if [ -z "$JAVA_HOME" ]; then
    echo "[ERROR] JAVA_HOME 未设置！"
    echo "请设置 JAVA_HOME 环境变量指向 JDK 安装目录"
    exit 1
fi

echo "JAVA_HOME: $JAVA_HOME"
echo

# 构建项目
echo "[1/3] 构建项目..."
mvn clean package -DskipTests -q
if [ $? -ne 0 ]; then
    echo "[ERROR] 构建失败！"
    exit 1
fi

# 启动服务
echo "[2/3] 启动基础设施..."
docker-compose -f docker-compose-lite.yml up -d consul redis zipkin
sleep 10

echo "[3/3] 启动业务服务..."
docker-compose -f docker-compose-lite.yml up -d

echo
echo "=== 启动完成 ==="
echo
echo "前端:    http://localhost:3000"
echo "网关:    http://localhost:8080"
echo "Consul:  http://localhost:8500"
echo "Zipkin:  http://localhost:9411"
echo
echo "查看日志: docker-compose -f docker-compose-lite.yml logs -f"
