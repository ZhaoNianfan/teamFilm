@echo off
title TeamFiles Deploy
echo ============================================
echo   TeamFiles - Portable Deployment
echo   Requires: Java 21 only
echo   First run needs internet (~5 min for deps)
echo   Subsequent launches use run.bat
echo ============================================
echo.

:: Check Java
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java not found. Please install JDK 21+
    echo Download: https://adoptium.net/download/
    pause
    exit /b 1
)
echo [OK] Java is ready

:: Build frontend if needed
if not exist "teamfiles-web\dist\index.html" (
    echo.
    echo [1/3] Building frontend...
    cd teamfiles-web
    call npm install --silent
    call npm run build
    cd ..
    echo [OK] Frontend build done
) else (
    echo [OK] Frontend already built
)

:: Build backend JAR if needed
if not exist "target\teamfiles-0.0.1-SNAPSHOT.jar" (
    echo.
    echo [2/3] Building backend...
    call mvnw package -DskipTests -q
    echo [OK] Backend build done
) else (
    echo [OK] Backend already built
)

:: Start application with H2 profile
echo.
echo [3/3] Starting service...
echo.
echo ============================================
echo   URL: http://localhost:8088
echo   Account: admin / admin123
echo   H2 Console: http://localhost:8088/h2-console
echo ============================================
echo.
java -jar target\teamfiles-0.0.1-SNAPSHOT.jar --spring.profiles.active=deploy
pause
