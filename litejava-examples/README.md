# LiteJava 图书管理示例

使用 LiteJava 框架 + MySQL 构建的图书管理系统。

## 功能

- 用户登录/登出/注册
- 图书 CRUD (MySQL 存储)
- 图书封面上传/下载
- 静态文件服务

## 准备

### 1. 创建 MySQL 数据库

```sql
CREATE DATABASE bookdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. 修改配置

编辑 `config.yml` 中的数据库连接信息。

## 运行

```bash
cd litejava-example
mvn exec:java -Dexec.mainClass="example.BookApp"
```

访问 http://localhost:8080

## 默认账号

- admin / admin123
- user / user123

## API

- POST /api/auth/login - 登录
- POST /api/auth/register - 注册  
- GET /api/books - 图书列表
- POST /api/books - 添加图书
- PUT /api/books/:id - 更新图书
- DELETE /api/books/:id - 删除图书
- POST /api/upload/cover - 上传封面
