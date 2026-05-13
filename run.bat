@echo off
chcp 65001 >nul
title TeamFiles
echo ============================================
echo   TeamFiles - 启动中...
echo ============================================
java -jar target\teamfiles-0.0.1-SNAPSHOT.jar --spring.profiles.active=deploy
pause
