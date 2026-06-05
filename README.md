# Kris Agent

Vue3 + TypeScript + Vite 前端，Node.js Express BFF 代理大模型 API，支持 MCP 工具扩展与自定义 Skill 调试。

## 快速启动

### 1. 初始化数据库

```bash
cd server
node setup.js          # 创建数据库和表结构
node seed-mcp-skill.js  # 可选：导入种子数据（示例 MCP/Skill）
```

### 2. 配置环境变量

```bash
cd server
cp .env.example .env
# 编辑 .env，填入数据库连接信息和 API Key
```

### 3. 安装依赖

```bash
npm install              # 根目录安装 concurrently
cd web && npm install
cd ../server && npm install
```

### 4. 启动

```bash
npm run dev              # 根目录一条命令同时启动前后端
```

或分别启动：

```bash
cd server && npm run dev    # 后端 http://localhost:3001
cd web && npm run dev       # 前端 http://localhost:5173
```

浏览器打开 `http://localhost:5173`，注册账号后在页面中配置 API Key 即可开始对话。

## 项目结构

```
Kris-agent/
├── web/                      # Vue3 + TypeScript 前端 (Vite)
│   ├── src/
│   │   ├── api/              # API 层：请求封装
│   │   │   ├── index.ts      #   基础请求 + authApi + keysApi
│   │   │   ├── chat.ts       #   流式聊天 SSE 接口
│   │   │   ├── debug.ts      #   Skill/MCP 调试接口
│   │   │   ├── skill.ts      #   Skill CRUD 接口
│   │   │   └── mcp.ts        #   MCP CRUD 接口
│   │   ├── stores/           # Pinia 状态管理
│   │   │   ├── auth.ts       #   认证状态
│   │   │   ├── chat.ts       #   聊天状态（核心 store）
│   │   │   ├── skill.ts      #   Skill 列表状态
│   │   │   └── mcp.ts        #   MCP 列表状态
│   │   ├── components/       # 视图组件
│   │   │   ├── chat/         #   聊天组件
│   │   │   │   ├── ChatWindow.vue    消息列表容器
│   │   │   │   ├── ChatInput.vue     输入框 + MCP/Skill 选择 + 调试入口
│   │   │   │   └── ChatMessage.vue   单条消息气泡
│   │   │   ├── KeysModal.vue         API Key 管理弹窗
│   │   │   ├── McpModal.vue          MCP 管理弹窗（CRUD + 启禁用）
│   │   │   ├── SkillModal.vue        Skill 管理弹窗（CRUD + 启禁用）
│   │   │   ├── SkillDebugModal.vue   创建 Skill 并调试弹窗
│   │   │   └── McpDebugModal.vue     创建 MCP 并调试弹窗
│   │   ├── pages/            # 页面
│   │   │   ├── ChatPage.vue         主对话页面
│   │   │   └── LoginPage.vue        登录/注册页
│   │   ├── router/index.ts   # 路由配置（含登录守卫）
│   │   └── types/chat.ts     # 聊天的 TypeScript 类型
│   └── vite.config.ts        # Vite 配置（/api 代理到 3001）
├── server/                   # Node.js Express BFF 后端 (ESM)
│   ├── index.js              #   入口：自动加载 routes/ 目录路由
│   ├── db.js                 #   MySQL2 连接池
│   ├── crypto.js             #   AES-256-GCM 加解密（存储 API Key）
│   ├── middleware/auth.js    #   JWT 认证中间件
│   ├── routes/               # 路由层（仅负责接口引入）
│   │   ├── auth.js           #   /api/auth    注册/登录
│   │   ├── apikeys.js        #   /api/apikeys API Key CRUD
│   │   ├── chat.js           #   /api/chat    流式聊天
│   │   ├── debug.js          #   /api/debug   Skill/MCP 创建并调试
│   │   ├── mcps.js           #   /api/mcps    MCP CRUD + toggle
│   │   └── skills.js         #   /api/skills  Skill CRUD + toggle
│   ├── services/             # 业务服务层（处理接口逻辑）
│   │   ├── chat.js           #   核心聊天：AI SDK 流式 + 工具调用
│   │   ├── mcp-loader.js     #   MCP 工具加载（Stdio 子进程通信）
│   │   └── skill-loader.js   #   Skill 加载（Tool 执行 / Prompt 拼装）
│   └── mcp-examples/         # MCP 示例
│       ├── package.json
│       └── calculator-mcp.js # 计算器 MCP 示例
└── deploy/
    └── init.sql              # 数据库初始化 SQL
```

## 核心功能

### 流式对话

支持 DeepSeek、OpenAI、豆包、火山引擎多种模型，SSE 流式输出，打字机效果。对话中可选择多个 MCP 和 Skill 扩展 AI 能力。

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
| GET    | `/api/apikeys`            | API Key 列表          |
| POST   | `/api/apikeys`            | 添加 API Key          |
| PUT    | `/api/apikeys/:id`        | 更新 API Key          |
| DELETE | `/api/apikeys/:id`        | 删除 API Key          |
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
| username       | VARCHAR(255) | 用户名(唯一) |
| password_hash  | VARCHAR(255) | bcrypt 哈希 |
| created_at     | TIMESTAMP    | 创建时间    |

### api_keys
| 字段           | 类型          | 说明              |
|----------------|--------------|-------------------|
| id             | INT AUTO PK  | 主键               |
| user_id        | INT FK       | 所属用户            |
| provider       | VARCHAR(50)  | 提供商              |
| encrypted_key  | TEXT         | AES-256-GCM 加密   |
| model          | VARCHAR(100) | 模型名称            |
| base_url       | VARCHAR(255) | 自定义 API 地址     |
| created_at     | TIMESTAMP    | 创建时间             |

### mcp_servers
| 字段           | 类型                          | 说明       |
|----------------|------------------------------|-----------|
| id             | INT AUTO PK                  | 主键       |
| user_id        | INT FK                       | 所属用户    |
| name           | VARCHAR(255)                 | MCP 名称   |
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
| name           | VARCHAR(255)           | Skill 名称     |
| is_global      | TINYINT(1)             | 是否全局        |
| skill_type     | ENUM(tool,prompt)      | 类型           |
| source_type    | ENUM(database,file)    | 来源类型        |
| tool_schema    | JSON                   | Tool 参数定义   |
| tool_code      | TEXT                   | Tool 执行代码   |
| prompt_content | TEXT                   | Prompt 内容    |
| file_path      | VARCHAR(500)           | 文件路径        |
| enabled        | TINYINT(1)             | 是否启用        |
| created_at     | TIMESTAMP              | 创建时间        |
