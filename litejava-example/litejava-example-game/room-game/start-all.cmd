@echo off
chcp 65001 >nul
echo ========================================
echo   Room Game - BabyKylin 模式启动
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
timeout /t 2 /nobreak > nul

echo [4/4] 启动 五子棋服务器 (9200/9201)...
start "Gobang Server" cmd /c "mvn exec:java -pl games-java/game-gobang -Dexec.mainClass=game.gobang.GobangServer -q"

echo.
echo ========================================
echo   所有服务已启动！
echo ========================================
echo.
echo   架构: BabyKylin 模式 (HTTP + WebSocket 分离)
echo.
echo   服务端口:
echo   - Account:   8101 (HTTP) - 登录/玩家数据/排行/商城
echo   - Hall:      8201 (HTTP) - 房间/匹配/GameServer注册
echo   - 斗地主:    9100 (WS), 9101 (HTTP)
echo   - 五子棋:    9200 (WS), 9201 (HTTP)
echo.
echo   流程:
echo   1. 客户端 HTTP 调用 Account 登录
echo   2. 客户端 HTTP 调用 Hall 创建/加入房间或匹配
echo   3. 获取 token + wsUrl 后 WebSocket 连接 GameServer
echo.
echo   生产环境用 Nginx 做反向代理
echo.
pause
