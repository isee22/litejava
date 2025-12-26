# Serial Benchmark - Test one server at a time for fair comparison
# This avoids JVM competition for CPU/memory

param(
    [int]$Requests = 200000,
    [int]$Concurrency = 200,
    [int]$DbRequests = 50000,
    [int]$DbConcurrency = 100,
    [int]$WarmupRequests = 50000,
    [int]$Rounds = 5
)

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# Server configurations
$servers = [ordered]@{
    "Gin" = @{ Port = 8184; Type = "go"; Dir = "benchmark-gin" }
    "JdkVT" = @{ Port = 8185; Type = "java"; Jar = "benchmark-litejava-jdkvt\target\benchmark-litejava-jdkvt-1.0.0-SNAPSHOT.jar" }
    "JettyVT" = @{ Port = 8187; Type = "java"; Jar = "benchmark-litejava-jettyvt\target\benchmark-litejava-jettyvt-1.0.0-SNAPSHOT.jar" }
    "Netty" = @{ Port = 8186; Type = "java"; Jar = "benchmark-litejava-netty\target\benchmark-litejava-netty-1.0.0-SNAPSHOT.jar" }
    "Javalin" = @{ Port = 8182; Type = "java"; Jar = "benchmark-javalin\target\benchmark-javalin-1.0.0-SNAPSHOT.jar" }
    "SpringBoot" = @{ Port = 8183; Type = "java"; Jar = "benchmark-springboot\target\benchmark-springboot-1.0.0-SNAPSHOT.jar" }
}

$results = @{}
$latencyResults = @{}

function Write-Title($text) {
    Write-Host "`n========== $text ==========" -ForegroundColor Cyan
}

function Stop-AllServers {
    Write-Host "Stopping all servers..." -ForegroundColor Yellow
    
    # 强制释放所有 benchmark 端口
    $ports = @(8181, 8182, 8183, 8184, 8185, 8186, 8187)
    foreach ($port in $ports) {
        $netstat = netstat -ano | Select-String ":$port\s+.*LISTENING"
        if ($netstat) {
            $pid = ($netstat -split '\s+')[-1]
            if ($pid -match '^\d+$') {
                Write-Host "  Killing process on port $port (PID: $pid)" -ForegroundColor Gray
                taskkill /F /PID $pid 2>$null | Out-Null
            }
        }
    }
    
    # 额外杀掉所有 java 和 gin 进程
    Get-Process -Name java -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
    Get-Process -Name gin-server -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
    
    Start-Sleep -Seconds 3
}

function Start-Server($name, $config) {
    Write-Host "Starting $name..." -NoNewline
    
    if ($config.Type -eq "go") {
        $ginDir = "$scriptDir\$($config.Dir)"
        $ginExe = "$ginDir\gin-server.exe"
        Push-Location $ginDir
        go build -o gin-server.exe main.go 2>$null
        Pop-Location
        $proc = Start-Process -FilePath $ginExe -WorkingDirectory $ginDir -WindowStyle Hidden -PassThru
    } else {
        $jarPath = "$scriptDir\$($config.Jar)"
        # JVM optimization flags - 与 CMD 版本一致
        $jvmArgs = @(
            "-Xms512m",
            "-Xmx512m",
            "-XX:+UseZGC",
            "-XX:+ZGenerational",
            "-jar", $jarPath
        )
        $proc = Start-Process -FilePath "java" -ArgumentList $jvmArgs -WindowStyle Hidden -PassThru
    }
    
    $waitTime = if ($config.Wait) { $config.Wait } else { 5 }
    Start-Sleep -Seconds $waitTime
    
    $port = $config.Port
    for ($i = 0; $i -lt 10; $i++) {
        try {
            $response = Invoke-WebRequest -Uri "http://localhost:$port/text" -UseBasicParsing -TimeoutSec 2
            if ($response.StatusCode -eq 200) {
                Write-Host " OK" -ForegroundColor Green
                return $proc
            }
        } catch { }
        Start-Sleep -Seconds 1
    }
    Write-Host " FAILED" -ForegroundColor Red
    return $null
}

