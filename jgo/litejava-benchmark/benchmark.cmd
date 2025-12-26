@echo off
chcp 65001 >nul
title LiteJava Benchmark

echo ========================================
echo   LiteJava Benchmark Runner
echo ========================================
echo.
echo   Frameworks: Gin, JettyVT, JdkVT, Javalin, SpringBoot, Netty
echo.

:: Check hey installed
where hey >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] hey not found! Please install: go install github.com/rakyll/hey@latest
    pause
    exit /b 1
)

:: Menu
:menu
echo.
echo   1. Build all projects
echo   2. Start all servers (parallel)
echo   3. Run benchmark (parallel - all servers running)
echo   4. Stop all servers
echo   5. Full parallel test (build + start + test + stop)
echo   6. Serial benchmark (one server at a time - RECOMMENDED)
echo   7. Exit
echo.
set /p choice=Select option (1-7): 

if "%choice%"=="1" goto build
if "%choice%"=="2" goto start
if "%choice%"=="3" goto benchmark
if "%choice%"=="4" goto stop
if "%choice%"=="5" goto full
if "%choice%"=="6" goto serial
if "%choice%"=="7" exit /b 0
goto menu

:build
echo.
echo [BUILD] Compiling all projects...
cd /d %~dp0..
call mvn clean install -DskipTests -q
if %errorlevel% neq 0 (
    echo [ERROR] Build failed!
    pause
    goto menu
)
cd /d %~dp0
call mvn clean package -DskipTests -q
echo [BUILD] Done!
goto menu

:start
echo.
echo [START] Starting all servers...
call :stop_servers

echo Starting Gin (port 8184)...
start "Gin-8184" /min cmd /c "cd /d %~dp0benchmark-gin && go run main.go"

echo Starting Javalin (port 8182)...
start "Javalin-8182" /min cmd /c "java -jar %~dp0benchmark-javalin\target\benchmark-javalin-1.0.0-SNAPSHOT.jar"

echo Starting SpringBoot (port 8183)...
start "SpringBoot-8183" /min cmd /c "java -jar %~dp0benchmark-springboot\target\benchmark-springboot-1.0.0-SNAPSHOT.jar"

echo Starting JdkVT (port 8185)...
start "JdkVT-8185" /min cmd /c "java -jar %~dp0benchmark-litejava-jdkvt\target\benchmark-litejava-jdkvt-1.0.0-SNAPSHOT.jar"

echo Starting Netty (port 8186)...
start "Netty-8186" /min cmd /c "java -jar %~dp0benchmark-litejava-netty\target\benchmark-litejava-netty-1.0.0-SNAPSHOT.jar"

echo Starting JettyVT (port 8187)...
start "JettyVT-8187" /min cmd /c "java -jar %~dp0benchmark-litejava-jettyvt\target\benchmark-litejava-jettyvt-1.0.0-SNAPSHOT.jar"

echo Waiting for servers (10s for SpringBoot)...
timeout /t 10 /nobreak >nul

echo Checking servers...
call :check_server 8182 Javalin
call :check_server 8183 SpringBoot
call :check_server 8184 Gin
call :check_server 8185 JdkVT
call :check_server 8186 Netty
call :check_server 8187 JettyVT

echo [START] Done!
goto menu

:benchmark
echo.
echo [BENCHMARK] Running tests...
powershell -ExecutionPolicy Bypass -File "%~dp0run-benchmark.ps1"
echo.
echo [BENCHMARK] Results saved to benchmark-results.html
start benchmark-results.html
goto menu

:stop
echo.
echo [STOP] Stopping all servers...
call :stop_servers
echo [STOP] Done!
goto menu

:full
echo.
echo [FULL TEST] Starting complete benchmark...
echo.
echo Step 1/4: Building...
call :build_silent
echo Step 2/4: Stopping old servers...
call :stop_servers
timeout /t 2 /nobreak >nul

echo Step 3/4: Starting servers...
start "Gin-8184" /min cmd /c "cd /d %~dp0benchmark-gin && go run main.go"
start "Javalin-8182" /min cmd /c "java -jar %~dp0benchmark-javalin\target\benchmark-javalin-1.0.0-SNAPSHOT.jar"
start "SpringBoot-8183" /min cmd /c "java -jar %~dp0benchmark-springboot\target\benchmark-springboot-1.0.0-SNAPSHOT.jar"
start "JdkVT-8185" /min cmd /c "java -jar %~dp0benchmark-litejava-jdkvt\target\benchmark-litejava-jdkvt-1.0.0-SNAPSHOT.jar"
start "Netty-8186" /min cmd /c "java -jar %~dp0benchmark-litejava-netty\target\benchmark-litejava-netty-1.0.0-SNAPSHOT.jar"
start "JettyVT-8187" /min cmd /c "java -jar %~dp0benchmark-litejava-jettyvt\target\benchmark-litejava-jettyvt-1.0.0-SNAPSHOT.jar"

echo Waiting for servers (6s)...
timeout /t 6 /nobreak >nul

echo Step 4/4: Running benchmark...
powershell -ExecutionPolicy Bypass -File "%~dp0run-benchmark.ps1"

echo Stopping servers...
call :stop_servers

echo.
echo [FULL TEST] Complete! Opening results...
start benchmark-results.html
goto menu

:serial
echo.
echo [SERIAL BENCHMARK] Running one server at a time...
echo This provides fair comparison without JVM competition.
echo.
call :build_silent
powershell -ExecutionPolicy Bypass -File "%~dp0run-serial-benchmark.ps1"
echo.
echo [SERIAL BENCHMARK] Complete! Opening results...
start serial-benchmark-results.html
goto menu

:: Helper functions
:stop_servers
taskkill /f /fi "WINDOWTITLE eq Gin-8184" >nul 2>&1
taskkill /f /fi "WINDOWTITLE eq Javalin-8182" >nul 2>&1
taskkill /f /fi "WINDOWTITLE eq SpringBoot-8183" >nul 2>&1
taskkill /f /fi "WINDOWTITLE eq JdkVT-8185" >nul 2>&1
taskkill /f /fi "WINDOWTITLE eq Netty-8186" >nul 2>&1
taskkill /f /fi "WINDOWTITLE eq JettyVT-8187" >nul 2>&1
taskkill /f /im java.exe >nul 2>&1
exit /b 0

:check_server
curl -s -o nul -w "" http://localhost:%1/text >nul 2>&1
if %errorlevel% equ 0 (
    echo   %2 (port %1): OK
) else (
    echo   %2 (port %1): FAILED
)
exit /b 0

:build_silent
cd /d %~dp0..
call mvn clean install -DskipTests -q >nul 2>&1
cd /d %~dp0
call mvn clean package -DskipTests -q >nul 2>&1
exit /b 0
