@echo off
REM ============================================
REM Accounts Receivable System - Run API Script
REM ============================================

echo.
echo Checking Java version...
java -version

echo.
echo ============================================
echo   Starting AR System API Server
echo   http://localhost:8081
echo ============================================
echo.

call gradlew.bat runApi --console=plain

pause
