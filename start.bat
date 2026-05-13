@echo off
chcp 65001 >nul
title TeamFiles - Starting...

echo ============================================
echo   TeamFiles 局域网团队文件管理系统
echo ============================================
echo.

:: Check Java
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java not found. Please install JDK 21+
    pause
    exit /b 1
)

:: Check MySQL
sc query MySQL84 | find "RUNNING" >nul
if %errorlevel% neq 0 (
    echo [WARN] MySQL84 service is not running, attempting to start...
    net start MySQL84
    if %errorlevel% neq 0 (
        echo [ERROR] Cannot start MySQL. Please start it manually.
        pause
        exit /b 1
    )
)
echo [OK] MySQL is running

:: Check Redis
sc query Redis | find "RUNNING" >nul
if %errorlevel% neq 0 (
    echo [WARN] Redis service is not running, attempting to start...
    net start Redis
    if %errorlevel% neq 0 (
        echo [WARN] Cannot start Redis. Some features may not work.
    )
) else (
    echo [OK] Redis is running
)

:: Build frontend if needed
if not exist "teamfiles-web\dist\index.html" (
    echo.
    echo [INFO] Building frontend...
    cd teamfiles-web
    call npm install
    call npm run build
    cd ..
)

:: Start backend
echo.
echo [INFO] Starting TeamFiles backend on port 8088...
start "TeamFiles Backend" cmd /c "mvnw spring-boot:run"
echo.
echo [INFO] Backend starting... Please wait ~10 seconds.
echo [INFO] Open http://localhost:8088 in your browser.
echo.
echo Default login: admin / admin123
echo.
echo ============================================
echo   Close this window to keep server running
echo ============================================
pause
