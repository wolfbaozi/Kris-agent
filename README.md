# Kris Agent

Vue3 + TypeScript + Vite 前端，Java Spring Boot 后端，支持 MCP 工具扩展、自定义 Skill 调试与文件管理。

## 快速启动

### 方式一：Docker 一键部署（推荐）

```bash
# 1. 配置环境变量
cp .env.example .env
# 编辑 .env，填入数据库连接信息和密钥

# 2. 构建并启动
docker compose up -d --build
```

服务启动后访问 `http://localhost`。

### 方式二：本地开发

#### 1. 初始化数据库

```bash
# 执行 deploy/init.sql 创建数据库和表结构
mysql -u root -p < deploy/init.sql
```

#### 2. 启动后端

```bash
cd kris-agent-server
# 使用 IDE 或命令行运行 Spring Boot 应用
mvn spring-boot:run
```

后端地址：`http://localhost:3001`

#### 3. 启动前端

```bash
cd web
npm install
npm run dev
```

前端地址：`http://localhost:5173`

浏览器打开前端地址，注册账号后在页面中配置 API Key 即可开始对话。

## 项目结构

```
Kris-agent/
├── web/                      # Vue3 + TypeScript 前端 (Vite)
│   ├── src/
│   │   ├── api/              # API 层：请求封装
│   │   ├── stores/           # Pinia 状态管理
│   │   ├── components/       # 视图组件
│   │   │   ├── chat/         #   聊天组件
│   │   │   │   ├── ChatWindow.vue
│   │   │   │   ├── ChatInput.vue
│   │   │   │   └── ChatMessage.vue   支持 Markdown 渲染与代码高亮
│   │   │   ├── KeysModal.vue
│   │   │   ├── McpModal.vue
│   │   │   ├── SkillModal.vue
│   │   │   └── ...
│   │   ├── pages/            # 页面
│   │   ├── router/           # 路由配置
│   │   └── types/            # TypeScript 类型定义
│   └── package.json
├── kris-agent-server/        # Java Spring Boot 后端 (JDK 8)
│   ├── src/main/java/com/kris/agent/
│   │   ├── config/           # 配置类 (Security, JWT, WebMvc)
│   │   ├── controller/       # API 层
│   │   ├── service/          # 业务逻辑层
│   │   ├── mapper/           # MyBatis-Plus 数据访问层
│   │   ├── entity/           # 实体类
│   │   ├── dto/              # 数据传输对象
│   │   └── security/         # 安全认证
│   ├── src/main/resources/
│   │   ├── application.yml   # 本地开发配置
│   │   ├── application-docker.yml # 生产环境配置
│   │   └── db/migration/     # Flyway 数据库迁移脚本
│   ├── Dockerfile
│   └── pom.xml
├── deploy/
│   └── init.sql              # 数据库初始化 SQL
├── docker-compose.yml        # Docker 服务编排
└── nginx.conf                # Nginx 反向代理配置
```

## 核心功能

### 流式对话

支持 DeepSeek、OpenAI、豆包、火山引擎多种模型，SSE 流式输出，打字机效果。
- **Markdown 渲染**：AI 返回的内容支持 Markdown 格式，代码块自动高亮。
- **MCP/Skill 扩展**：对话中可选择多个 MCP 和 Skill 扩展 AI 能力。

### 文件管理

支持文件上传、下载、列表查询与删除。
- 支持多种文件类型（.js, .json, .md, .txt, .yml 等）
- 文件按用户隔离存储
- 提供 RESTful API 接口

### MCP 管理

- 添加/编辑/删除 MCP 配置（Stdio 进程通信）
- 支持按环境启用（本地/生产/所有环境）
- 全局 MCP（由管理员 "Kris" 用户创建）
- 一键启用/禁用

### Skill 管理

- 函数式 Tool：定义参数 Schema 和执行代码，由 AI 自动调用
- Prompt 指令：注入到 System Prompt 中，引导 AI 行为
- 全局 Skill 同样由管理员管理

### Skill / MCP 一键调试

在对话输入框底部提供"新 Skill 调试"和"新 MCP 调试"快捷入口：
1. 在弹出的表单中填写配置信息和测试消息
2. 点击"创建并调试"，系统自动创建记录并发送调试请求
3. 调试结果（工具调用过程 / AI 回复）以对话消息形式直接展示在聊天窗口中

## API 接口一览

