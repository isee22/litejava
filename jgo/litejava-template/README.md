# LiteJava Template

LiteJava 项目模板 - MyBatis + Thymeleaf

## 技术栈

- **框架**: LiteJava
- **数据库**: MyBatis + MySQL + HikariCP
- **模板**: Thymeleaf
- **JDK**: 1.8+

## 快速开始

### 1. 创建数据库

```bash
mysql -u root -p < src/main/resources/schema.sql
```

### 2. 修改配置

编辑 `src/main/resources/application.yml`：

```yaml
db:
  url: jdbc:mysql://localhost:3306/litejava
  username: root
  password: 123456
```

### 3. 编译运行

```bash
# 编译
mvn clean package

# 运行
java -jar target/litejava-template-1.0.0-SNAPSHOT.jar
```

### 4. 访问

- 首页: http://localhost:8080
- 用户管理: http://localhost:8080/users
- API: http://localhost:8080/api/users

## 项目结构

```
src/main/java/com/example/
├── App.java                 # 应用入口
├── controller/              # 控制器
│   ├── HomeController.java
│   └── UserController.java
├── service/                 # 服务层
│   └── UserService.java
├── mapper/                  # MyBatis Mapper
│   └── UserMapper.java
└── model/                   # 实体类
    └── User.java

src/main/resources/
├── application.yml          # 配置文件
├── schema.sql               # 数据库脚本
├── templates/               # Thymeleaf 模板
│   ├── index.html
│   ├── about.html
│   ├── users/
│   │   ├── list.html
│   │   ├── form.html
│   │   └── detail.html
│   └── error/
│       └── 404.html
└── static/                  # 静态资源
    └── css/
        └── style.css
```

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/users | 获取用户列表 |
| GET | /api/users/:id | 获取单个用户 |
| POST | /api/users | 创建用户 |
| PUT | /api/users/:id | 更新用户 |
| DELETE | /api/users/:id | 删除用户 |

## 开发指南

### 添加新实体

1. 创建 Model: `model/Book.java`
2. 创建 Mapper: `mapper/BookMapper.java`
3. 创建 Service: `service/BookService.java`
4. 创建 Controller: `controller/BookController.java`
5. 在 `App.java` 中注册路由

### 添加新页面

1. 在 `templates/` 下创建 HTML 文件
2. 在 Controller 中添加路由和渲染逻辑

## License

MIT
