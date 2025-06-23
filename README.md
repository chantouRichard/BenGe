# 剧本管理系统 - Spring Boot 重构版

## 项目概述

本项目是将原 C# (.NET Core) 的 picture_backend 项目中的 Script 模块重构为 Spring Boot 项目。主要实现了剧本的创建、编辑、分析和可视化功能。

## 技术栈

- **后端框架**: Spring Boot 3.5.3
- **数据库**: MySQL
- **ORM**: MyBatis
- **安全**: Spring Security
- **构建工具**: Maven
- **Java版本**: JDK 17

## 项目结构

```
BenGe/
├── src/main/java/com/bengebackend/
│   ├── controller/          # 控制器层
│   │   └── ScriptController.java
│   ├── service/             # 服务层接口
│   │   ├── ScriptService.java
│   │   ├── ScriptHistoryService.java
│   │   ├── ScriptAnalysisService.java
│   │   └── VisualElementService.java
│   ├── service/serviceImpl/ # 服务层实现
│   │   ├── ScriptServiceImpl.java
│   │   ├── ScriptHistoryServiceImpl.java
│   │   ├── ScriptAnalysisServiceImpl.java
│   │   └── VisualElementServiceImpl.java
│   ├── mapper/              # 数据访问层
│   │   ├── ScriptMapper.java
│   │   ├── ScriptHistoryMapper.java
│   │   ├── ScriptAnalysisMapper.java
│   │   └── VisualElementMapper.java
│   ├── model/               # 实体类
│   │   ├── Script.java
│   │   ├── ScriptHistory.java
│   │   ├── ScriptAnalysis.java
│   │   ├── VisualElement.java
│   │   └── User.java
│   ├── dto/                 # 数据传输对象
│   │   ├── ScriptDetailDto.java
│   │   └── ScriptFrameworkDto.java
│   └── entity/              # 请求实体
│       ├── ScriptReplyRequestEntity.java
│       ├── ScriptUpdateRequestEntity.java
│       └── ScriptVisualRequestEntity.java
├── src/main/resources/
│   ├── application.yaml     # 应用配置
│   └── sql/
│       └── script_tables.sql # 数据库表结构
└── src/test/java/           # 测试类
    └── com/bengebackend/controller/
        └── ScriptControllerTest.java
```

## 核心功能

### 1. 剧本管理
- **创建剧本**: 初始化新的剧本项目
- **查询剧本**: 根据ID获取剧本详情，根据用户ID获取剧本列表
- **更新剧本**: 修改剧本内容和阶段
- **删除剧本**: 软删除剧本

### 2. 剧本生成流程
- **第一阶段**: 创建基础剧本框架
- **第二阶段**: 与AI对话生成详细剧本框架
- **第三阶段**: 生成完整剧本内容

### 3. 剧本分析
- **内容分析**: AI分析剧本内容，提供改进建议
- **历史记录**: 保存用户与AI的对话历史

### 4. 可视化元素
- **元素管理**: 管理剧本中的角色、场景、道具等可视化元素
- **图像生成**: 为选定元素生成对应图像

## API 接口

### 剧本管理接口

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| GET | `/api/script/{id}` | 获取剧本详情 | id: 剧本ID |
| GET | `/api/script/user` | 获取用户剧本列表 | - |
| POST | `/api/script/create` | 创建新剧本 | - |
| PUT | `/api/script/update` | 更新剧本 | ScriptUpdateRequestEntity |
| DELETE | `/api/script/{id}` | 删除剧本 | id: 剧本ID |

### 剧本生成接口

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| POST | `/api/script/reply2nd` | 第二阶段对话 | ScriptReplyRequestEntity |
| PUT | `/api/script/reply2nd/stream` | 流式生成框架 | ScriptReplyRequestEntity |
| POST | `/api/script/reply3rd` | 获取完整剧本 | ScriptReplyRequestEntity |

### 剧本分析接口

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| PUT | `/api/script/analyze` | 分析剧本内容 | ScriptReplyRequestEntity |
| PUT | `/api/script/visualize` | 可视化元素 | ScriptVisualRequestEntity |

## 数据库表结构

### script (剧本表)
- `id`: 主键
- `title`: 剧本标题
- `content`: 剧本内容
- `is_deleted`: 是否删除
- `last_updated`: 最后更新时间
- `user_id`: 用户ID
- `stage`: 当前阶段

### script_history (剧本历史表)
- `id`: 主键
- `script_id`: 剧本ID
- `message`: 用户消息
- `response`: AI响应
- `created_at`: 创建时间

### script_analysis (剧本分析表)
- `id`: 主键
- `script_id`: 剧本ID
- `analysis_result`: 分析结果
- `analyzed_at`: 分析时间

### visual_element (可视化元素表)
- `id`: 主键
- `script_id`: 剧本ID
- `type`: 元素类型 (Character/Scene/Prop)
- `name`: 元素名称
- `description`: 元素描述
- `image_url`: 图片URL
- `image_generated_at`: 图片生成时间

## 配置说明

### application.yaml
```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/picture?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: 123456

mybatis:
  mapper-locations: classpath:mapper/*.xml
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    map-underscore-to-camel-case: true
    type-aliases-package: com.bengebackend.entity
```

## 运行说明

1. **环境准备**
   - 安装 JDK 17
   - 安装 MySQL 数据库
   - 安装 Maven

2. **数据库初始化**
   ```sql
   -- 执行 src/main/resources/sql/script_tables.sql 文件
   ```

3. **启动应用**
   ```bash
   mvn spring-boot:run
   ```

4. **访问接口**
   - 服务地址: http://localhost:8080
   - API文档: http://localhost:8080/api/script/*

## 重构对比

### C# 原项目 vs Spring Boot 重构版

| 特性 | C# 版本 | Spring Boot 版本 |
|------|---------|------------------|
| 框架 | ASP.NET Core | Spring Boot |
| ORM | Entity Framework | MyBatis |
| 依赖注入 | 内置DI | Spring IoC |
| 配置管理 | appsettings.json | application.yaml |
| 数据库 | SQL Server | MySQL |
| 注解支持 | Data Annotations | Lombok + Validation |

### 主要改进

1. **代码结构**: 采用标准的Spring Boot项目结构
2. **数据访问**: 使用MyBatis提供更灵活的SQL控制
3. **异常处理**: 统一的异常处理机制
4. **配置管理**: YAML格式的配置文件
5. **测试支持**: 完整的单元测试和集成测试

## 注意事项

1. **AI功能**: 目前AI相关功能使用模拟数据，实际部署时需要集成真实的AI服务
2. **认证授权**: 当前使用简化的用户认证，生产环境需要完善JWT认证
3. **异常处理**: 需要添加全局异常处理器
4. **日志记录**: 需要配置适当的日志记录策略
5. **性能优化**: 大量数据情况下需要考虑分页和缓存策略

## 后续扩展

1. **集成真实AI服务**: 对接GPT或其他AI模型
2. **添加缓存**: 使用Redis缓存热点数据
3. **消息队列**: 使用RabbitMQ处理异步任务
4. **监控告警**: 集成Prometheus和Grafana
5. **容器化部署**: 使用Docker和Kubernetes部署
