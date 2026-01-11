# Nginx 部署指南

本文档描述如何使用 Nginx 作为反向代理，统一 API 入口，隔离后端服务 IP/端口。

## 架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                            客户端                                    │
│                                                                      │
│   所有请求统一发送到 Nginx (80/443)                                  │
│   - HTTP API: /api/account/*, /api/hall/*                           │
│   - WebSocket: ws://domain/ws/game/{gameType} 或直连 GameServer     │
└───────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌───────────────────────────────────────────────────────────────────────┐
│                              Nginx                                    │
│                           (80/443)                                    │
│                                                                       │
│   /api/account/*  ──────────────────►  Account Server (8101)         │
│   /api/hall/*     ──────────────────►  Hall Server (8201)            │
│   /ws/game/*      ──────────────────►  Game Servers (9100+)          │
│   /*              ──────────────────►  静态文件 (game-client)         │
└───────────────────────────────────────────────────────────────────────┘
```

## 统一 API 地址

| 功能 | 开发环境 | 生产环境 (Nginx) |
|------|----------|------------------|
| 登录 | `http://localhost:8101/auth/login` | `/api/account/auth/login` |
| 注册 | `http://localhost:8101/auth/register` | `/api/account/auth/register` |
| 玩家信息 | `http://localhost:8101/player/{id}` | `/api/account/player/{id}` |
| 创建房间 | `http://localhost:8201/create_private_room` | `/api/hall/create_private_room` |
| 加入房间 | `http://localhost:8201/enter_private_room` | `/api/hall/enter_private_room` |
| 匹配 | `http://localhost:8201/match/start` | `/api/hall/match/start` |
| WebSocket | `ws://ip:port/{gameType}` | 直连或 `/ws/game/{gameType}` |

## 安装配置

### 1. 安装 Nginx

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install nginx

# CentOS/RHEL
sudo yum install nginx

# macOS
brew install nginx
```

### 2. 复制配置文件

```bash
# 复制配置
sudo cp deploy/nginx.conf /etc/nginx/conf.d/room-game.conf

# 测试配置
sudo nginx -t

# 重载配置
sudo nginx -s reload
```

### 3. 部署前端

```bash
# 构建前端
cd game-client
npm run build

# 复制到 Nginx 目录
sudo cp -r dist/* /var/www/game-client/
```

## 配置说明

### nginx.conf 核心配置

```nginx
# Upstream 定义
upstream account_server {
    server 127.0.0.1:8101;
    keepalive 32;
}

upstream hall_server {
    server 127.0.0.1:8201;
    keepalive 32;
}

server {
    listen 80;
    
    # 前端静态文件
    location / {
        root /var/www/game-client;
        try_files $uri $uri/ /index.html;
    }
    
    # Account API
    location /api/account/ {
        proxy_pass http://account_server/;
        proxy_set_header X-Real-IP $remote_addr;
    }
    
    # Hall API
    location /api/hall/ {
        proxy_pass http://hall_server/;
        proxy_set_header X-Real-IP $remote_addr;
    }
    
    # WebSocket (可选)
    location ~ ^/ws/game/(.+)$ {
        proxy_pass http://game_servers/$1;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

## WebSocket 连接策略

### 方案 A: 客户端直连 GameServer (推荐)

HallServer 返回 GameServer 的真实地址，客户端直连：

```javascript
// HallServer 返回
{ "ip": "game1.example.com", "port": 9100, "token": "xxx" }

// 客户端直连
ws = new WebSocket("ws://game1.example.com:9100/doudizhu")
```

**优点**: 性能最佳，无额外代理开销
**缺点**: 暴露 GameServer 地址

### 方案 B: 通过 Nginx 代理 WebSocket

所有 WebSocket 都通过 Nginx：

```javascript
// HallServer 返回 (不含真实 IP)
{ "gameType": "doudizhu", "roomId": "123456", "token": "xxx" }

// 客户端通过 Nginx
ws = new WebSocket("ws://example.com/ws/game/doudizhu?room=123456")
```

**优点**: 隐藏 GameServer 地址
**缺点**: 增加代理开销，需要额外路由逻辑

### 方案 C: 混合模式

- 开发环境: 直连
- 生产环境: 通过 Nginx 或 CDN

## 客户端配置

更新 `game-client/src/utils/websocket.js`:

```javascript
// API 配置 - 根据环境自动切换
const isProd = import.meta.env.PROD

const API_CONFIG = {
  // 开发环境: 直连各服务
  // 生产环境: 统一通过 Nginx
  accountServer: isProd ? '/api/account' : `http://${location.hostname}:8101`,
  hallServer: isProd ? '/api/hall' : `http://${location.hostname}:8201`,
  
  // WebSocket 基础地址 (生产环境可选通过 Nginx)
  wsBase: isProd ? `wss://${location.host}/ws/game` : null
}
```

## HTTPS 配置

生产环境建议启用 HTTPS:

```nginx
server {
    listen 443 ssl http2;
    server_name your-domain.com;
    
    ssl_certificate /etc/nginx/ssl/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    
    # ... 其他配置同 HTTP ...
}

# HTTP 重定向到 HTTPS
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$server_name$request_uri;
}
```

## 健康检查

```bash
# 检查 Nginx
curl http://localhost/health

# 检查 Account Server
curl http://localhost/api/account/health

# 检查 Hall Server
curl http://localhost/api/hall/health
```

## 常见问题

### 1. WebSocket 连接失败

检查 Nginx 配置:
```nginx
proxy_http_version 1.1;
proxy_set_header Upgrade $http_upgrade;
proxy_set_header Connection "upgrade";
```

### 2. 超时问题

增加超时时间:
```nginx
proxy_read_timeout 3600s;
proxy_send_timeout 3600s;
```

### 3. 跨域问题

添加 CORS 头:
```nginx
add_header Access-Control-Allow-Origin *;
add_header Access-Control-Allow-Methods "GET, POST, OPTIONS";
add_header Access-Control-Allow-Headers "Content-Type, Authorization";
```
