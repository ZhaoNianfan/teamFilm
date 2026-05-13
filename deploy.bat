@echo off
chcp 65001 >nul
title TeamFiles Deploy
echo ============================================
echo   TeamFiles 免安装部署
echo   只需 Java 21，无需 MySQL/Redis
echo ============================================
echo.

:: Check Java
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] 未找到 Java，请先安装 JDK 21+
    echo 下载地址: https://adoptium.net/download/
    pause
    exit /b 1
)
echo [OK] Java 已就绪

:: Build frontend if needed
if not exist "teamfiles-web\dist\index.html" (
    echo.
    echo [1/3] 构建前端...
    cd teamfiles-web
    call npm install --silent
    call npm run build
    cd ..
    echo [OK] 前端构建完成
) else (
    echo [OK] 前端已构建
)

:: Build backend JAR if needed
if not exist "target\teamfiles-0.0.1-SNAPSHOT.jar" (
    echo.
    echo [2/3] 构建后端...
    call mvnw package -DskipTests -q
    echo [OK] 后端构建完成
) else (
    echo [OK] 后端已构建
)

:: Start application with H2 profile
echo.
echo [3/3] 启动服务...
echo.
echo ============================================
echo   服务地址: http://localhost:8088
echo   默认账号: admin / admin123
echo   H2 控制台: http://localhost:8088/h2-console
echo ============================================
echo.
java -jar target\teamfiles-0.0.1-SNAPSHOT.jar --spring.profiles.active=deploy
pause
