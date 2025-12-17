@echo off
REM ============================================
REM Accounts Receivable System - Run Desktop App
REM ============================================

echo.
echo Checking Java version...
java -version

echo.
echo ============================================
echo   Starting AR System Desktop App
echo ============================================
echo.

call gradlew.bat run --console=plain

pause
