@echo off
chcp 65001 >nul
echo ============================================
echo  校圈 CampusHub - 停止中间件容器
echo ============================================
cd /d "%~dp0"
docker-compose stop
echo.
echo 所有容器已停止。数据卷保留未删除。
echo 如需完全清理（包括数据），请运行: docker-compose down -v
pause