function Stop-Server($proc, $port) {
    if ($proc) {
        Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
    }
    # 确保端口释放
    if ($port) {
        $netstat = netstat -ano | Select-String ":$port\s+.*LISTENING"
        if ($netstat) {
            $pid = ($netstat -split '\s+')[-1]
            if ($pid -match '^\d+$') {
                taskkill /F /PID $pid 2>$null | Out-Null
            }
        }
    }
    Start-Sleep -Seconds 2
}

function Get-BenchmarkResult($port, $path, $n, $c) {
    $url = "http://localhost:$port$path"
    $output = hey -n $n -c $c $url 2>&1
    
    $qps = 0; $avgLatency = 0; $p99Latency = 0; $successRate = 100
    
    $qpsMatch = $output | Select-String "Requests/sec:\s+([\d.]+)"
    if ($qpsMatch) { $qps = [math]::Round([double]$qpsMatch.Matches[0].Groups[1].Value) }
    
    $avgMatch = $output | Select-String "Average:\s+([\d.]+)\s+secs"
    if ($avgMatch) { $avgLatency = [math]::Round([double]$avgMatch.Matches[0].Groups[1].Value * 1000, 2) }
    
    $p99Match = $output | Select-String "99%\s+in\s+([\d.]+)\s+secs"
    if ($p99Match) { $p99Latency = [math]::Round([double]$p99Match.Matches[0].Groups[1].Value * 1000, 2) }
    
    # 统计成功率
    $status200Match = $output | Select-String "\[200\]\s+(\d+)\s+responses"
    if ($status200Match) {
        $success = [int]$status200Match.Matches[0].Groups[1].Value
        $successRate = [math]::Round($success / $n * 100, 2)
    }
    
    return @{ QPS = $qps; AvgLatency = $avgLatency; P99Latency = $p99Latency; SuccessRate = $successRate }
}

# 多轮测试取平均
function Get-AverageBenchmarkResult($port, $path, $n, $c, $rounds) {
    $totalQps = 0; $totalAvg = 0; $totalP99 = 0; $totalSuccess = 0
    
    for ($r = 1; $r -le $rounds; $r++) {
        Write-Host "    Round $r/$rounds..." -NoNewline
        $result = Get-BenchmarkResult $port $path $n $c
        $totalQps += $result.QPS
        $totalAvg += $result.AvgLatency
        $totalP99 += $result.P99Latency
        $totalSuccess += $result.SuccessRate
        
        $successInfo = if ($result.SuccessRate -lt 100) { " ($($result.SuccessRate)% ok)" } else { "" }
        Write-Host " $($result.QPS) QPS$successInfo" -ForegroundColor Gray
    }
    
    return @{
        QPS = [math]::Round($totalQps / $rounds)
        AvgLatency = [math]::Round($totalAvg / $rounds, 2)
        P99Latency = [math]::Round($totalP99 / $rounds, 2)
        SuccessRate = [math]::Round($totalSuccess / $rounds, 2)
    }
}

function Run-Warmup($port) {
    Write-Host "  Warmup ($WarmupRequests requests)..." -NoNewline
    hey -n $WarmupRequests -c 50 "http://localhost:$port/json" | Out-Null
    Write-Host " Done" -ForegroundColor Green
}

$endpoints = @(
    @{ Path = "/text"; Name = "Plain Text"; N = $Requests; C = $Concurrency },
    @{ Path = "/json"; Name = "JSON"; N = $Requests; C = $Concurrency },
    @{ Path = "/dynamic"; Name = "Dynamic Page"; N = $DbRequests; C = $DbConcurrency },
    @{ Path = "/users?page=1&size=10"; Name = "DB Users"; N = $DbRequests; C = $DbConcurrency },
    @{ Path = "/posts?page=1&size=10"; Name = "DB Posts"; N = $DbRequests; C = $DbConcurrency }
)

foreach ($ep in $endpoints) {
    $results[$ep.Path] = @{}
    $latencyResults[$ep.Path] = @{}
}