| 方法   | 路径                      | 说明                  |
|--------|---------------------------|----------------------|
| POST   | `/api/auth/register`      | 用户注册              |
| POST   | `/api/auth/login`         | 用户登录              |
| GET    | `/api/auth/me`            | 获取当前用户信息      |
| GET    | `/api/apikeys`            | API Key 列表          |
| POST   | `/api/apikeys`            | 添加 API Key          |
| PUT    | `/api/apikeys/:id`        | 更新 API Key          |
| DELETE | `/api/apikeys/:id`        | 删除 API Key          |
| POST   | `/api/files`              | 上传文件              |
| GET    | `/api/files`              | 文件列表              |
| GET    | `/api/files/:id/download` | 下载文件              |
| DELETE | `/api/files/:id`          | 删除文件              |
| POST   | `/api/chat`               | 流式聊天（SSE）       |
| GET    | `/api/mcps`               | MCP 配置列表          |
| POST   | `/api/mcps`               | 添加 MCP 配置         |
| PUT    | `/api/mcps/:id`           | 更新 MCP 配置         |
| DELETE | `/api/mcps/:id`           | 删除 MCP 配置         |
| PATCH  | `/api/mcps/:id/toggle`    | 启/禁用 MCP           |
| GET    | `/api/skills`             | Skill 配置列表        |
| POST   | `/api/skills`             | 添加 Skill 配置       |
| PUT    | `/api/skills/:id`         | 更新 Skill 配置       |
| DELETE | `/api/skills/:id`         | 删除 Skill 配置       |
| PATCH  | `/api/skills/:id/toggle`  | 启/禁用 Skill         |
| POST   | `/api/debug/skill`        | 创建 Skill 并调试     |
| POST   | `/api/debug/mcp`          | 创建 MCP 并调试       |

## 数据库表

### users
| 字段           | 类型          | 说明       |
|----------------|--------------|-----------|
| id             | INT AUTO PK  | 主键       |
| username       | VARCHAR(50)  | 用户名(唯一) |
| password_hash  | VARCHAR(255) | BCrypt 哈希 |
| role           | ENUM         | 角色       |
| created_at     | TIMESTAMP    | 创建时间    |

### api_keys
| 字段           | 类型          | 说明              |
|----------------|--------------|-------------------|
| id             | INT AUTO PK  | 主键               |
| user_id        | INT FK       | 所属用户            |
| provider       | VARCHAR(20)  | 提供商              |
| encrypted_key  | VARCHAR(512) | AES 加密            |
| model          | VARCHAR(100) | 模型名称            |
| base_url       | VARCHAR(255) | 自定义 API 地址     |
| created_at     | TIMESTAMP    | 创建时间             |

### file_records
| 字段           | 类型          | 说明              |
|----------------|--------------|-------------------|
| id             | INT AUTO PK  | 主键               |
| user_id        | INT FK       | 所属用户            |
| original_name  | VARCHAR(255) | 原始文件名          |
| stored_name    | VARCHAR(255) | 存储文件名          |
| file_path      | VARCHAR(500) | 访问路径            |
| file_size      | BIGINT       | 文件大小            |
| file_type      | VARCHAR(100) | MIME 类型           |
| created_at     | TIMESTAMP    | 创建时间             |

### mcp_servers
| 字段           | 类型                          | 说明       |
|----------------|------------------------------|-----------|
| id             | INT AUTO PK                  | 主键       |
| user_id        | INT FK                       | 所属用户    |
| name           | VARCHAR(100)                 | MCP 名称   |
| is_global      | TINYINT(1)                   | 是否全局    |
| run_env        | ENUM(local,production,all)   | 运行环境    |
| source_type    | ENUM(database,file)          | 来源类型    |
| command        | VARCHAR(255)                 | 启动命令    |
| args           | JSON                         | 命令参数    |
| env            | JSON                         | 环境变量    |
| file_path      | VARCHAR(500)                 | 文件路径    |
| enabled        | TINYINT(1)                   | 是否启用    |
| config         | JSON                         | 额外配置    |
| created_at     | TIMESTAMP                    | 创建时间    |

### skills
| 字段            | 类型                    | 说明          |
|----------------|------------------------|--------------|
| id             | INT AUTO PK            | 主键           |
| user_id        | INT FK                 | 所属用户        |
| name           | VARCHAR(100)           | Skill 名称     |
| is_global      | TINYINT(1)             | 是否全局        |
| skill_type     | ENUM(tool,prompt)      | 类型           |
| source_type    | ENUM(database,file)    | 来源类型        |
| tool_schema    | JSON                   | Tool 参数定义   |
| tool_code      | TEXT                   | Tool 执行代码   |
| prompt_content | TEXT                   | Prompt 内容    |
| file_path      | VARCHAR(500)           | 文件路径        |
| enabled        | TINYINT(1)             | 是否启用        |
| created_at     | TIMESTAMP              | 创建时间        |

## 安全配置

- **JWT 时效**：默认 6 小时，超时需重新登录
- **密码加密**：使用 Spring Security BCrypt
- **API Key 存储**：AES-256 加密存储
- **CORS**：生产环境通过 Nginx 统一代理

## 生产环境部署

1. 在服务器创建 `.env` 文件，配置以下变量：
   ```env
   DB_PASSWORD=your_db_password
   DB_NAME=kris_agent
   JWT_SECRET=your_jwt_secret_key_at_least_32_chars
   ENCRYPTION_KEY=your_32_char_encryption_key
   ```

2. 执行部署命令：
   ```bash
   docker compose up -d --build
   ```

3. 查看日志确认启动状态：
   ```bash
   docker logs -f kris-server
   ```
