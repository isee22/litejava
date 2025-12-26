# Implementation Plan

- [x] 1. 项目结构与Maven配置





  - [x] 1.1 创建Maven多模块项目结构






    - 创建父pom.xml，定义litejava-core和litejava-plugins两个子模块
    - 配置JDK 1.8编译，JUnit 5和jqwik测试依赖
    - _Requirements: 1.1_
  - [x] 1.2 创建核心模块基础包结构


    - 创建io.litejava包及exception子包
    - 创建基础异常类LiteJavaException
    - _Requirements: 11.1, 11.2_



- [x] 2. JSON处理模块




  - [x] 2.1 实现Json类（序列化/反序列化）


    - 实现stringify()方法，支持Map、List、基本类型序列化


    - 实现parse()方法，返回Map或List


    - 处理嵌套结构和特殊字符转义




    - _Requirements: 5.1, 5.2, 5.4_
  - [x] 2.2 编写JSON往返属性测试


    - **Property 1: JSON Round-Trip Consistency**

    - **Validates: Requirements 4.4, 5.1, 5.2, 5.4**
  - [x] 2.3 实现JSON解析错误处理
    - 无效JSON抛出JsonParseException

    - _Requirements: 5.3_
  - [x] 2.4 编写JSON错误处理属性测试

    - **Property 10: Invalid JSON Throws ParseException**
    - **Validates: Requirements 5.3**


- [x] 3. 统一配置系统
  - [x] 3.1 实现Config类基础功能

    - 实现load()方法，支持YAML/JSON/Properties格式
    - 实现类型安全的get方法（getString, getInt, getBool等）

    - _Requirements: 14.1, 14.3_
  - [x] 3.2 编写配置类型安全属性测试

    - **Property 26: Configuration Value Type Safety**
    - **Validates: Requirements 14.3**
  - [x] 3.3 实现配置合并与环境支持

    - 实现多源配置合并（文件 < 环境变量 < 代码）
    - 实现profile支持（config-dev.yml等）

    - _Requirements: 14.2, 14.5_
  - [x] 3.4 编写配置合并优先级属性测试
    - **Property 27: Configuration Merge Precedence**
    - **Validates: Requirements 14.2**
  - [x] 3.5 实现占位符解析
    - 支持${VAR}和${VAR:default}语法
    - 从环境变量解析占位符
    - _Requirements: 14.7_
  - [x] 3.6 编写占位符解析属性测试
    - **Property 28: Configuration Placeholder Resolution**
    - **Validates: Requirements 14.7**
  - [x] 3.7 实现子配置命名空间
    - 实现sub()方法获取插件专属配置
    - _Requirements: 14.6_
  - [x] 3.8 编写子配置命名空间属性测试
    - **Property 29: Configuration Sub-namespace Isolation**
    - **Validates: Requirements 14.6**
  - [x] 3.9 实现配置验证
    - 实现require()和requireAll()方法
    - 缺失必需配置时抛出ConfigException
    - _Requirements: 14.4, 14.8_
  - [x] 3.10 编写配置验证属性测试
    - **Property 30: Required Configuration Validation**
    - **Validates: Requirements 14.4, 14.8**

- [x] 4. Checkpoint - 确保所有测试通过

  - Ensure all tests pass, ask the user if questions arise.



- [x] 5. Context与请求响应

  - [x] 5.1 实现Context类

    - 定义public字段：method, path, headers, params, queryParams
    - 实现请求体读取方法：bodyString(), bodyBytes(), bodyJson()
    - 实现响应设置方法：status(), header(), body(), json()
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_


  - [x] 5.2 编写Context请求信息属性测试

    - **Property 8: Context Contains Request Information**
    - **Validates: Requirements 4.1**

  - [x] 5.3 编写响应配置属性测试
    - **Property 9: Response Configuration Consistency**
    - **Validates: Requirements 4.3**

  - [x] 5.4 实现请求体解析
    - 根据Content-Type自动解析JSON/Form/Multipart

    - 实现bodyForm()和bodyFiles()方法
    - _Requirements: 13.1, 13.2, 13.3, 13.4_




  - [x] 5.5 编写请求体解析属性测试
    - **Property 23: Body Parsing by Content-Type**


    - **Validates: Requirements 13.1, 13.2, 13.3**

  - [x] 5.6 编写请求体解析错误属性测试
    - **Property 24: Body Parsing Error Handling**
    - **Validates: Requirements 13.4**


