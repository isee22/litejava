# Room Game - BabyKylin 模式启动脚本 (PowerShell)

Write-Host "========================================"
Write-Host "  Room Game - BabyKylin 模式启动"
Write-Host "========================================"
Write-Host ""

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

Write-Host "[1/4] 启动 Account Server (8101)..."
Start-Process -FilePath "cmd" -ArgumentList "/c", "mvn exec:java -pl account-server -Dexec.mainClass=game.account.AccountServer -q" -WindowStyle Normal
Start-Sleep -Seconds 3

Write-Host "[2/4] 启动 Hall Server (8201)..."
Start-Process -FilePath "cmd" -ArgumentList "/c", "mvn exec:java -pl hall-server -Dexec.mainClass=game.hall.HallServer -q" -WindowStyle Normal
Start-Sleep -Seconds 3

Write-Host "[3/4] 启动 斗地主服务器 (9100/9101)..."
Start-Process -FilePath "cmd" -ArgumentList "/c", "mvn exec:java -pl game-doudizhu -Dexec.mainClass=game.doudizhu.DoudizhuServer -q" -WindowStyle Normal
Start-Sleep -Seconds 2

Write-Host "[4/4] 启动 五子棋服务器 (9200/9201)..."
Start-Process -FilePath "cmd" -ArgumentList "/c", "mvn exec:java -pl game-gobang -Dexec.mainClass=game.gobang.GobangServer -q" -WindowStyle Normal

Write-Host ""
Write-Host "========================================"
Write-Host "  所有服务已启动！"
Write-Host "========================================"
Write-Host ""
Write-Host "  架构: BabyKylin 模式 (HTTP + WebSocket 分离)"
Write-Host ""
Write-Host "  服务端口:"
Write-Host "  - Account:   8101 (HTTP) - 登录/玩家数据/排行/商城"
Write-Host "  - Hall:      8201 (HTTP) - 房间/匹配/GameServer注册"
Write-Host "  - 斗地主:    9100 (WS), 9101 (HTTP)"
Write-Host "  - 五子棋:    9200 (WS), 9201 (HTTP)"
Write-Host ""
Write-Host "  流程:"
Write-Host "  1. 客户端 HTTP 调用 Account 登录"
Write-Host "  2. 客户端 HTTP 调用 Hall 创建/加入房间或匹配"
Write-Host "  3. 获取 token + wsUrl 后 WebSocket 连接 GameServer"
Write-Host ""
Write-Host "  生产环境用 Nginx 做反向代理"
Write-Host ""
Read-Host "按回车键退出"