Write-Title "Serial Benchmark - $Rounds Rounds Average"
Write-Host "This ensures fair comparison without JVM competition"
Write-Host "Requests: $Requests | Concurrency: $Concurrency | Warmup: $WarmupRequests | Rounds: $Rounds"

Stop-AllServers

foreach ($server in $servers.GetEnumerator()) {
    $name = $server.Key
    $config = $server.Value
    $port = $config.Port
    
    Write-Title "Testing $name (port $port)"
    
    $proc = Start-Server $name $config
    if (-not $proc) {
        Write-Host "  Skipping $name - failed to start" -ForegroundColor Red
        continue
    }
    
    Run-Warmup $port
    
    foreach ($ep in $endpoints) {
        Write-Host "  $($ep.Name) ($Rounds rounds):" -ForegroundColor Yellow
        $result = Get-AverageBenchmarkResult $port $ep.Path $ep.N $ep.C $Rounds
        $results[$ep.Path][$name] = $result.QPS
        $latencyResults[$ep.Path][$name] = @{ Avg = $result.AvgLatency; P99 = $result.P99Latency; Success = $result.SuccessRate }
        $successInfo = if ($result.SuccessRate -lt 100) { ", $($result.SuccessRate)% ok" } else { "" }
        Write-Host "  -> Average: $($result.QPS) QPS, $($result.AvgLatency)ms avg$successInfo" -ForegroundColor Green
    }
    
    Stop-Server $proc $port
    Write-Host "  Stopped $name" -ForegroundColor Gray
}

Write-Title "Generating Report"

$timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
$ginQps = @{
    "/json" = $results["/json"]["Gin"]
    "/text" = $results["/text"]["Gin"]
    "/dynamic" = $results["/dynamic"]["Gin"]
    "/users?page=1&size=10" = $results["/users?page=1&size=10"]["Gin"]
    "/posts?page=1&size=10" = $results["/posts?page=1&size=10"]["Gin"]
}
$ginLat = @{
    "/json" = $latencyResults["/json"]["Gin"]
    "/text" = $latencyResults["/text"]["Gin"]
    "/dynamic" = $latencyResults["/dynamic"]["Gin"]
}

function Get-BarColor($name, $pct) {
    if ($name -eq "Gin") { return "linear-gradient(90deg, #ff9800, #ffb74d)" }
    if ($pct -ge 100) { return "linear-gradient(90deg, #4CAF50, #8BC34A)" }
    if ($name -like "*VT" -or $name -eq "Netty") { return "linear-gradient(90deg, #2196F3, #64B5F6)" }
    if ($name -eq "SpringBoot") { return "linear-gradient(90deg, #9C27B0, #BA68C8)" }
    if ($name -eq "Javalin") { return "linear-gradient(90deg, #4CAF50, #8BC34A)" }
    if ($pct -lt 50) { return "linear-gradient(90deg, #f44336, #e57373)" }
    return "linear-gradient(90deg, #2196F3, #64B5F6)"
}

