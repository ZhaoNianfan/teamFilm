@echo off
title TeamFiles
echo ============================================
echo   TeamFiles - Starting...
echo ============================================
java -jar target\teamfiles-0.0.1-SNAPSHOT.jar --spring.profiles.active=deploy
pause
