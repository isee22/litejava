@echo off
chcp 65001 >nul
echo ========================================
echo   Room Game - 机器人测试启动
echo ========================================
echo.

cd /d %~dp0

echo [1/4] 启动 Account Server (8101)...
start "Account Server" cmd /c "mvn exec:java -pl account-server -Dexec.mainClass=game.account.AccountServer -q"
timeout /t 3 /nobreak > nul

echo [2/4] 启动 Hall Server (8201)...
start "Hall Server" cmd /c "mvn exec:java -pl hall-server -Dexec.mainClass=game.hall.HallServer -q"
timeout /t 3 /nobreak > nul

echo [3/4] 启动 斗地主服务器 (9100/9101)...
start "Doudizhu Server" cmd /c "mvn exec:java -pl games-java/game-doudizhu -Dexec.mainClass=game.doudizhu.DoudizhuServer -q"
timeout /t 3 /nobreak > nul

echo [4/4] 启动 机器人客户端 (8900)...
start "Robot Client" cmd /c "mvn exec:java -pl robot -Dexec.mainClass=game.robot.RobotClient -q"

echo.
echo ========================================
echo   机器人测试环境已启动！
echo ========================================
echo.
echo   服务端口:
echo   - Account:   8101 (HTTP)
echo   - Hall:      8201 (HTTP)
echo   - 斗地主:    9100 (WS), 9101 (HTTP)
echo   - 机器人:    8900 (HTTP 管理接口)
echo.
echo   机器人会自动:
echo   1. 启动后等待3秒
echo   2. 创建100个斗地主机器人
echo   3. 每个机器人自动快速开始 (有房间加入，没房间创建)
echo   4. 进入房间后自动准备
echo   5. 游戏中自动出牌 (带思考时间)
echo   6. 游戏结束后重新匹配
echo.
echo   管理接口:
echo   - GET  http://localhost:8900/status  查看状态
echo   - POST http://localhost:8900/game/doudizhu  设置机器人
echo   - GET  http://localhost:8900/health  健康检查
echo.
pause