- [x] 6. 路由系统

  - [x] 6.1 实现Route类





    - 实现路径模式匹配（支持:param参数）

    - 实现extractParams()方法提取路径参数
    - _Requirements: 3.1, 3.2_

  - [x] 6.2 编写路径参数提取属性测试
    - **Property 5: Path Parameter Extraction**

    - **Validates: Requirements 3.2, 4.5**
  - [x] 6.3 实现Router类


    - 实现get/post/put/delete方法注册路由


    - 实现路由匹配和方法分发
    - _Requirements: 3.1, 3.3_


  - [x] 6.4 编写路由匹配属性测试
    - **Property 4: Route Matching and Method Dispatch**
    - **Validates: Requirements 3.1, 3.3**
  - [-] 6.5 实现路由分组

    - 实现group()方法创建带前缀的子路由
    - _Requirements: 3.5_
  - [x] 6.6 编写路由分组属性测试
    - **Property 6: Route Group Prefix Application**
    - **Validates: Requirements 3.5**
  - [x] 6.7 实现404处理
    - 无匹配路由时返回404响应
    - _Requirements: 3.4_
  - [x] 6.8 编写404属性测试
    - **Property 7: Unmatched Route Returns 404**
    - **Validates: Requirements 3.4**

- [x] 7. Checkpoint - 确保所有测试通过
  - Ensure all tests pass, ask the user if questions arise.

- [x] 8. 中间件系统
  - [x] 8.1 实现Middleware和Next接口
    - 定义函数式接口
    - _Requirements: 2.1_
  - [x] 8.2 实现中间件链执行（洋葱模型）
    - 实现中间件按注册顺序执行
    - 实现next()调用传递控制
    - 实现后处理逻辑（洋葱模型）
    - _Requirements: 2.1, 2.2, 2.3_
  - [x] 8.3 编写中间件执行顺序属性测试

    - **Property 2: Middleware Execution Order (Onion Model)**
    - **Validates: Requirements 2.1, 2.2, 2.3**

  - [x] 8.4 实现中间件链终止
    - 不调用next()时停止后续中间件执行
    - _Requirements: 2.4_

  - [x] 8.5 编写中间件链终止属性测试
    - **Property 3: Middleware Chain Termination**

    - **Validates: Requirements 2.4**
  - [x] 8.6 实现中间件异常传播

    - 异常传播到错误处理中间件
    - _Requirements: 2.5_
  - [x] 8.7 编写中间件异常传播属性测试
    - **Property 25: Middleware Exception Propagation**
    - **Validates: Requirements 2.5**

- [x] 9. App主类与HTTP服务器



  - [x] 9.1 实现App类基础结构





    - 定义port, devMode等public字段
    - 实现use()注册中间件
    - 实现get/post/put/delete注册路由

    - _Requirements: 1.1, 1.2, 1.3_
  - [x] 9.2 实现HTTP服务器
    - 基于com.sun.net.httpserver实现
    - 实现listen()方法启动服务器

    - 实现请求分发到路由和中间件
    - _Requirements: 1.2, 1.3, 1.4, 1.5_
  - [x] 9.3 实现错误处理
    - 开发模式返回详细错误信息
    - 生产模式返回通用错误信息
    - 实现onError()自定义错误处理
    - _Requirements: 11.1, 11.2, 11.3, 11.4_
  - [x] 9.4 编写开发模式错误响应属性测试
    - **Property 17: Error Response in Dev Mode Contains Details**
    - **Validates: Requirements 11.1**
  - [x] 9.5 编写生产模式错误响应属性测试
    - **Property 18: Error Response in Prod Mode Hides Details**
    - **Validates: Requirements 11.2**
  - [x] 9.6 编写自定义错误处理属性测试
    - **Property 19: Custom Error Handler Invocation**
    - **Validates: Requirements 11.3**

- [x] 10. Checkpoint - 确保所有测试通过
  - Ensure all tests pass, ask the user if questions arise.

