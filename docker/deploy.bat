@echo off
chcp 65001 >nul
echo ============================================
echo  校圈 CampusHub - 一键上线部署
echo ============================================
echo.

:: 检查 .env 文件
if not exist "%~dp0.env" (
    echo [警告] 未找到 .env 文件，使用默认配置
    echo [提示] 复制 .env.example 为 .env 并修改敏感信息
    echo.
    copy "%~dp0.env.example" "%~dp0.env"
    echo [提示] 已从 .env.example 创建 .env，请编辑后重新运行
    echo.
    pause
    exit /b 1
)

:: 检查 Docker
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] Docker 未运行，请先启动 Docker Desktop
    pause
    exit /b 1
)

cd /d "%~dp0"

echo [1/4] 构建前端...
cd ..\campus-web
call npm install --silent
call npm run build
if %errorlevel% neq 0 (
    echo [错误] 前端构建失败
    pause
    exit /b 1
)
echo   前端构建完成 ✓

cd ..\docker

echo.
echo [2/4] 构建后端镜像...
docker-compose build campus-server
if %errorlevel% neq 0 (
    echo [错误] 后端镜像构建失败
    pause
    exit /b 1
)
echo   后端镜像构建完成 ✓

echo.
echo [3/4] 启动服务...
docker-compose up -d

echo.
echo [4/4] 等待服务就绪...
echo   这可能需要 1-2 分钟...

:: 等待 MySQL
echo   等待 MySQL...
:wait_mysql
docker exec campus-mysql mysqladmin ping -h localhost --silent >nul 2>&1
if %errorlevel% neq 0 ( timeout /t 3 >nul & goto wait_mysql )
echo   MySQL ✓

:: 等待后端
echo   等待后端...
:wait_app
curl -s http://localhost:8080/doc.html >nul 2>&1
if %errorlevel% neq 0 ( timeout /t 5 >nul & goto wait_app )
echo   后端 ✓

echo.
echo ============================================
echo  部署成功！服务已上线
echo.
echo  访问地址:
echo    平台首页:  http://localhost
echo    API 文档:  http://localhost/doc.html
echo    RabbitMQ:   http://localhost:15672
echo    MinIO:      http://localhost:9001
echo.
echo  管理命令:
echo    查看日志:   docker-compose logs -f
echo    停止服务:   docker-compose stop
echo    重启服务:   docker-compose restart
echo ============================================
pause
