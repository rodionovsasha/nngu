@echo off
setlocal
cd /d "%~dp0"
call gradlew.bat :compose:run %*
exit /b %errorlevel%