- [x] 11. 插件系统
  - [x] 11.1 实现Plugin接口

    - 定义init(), destroy(), description()方法
    - _Requirements: 6.1_

  - [x] 11.2 实现PluginManager
    - 实现插件注册和初始化
    - 实现通过Context访问插件
    - _Requirements: 6.1, 6.2, 6.3_
  - [x] 11.3 编写插件注册访问属性测试
    - **Property 11: Plugin Registration and Access**
    - **Validates: Requirements 6.1, 6.2, 6.3**

  - [x] 11.4 实现插件初始化顺序
    - 按注册顺序初始化插件
    - _Requirements: 6.5_
  - [x] 11.5 编写插件初始化顺序属性测试
    - **Property 12: Plugin Initialization Order**

    - **Validates: Requirements 6.5**
  - [x] 11.6 实现插件错误处理
    - 初始化失败抛出PluginException
    - _Requirements: 6.4_

- [x] 12. 静态文件服务
  - [x] 12.1 实现静态文件中间件

    - 实现staticFiles()方法配置静态目录
    - 实现文件读取和响应
    - _Requirements: 12.1_
  - [x] 12.2 编写静态文件服务属性测试
    - **Property 20: Static File Serving**
    - **Validates: Requirements 12.1**

  - [x] 12.3 实现Content-Type映射
    - 根据文件扩展名设置Content-Type
    - _Requirements: 12.2, 12.4_
  - [x] 12.4 编写Content-Type映射属性测试
    - **Property 21: Content-Type Mapping by Extension**

    - **Validates: Requirements 12.2**
  - [x] 12.5 实现静态文件404
    - 文件不存在时返回404
    - _Requirements: 12.3_
  - [x] 12.6 编写静态文件404属性测试
    - **Property 22: Static File 404 for Missing Files**

    - **Validates: Requirements 12.3**

- [x] 13. 测试客户端
  - [x] 13.1 实现TestClient类
    - 实现get/post/put/delete方法
    - 实现request()通用方法

    - _Requirements: 测试支持_
  - [x] 13.2 实现TestResponse类
    - 定义status, headers, body字段
    - 实现json()方法解析响应体
    - _Requirements: 测试支持_

- [x] 14. Checkpoint - 核心模块完成


  - Ensure all tests pass, ask the user if questions arise.


- [x] 15. 官方插件 - 数据库

  - [x] 15.1 实现DatabasePlugin
    - 实现连接池管理
    - 实现query()返回List<Map>
    - 实现execute()返回影响行数
    - _Requirements: 7.1, 7.2, 7.3_

  - [x] 15.2 实现参数化查询
    - 安全绑定参数防止SQL注入
    - _Requirements: 7.4_


  - [x] 15.3 编写SQL参数绑定属性测试

    - **Property 13: SQL Parameter Binding Safety**

    - **Validates: Requirements 7.4**
  - [x] 15.4 实现数据库错误处理
    - 包装为DatabaseException
    - _Requirements: 7.5_

- [x] 16. 官方插件 - 缓存
  - [x] 16.1 实现MemoryCachePlugin
    - 实现内存缓存，支持TTL
    - _Requirements: 8.1_
  - [x] 16.2 实现缓存操作
    - 实现set/get/delete/exists方法
    - _Requirements: 8.2, 8.3, 8.4_

  - [x] 16.3 编写缓存往返属性测试
    - **Property 14: Cache Round-Trip Consistency**

    - **Validates: Requirements 8.2, 8.3**
  - [x] 16.4 编写缓存删除属性测试


    - **Property 15: Cache Deletion Removes Entry**

    - **Validates: Requirements 8.4**
  - [-] 16.5 实现RedisCachePlugin
    - 实现Redis缓存支持
    - _Requirements: 8.1_

  - [x] 16.6 实现缓存错误处理
    - 包装为CacheException
    - _Requirements: 8.5_



- [x] 17. 官方插件 - WebSocket



  - [x] 17.1 实现WebSocketPlugin


    - 实现WebSocket握手处理
    - _Requirements: 9.1_


  - [x] 17.2 实现WebSocket消息处理
    - 实现onOpen/onMessage/onClose/onError回调

    - 实现send()方法发送消息
    - _Requirements: 9.2, 9.3, 9.4, 9.5_
  - [x] 17.3 编写WebSocket消息往返属性测试

    - **Property 16: WebSocket Message Round-Trip**
    - **Validates: Requirements 9.2, 9.3**

- [x] 18. 官方插件 - SSL





  - [x] 18.1 实现SSL配置

    - 实现ssl()方法配置证书
    - 支持HTTP和HTTPS同时运行
    - _Requirements: 10.1, 10.2, 10.3, 10.4_



- [x] 19. Final Checkpoint - 确保所有测试通过








  - Ensure all tests pass, ask the user if questions arise.
