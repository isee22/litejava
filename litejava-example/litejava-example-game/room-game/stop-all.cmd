@echo off
chcp 65001 >nul
echo ========================================
echo   Room Game - 停止所有服务
echo ========================================
echo.

echo 正在停止所有 Java 进程...
echo.

:: 通过窗口标题关闭
taskkill /FI "WINDOWTITLE eq Registry Server*" /F 2>nul
taskkill /FI "WINDOWTITLE eq Gateway Server*" /F 2>nul
taskkill /FI "WINDOWTITLE eq Lobby Server*" /F 2>nul
taskkill /FI "WINDOWTITLE eq Match Server*" /F 2>nul
taskkill /FI "WINDOWTITLE eq Doudizhu Server*" /F 2>nul
taskkill /FI "WINDOWTITLE eq Gobang Server*" /F 2>nul
taskkill /FI "WINDOWTITLE eq Mahjong Server*" /F 2>nul
taskkill /FI "WINDOWTITLE eq Chat Server*" /F 2>nul

echo.
echo ========================================
echo   所有服务已停止
echo ========================================
echo.
pause
