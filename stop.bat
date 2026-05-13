@echo off
chcp 65001 >nul
title TeamFiles - Stopping...

echo Stopping TeamFiles...

:: Kill Java processes running teamfiles
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8088') do (
    taskkill /PID %%a /F >nul 2>&1
)

echo TeamFiles stopped.
pause