$html = @"
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>LiteJava Serial Benchmark</title>
    <style>
        * { box-sizing: border-box; }
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 0; padding: 40px; background: #f5f5f5; }
        .container { max-width: 1200px; margin: 0 auto; }
        h1 { color: #333; border-bottom: 3px solid #2196F3; padding-bottom: 10px; }
        h2 { color: #555; margin-top: 30px; margin-bottom: 20px; }
        .timestamp { color: #888; font-size: 14px; }
        .section { background: white; border-radius: 12px; padding: 25px; margin: 20px 0; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
        .note { background: #e3f2fd; border-left: 4px solid #2196F3; padding: 15px; margin: 20px 0; border-radius: 4px; }
        .summary { background: white; padding: 30px; border-radius: 12px; margin: 20px 0; box-shadow: 0 2px 12px rgba(0,0,0,0.1); display: flex; justify-content: center; flex-wrap: wrap; }
        .metric { margin: 15px 30px; text-align: center; }
        .metric-value { font-size: 42px; font-weight: bold; color: #2196F3; }
        .metric-label { color: #666; font-size: 14px; }
        .chart-container { margin: 30px 0; }
        .chart-row { display: flex; align-items: center; margin: 12px 0; }
        .chart-label { width: 100px; font-weight: 500; color: #333; text-align: right; padding-right: 15px; }
        .chart-bar-wrapper { flex: 1; position: relative; height: 32px; }
        .chart-bar { height: 100%; border-radius: 4px; display: flex; align-items: center; justify-content: flex-end; padding-right: 10px; color: white; font-weight: bold; font-size: 13px; min-width: 50px; }
        .chart-value { margin-left: 10px; color: #666; font-size: 13px; min-width: 140px; }
        table { border-collapse: collapse; width: 100%; margin: 20px 0; }
        th, td { padding: 12px; text-align: right; border: 1px solid #e0e0e0; }
        th { background: #2196F3; color: white; }
        td:first-child, th:first-child { text-align: left; }
        .good { color: #4CAF50; font-weight: bold; }
        .bad { color: #f44336; }
    </style>
</head>
<body>
<div class="container">
    <h1>LiteJava Serial Benchmark Results</h1>
    <p class="timestamp">Generated: $timestamp | Mode: Serial ($Rounds rounds average) | Requests: $Requests | Concurrency: $Concurrency</p>
    
    <div class="note">
        <strong>Serial Testing:</strong> Each server was tested independently without other JVMs running ($Rounds rounds averaged). 
        This provides fair comparison by eliminating CPU/memory competition between processes.
        <br><strong>JVM Flags:</strong> -Xms512m -Xmx512m -XX:+UseZGC -XX:+ZGenerational
        <br><strong>Baseline:</strong> All percentages are relative to Gin (100% = Gin performance).
    </div>
"@

# Summary metrics
$jsonServers = $results["/json"].GetEnumerator() | Sort-Object { $_.Value } -Descending
$bestJsonServer = $jsonServers | Select-Object -First 1
$bestJsonPct = [math]::Round($bestJsonServer.Value / $ginQps["/json"] * 100)

# Find best LiteJava variant
$liteJavaServers = @("JdkVT", "JettyVT", "Netty")
$bestLiteJava = $liteJavaServers | ForEach-Object { @{ Name = $_; QPS = $results["/json"][$_] } } | Sort-Object { $_.QPS } -Descending | Select-Object -First 1
$bestLiteJavaPct = [math]::Round($bestLiteJava.QPS / $ginQps["/json"] * 100)

$html += @"
    <div class="summary">
        <div class="metric">
            <div class="metric-value">$bestJsonPct%</div>
            <div class="metric-label">Best vs Gin ($($bestJsonServer.Key) /json)</div>
        </div>
        <div class="metric">
            <div class="metric-value">$bestLiteJavaPct%</div>
            <div class="metric-label">LiteJava $($bestLiteJava.Name) vs Gin</div>
        </div>
        <div class="metric">
            <div class="metric-value">$($ginQps["/json"])</div>
            <div class="metric-label">Gin /json QPS (Baseline)</div>
        </div>
    </div>
"@

# Generate bar charts for each endpoint
foreach ($ep in $endpoints) {
    $path = $ep.Path
    $epName = $ep.Name
    $baseQps = $ginQps[$path]
    if ($baseQps -eq 0) { $baseQps = 1 }
    
    $sortedServers = $results[$path].GetEnumerator() | Sort-Object { $_.Value } -Descending
    
    $html += @"
    <div class="section">
        <h2>$epName Performance (vs Gin = 100%)</h2>
        <div class="chart-container">
"@
    
    foreach ($srv in $sortedServers) {
        $name = $srv.Key
        $qps = $srv.Value
        $pct = [math]::Round($qps / $baseQps * 100)
        $barWidth = [math]::Min($pct / 2, 100)
        $color = Get-BarColor $name $pct
        $lat = $latencyResults[$path][$name]
        
        $html += @"
            <div class="chart-row">
                <div class="chart-label">$name</div>
                <div class="chart-bar-wrapper">
                    <div class="chart-bar" style="width: $barWidth%; background: $color;">$pct%</div>
                </div>
                <div class="chart-value">$($qps.ToString("N0")) QPS | $($lat.Avg)ms$(if ($lat.Success -lt 100) { " | $($lat.Success)% ok" } else { "" })</div>
            </div>
"@
    }
    
    $html += @"
        </div>
    </div>
"@
}

# Summary table
$html += @"
    <div class="section">
        <h2>Summary - All Endpoints vs Gin (100%)</h2>
        <table>
            <tr>
                <th>Framework</th>
                <th>JSON</th>
                <th>Plain Text</th>
                <th>Dynamic</th>
                <th>DB Users</th>
                <th>DB Posts</th>
            </tr>
"@

$allServers = $results["/json"].Keys | Sort-Object { $results["/json"][$_] } -Descending
foreach ($srv in $allServers) {
    $jsonPct = [math]::Round($results["/json"][$srv] / $ginQps["/json"] * 100)
    $textPct = [math]::Round($results["/text"][$srv] / $ginQps["/text"] * 100)
    $dynamicPct = if ($ginQps["/dynamic"] -gt 0) { [math]::Round($results["/dynamic"][$srv] / $ginQps["/dynamic"] * 100) } else { 0 }
    $usersPct = [math]::Round($results["/users?page=1&size=10"][$srv] / $ginQps["/users?page=1&size=10"] * 100)
    $postsPct = [math]::Round($results["/posts?page=1&size=10"][$srv] / $ginQps["/posts?page=1&size=10"] * 100)
    
    $jsonClass = if ($jsonPct -ge 100) { "good" } elseif ($jsonPct -lt 50) { "bad" } else { "" }
    $textClass = if ($textPct -ge 100) { "good" } elseif ($textPct -lt 50) { "bad" } else { "" }
    $dynamicClass = if ($dynamicPct -ge 100) { "good" } elseif ($dynamicPct -lt 50) { "bad" } else { "" }
    $usersClass = if ($usersPct -ge 100) { "good" } elseif ($usersPct -lt 50) { "bad" } else { "" }
    $postsClass = if ($postsPct -ge 100) { "good" } elseif ($postsPct -lt 50) { "bad" } else { "" }
    
    $rowStyle = if ($srv -eq "Gin") { " style=`"background: #fff3e0;`"" } else { "" }
    
    $html += "            <tr$rowStyle><td>$srv</td><td class=`"$jsonClass`">$jsonPct%</td><td class=`"$textClass`">$textPct%</td><td class=`"$dynamicClass`">$dynamicPct%</td><td class=`"$usersClass`">$usersPct%</td><td class=`"$postsClass`">$postsPct%</td></tr>`n"
}

$html += @"
        </table>
    </div>

    <div class="section">
        <h2>Legend</h2>
        <div style="display: flex; flex-wrap: wrap; gap: 20px;">
            <div style="display: flex; align-items: center;"><div style="width: 20px; height: 20px; background: linear-gradient(90deg, #ff9800, #ffb74d); border-radius: 4px; margin-right: 8px;"></div> Gin (Baseline)</div>
            <div style="display: flex; align-items: center;"><div style="width: 20px; height: 20px; background: linear-gradient(90deg, #2196F3, #64B5F6); border-radius: 4px; margin-right: 8px;"></div> LiteJava Variants</div>
            <div style="display: flex; align-items: center;"><div style="width: 20px; height: 20px; background: linear-gradient(90deg, #4CAF50, #8BC34A); border-radius: 4px; margin-right: 8px;"></div> Better than Gin</div>
            <div style="display: flex; align-items: center;"><div style="width: 20px; height: 20px; background: linear-gradient(90deg, #9C27B0, #BA68C8); border-radius: 4px; margin-right: 8px;"></div> SpringBoot</div>
            <div style="display: flex; align-items: center;"><div style="width: 20px; height: 20px; background: linear-gradient(90deg, #f44336, #e57373); border-radius: 4px; margin-right: 8px;"></div> Needs Optimization</div>
        </div>
    </div>
</div>
</body>
</html>
"@

$html | Out-File -FilePath "$scriptDir\serial-benchmark-results.html" -Encoding UTF8
Write-Host "Report saved to: serial-benchmark-results.html" -ForegroundColor Green

Write-Title "Done!"
