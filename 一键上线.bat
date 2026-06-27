@echo off
chcp 65001 >nul
echo ============================================
echo  校圈 CampusHub - 一键上线
echo ============================================
echo.

:: 检查 Docker
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] Docker 未运行！请先启动 Docker Desktop
    pause
    exit /b 1
)

cd /d "%~dp0"

:: 1. 启动中间件
echo [1/5] 启动中间件...
cd docker
docker-compose up -d mysql postgres redis rabbitmq minio
echo 等待 MySQL 就绪...
timeout /t 15 /nobreak >nul
echo   中间件已就绪 ✓

:: 2. 启动 ngrok 并获取 URL
echo.
echo [2/5] 启动 ngrok...
start "ngrok" cmd /c "ngrok http 8080 --log=stdout > %temp%\ngrok-url.txt 2>&1"
echo 等待 ngrok 获取公网地址...
timeout /t 5 /nobreak >nul

:: 从 ngrok API 获取公网 URL
for /f "tokens=*" %%a in ('curl -s http://127.0.0.1:4040/api/tunnels 2^>nul ^| findstr "public_url"') do (
    for /f "tokens=2 delims=: " %%b in ("%%a") do (
        set NGROK_URL=%%b
        set NGROK_URL=!NGROK_URL:"=!
        set NGROK_URL=!NGROK_URL:,=!
    )
)

echo   ngrok URL: %NGROK_URL%

:: 3. 更新前端配置
echo.
echo [3/5] 更新前端 API 地址...
cd ..\campus-web
echo VITE_API_BASE_URL=%NGROK_URL%> .env.production
echo   已更新为 %NGROK_URL% ✓

:: 4. 构建前端
echo [4/5] 构建前端...
call npm run build
echo   前端构建完成 ✓

:: 5. 提交并推送
echo [5/5] 推送部署...
cd ..
git add campus-web/.env.production campus-web/dist
git commit -m "auto: 更新 ngrok URL - %NGROK_URL%" 2>nul
git push origin master --force 2>nul
echo   已推送，等待 GitHub Actions 部署...

echo.
echo ============================================
echo  部署完成！1分钟后访问:
echo    http://campus.qinlaihuie2.xyz
echo  后端还在启动中，请等待 20 秒...
echo ============================================
echo.
echo  提示: 现在打开新终端启动后端:
echo    cd campus-server\campus-app
echo    set DEEPSEEK_API_KEY=sk-你的key
echo    mvn spring-boot:run
echo ============================================
pause
