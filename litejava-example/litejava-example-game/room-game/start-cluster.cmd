@echo off
chcp 65001 >nul
echo ========================================
echo   Room Game - 集群模式启动
echo   (多实例显式端口分配)
echo ========================================
echo.

cd /d %~dp0

echo [1/9] 启动 Registry Server (8000)...
start "Registry Server" cmd /c "mvn exec:java -pl registry-server -Dexec.mainClass=game.registry.RegistryServer -q"
timeout /t 3 /nobreak > nul

echo [2/9] 启动 Account Server (8101)...
start "Account Server" cmd /c "mvn exec:java -pl account-server -Dexec.mainClass=game.account.AccountServer -q"
timeout /t 3 /nobreak > nul

echo [3/9] 启动 Hall Server (8201)...
start "Hall Server" cmd /c "mvn exec:java -pl hall-server -Dexec.mainClass=game.hall.HallServer -q"
timeout /t 3 /nobreak > nul

echo [4/9] 启动 斗地主服务器 #1 (9100/9101)...
start "Doudizhu Server 1" cmd /c "mvn exec:java -pl game-doudizhu -Dexec.mainClass=game.doudizhu.DoudizhuServer -q"
timeout /t 2 /nobreak > nul

echo [5/9] 启动 斗地主服务器 #2 (9102/9103)...
start "Doudizhu Server 2" cmd /c "mvn exec:java -pl game-doudizhu -Dexec.mainClass=game.doudizhu.DoudizhuServer -Dserver.wsPort=9102 -Dserver.httpPort=9103 -q"
timeout /t 2 /nobreak > nul

echo [6/9] 启动 五子棋服务器 #1 (9200/9201)...
start "Gobang Server 1" cmd /c "mvn exec:java -pl game-gobang -Dexec.mainClass=game.gobang.GobangServer -q"
timeout /t 2 /nobreak > nul

echo [7/9] 启动 五子棋服务器 #2 (9202/9203)...
start "Gobang Server 2" cmd /c "mvn exec:java -pl game-gobang -Dexec.mainClass=game.gobang.GobangServer -Dserver.wsPort=9202 -Dserver.httpPort=9203 -q"
timeout /t 2 /nobreak > nul

echo [8/9] 启动 麻将服务器 #1 (9300/9301)...
start "Mahjong Server 1" cmd /c "mvn exec:java -pl game-mahjong -Dexec.mainClass=game.mahjong.MahjongServer -q"
timeout /t 2 /nobreak > nul

echo [9/9] 启动 麻将服务器 #2 (9302/9303)...
start "Mahjong Server 2" cmd /c "mvn exec:java -pl game-mahjong -Dexec.mainClass=game.mahjong.MahjongServer -Dserver.wsPort=9302 -Dserver.httpPort=9303 -q"

echo.
echo ========================================
echo   集群模式已启动！
echo ========================================
echo.
echo   注册中心管理界面: http://localhost:8000
echo.
echo   服务端口:
echo   - Registry:  8000 (HTTP)
echo   - Account:   8101 (HTTP)
echo   - Hall:      8201 (HTTP)
echo.
echo   游戏服务器集群:
echo   - 斗地主 #1: 9100/9101
echo   - 斗地主 #2: 9102/9103
echo   - 五子棋 #1: 9200/9201
echo   - 五子棋 #2: 9202/9203
echo   - 麻将 #1:   9300/9301
echo   - 麻将 #2:   9302/9303
echo.
echo   提示: 生产环境使用 Nginx 做反向代理
echo.
pause
