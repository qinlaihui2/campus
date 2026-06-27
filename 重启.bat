@echo off
chcp 65001 >nul
echo ============================================
echo  校圈 CampusHub - 快速重启
echo ============================================
echo.

:: 检查 Docker
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] Docker 未运行！请先启动 Docker Desktop
    pause
    exit /b 1
)

:: 第 1 步：启动中间件
echo [1/4] 启动中间件...
cd /d "%~dp0docker"
docker-compose up -d mysql postgres redis rabbitmq minio
echo   等待 MySQL 就绪...
timeout /t 15 /nobreak >nul
echo   中间件已启动 ✓

:: 第 2 步：启动 ngrok
echo.
echo [2/4] 启动 ngrok...
start "ngrok" cmd /c "ngrok http 8080"
echo   ngrok 已在新窗口启动 ✓

:: 第 3 步：提示后端
echo.
echo [3/4] 请在新终端启动后端：
echo   cd E:\java\claude-myblog\campus-server\campus-app
echo   set DEEPSEEK_API_KEY=sk-你的密钥
echo   mvn spring-boot:run
echo.

:: 第 4 步：提示更新 URL
echo [4/4] ngrok 启动后会显示一个 URL
echo   把它更新到 campus-web\.env.production
echo   然后执行：
echo     git add . ^&^& git commit -m "update ngrok" ^&^& git push
echo.
echo ============================================
echo  完成后访问 http://campus.qinlaihuie2.xyz
echo ============================================
pause
