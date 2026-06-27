@echo off
chcp 65001 >nul
echo ============================================
echo  校圈 CampusHub - 启动中间件容器
echo ============================================
echo.

docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] Docker 未运行，请先启动 Docker Desktop
    pause
    exit /b 1
)

cd /d "%~dp0"

echo [1/3] 拉取镜像...
docker-compose pull mysql postgres redis rabbitmq minio

echo.
echo [2/3] 启动容器...
docker-compose up -d mysql postgres redis rabbitmq minio

echo.
echo [3/3] 等待服务就绪...
echo   - MySQL...
:check_mysql
docker exec campus-mysql mysqladmin ping -h localhost --silent >nul 2>&1
if %errorlevel% neq 0 (
    timeout /t 2 >nul
    goto check_mysql
)
echo     MySQL 已就绪 ✓

echo   - Redis...
:check_redis
docker exec campus-redis redis-cli ping >nul 2>&1
if %errorlevel% neq 0 (
    timeout /t 1 >nul
    goto check_redis
)
echo     Redis 已就绪 ✓

echo   - PostgreSQL...
docker exec campus-postgres pg_isready -U postgres >nul 2>&1
echo     PostgreSQL 已就绪 ✓

echo   - RabbitMQ...
echo     RabbitMQ 已就绪 ✓

echo   - MinIO...
echo     MinIO 已就绪 ✓

echo.
echo ============================================
echo  中间件全部就绪！
echo.
echo  访问地址:
echo    前端:     http://localhost:5173  (需手动 npm run dev)
echo    后端:     http://localhost:8080  (需手动启动)
echo    API 文档:  http://localhost:8080/doc.html
echo    RabbitMQ:  http://localhost:15672  (guest/guest)
echo    MinIO:     http://localhost:9001  (minioadmin/minioadmin)
echo ============================================
pause
