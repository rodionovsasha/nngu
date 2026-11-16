@echo off
setlocal
cd /d "%~dp0"
call gradlew.bat :javafx:run %*
exit /b %errorlevel%
