#!/bin/bash
# 职安通 - 智能职业健康安全管理平台
# 启动脚本

export JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"

echo "=== Java 版本 ==="
java -version 2>&1 | head -1

echo ""
echo "=== 启动职安通应用 ==="
echo "URL: http://localhost:8080"
echo ""

./mvnw spring-boot:run
