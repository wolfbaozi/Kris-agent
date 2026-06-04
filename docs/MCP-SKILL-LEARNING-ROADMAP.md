# MCP & Skill 学习路线与开发规划

## 一、核心概念理解

### 1.1 MCP 协议（Model Context Protocol）

MCP 是 Anthropic 提出的开放标准协议，用于统一 AI 模型与外部工具/数据源的通信方式。

**三大核心概念：**

| 概念 | 说明 | 类比 |
|------|------|------|
| **Resource** | 向 AI 提供只读数据（文件、数据库记录等） | AI 的"眼睛" |
| **Tool** | AI 可主动调用的函数（执行动作、获取数据） | AI 的"双手" |
| **Prompt** | 预定义的提示词模板，由用户触发 | AI 的"剧本" |

**运行模式：**

```
STDIO 模式（优先实现）
┌──────────────┐   stdin/stdout   ┌──────────────────┐
│  AI 后端服务  │ <──────────────> │  MCP Server 子进程 │
└──────────────┘                  └──────────────────┘

SSE/HTTP 模式（后续扩展）
┌──────────────┐   HTTP + SSE     ┌──────────────────┐
│  AI 后端服务  │ <──────────────> │  MCP Server HTTP  │
└──────────────┘                  └──────────────────┘
```

**参考资料：**
- 官方文档：https://modelcontextprotocol.io
- 官方 SDK：https://github.com/modelcontextprotocol/typescript-sdk

---

### 1.2 Vercel AI SDK

项目中已使用 `ai` + `@ai-sdk/openai`，重点掌握以下内容：

**Tool 定义（函数式 Skill 的基础）：**

```typescript
import { tool } from 'ai'
import { z } from 'zod'

const weatherTool = tool({
  description: '获取指定城市的天气信息',
  parameters: z.object({
    city: z.string().describe('城市名称'),
  }),
  execute: async ({ city }) => {
    // 执行逻辑
    return { temperature: 25, weather: '晴天', city }
  },
})
```

**在 streamText 中挂载 Tool：**

```typescript
import { streamText } from 'ai'

const result = await streamText({
  model: openai('gpt-4o'),
  messages,
  tools: {
    weather: weatherTool,
    // 更多 tool...
  },
  maxSteps: 5, // 允许多轮 tool 调用
})
```

**参考资料：**
- 官方文档：https://sdk.vercel.ai
- Tool 使用指南：https://sdk.vercel.ai/docs/ai-sdk-core/tools-and-tool-calling

---

### 1.3 MCP + AI SDK 桥接方案

将 MCP Server 的 Tool 转换为 AI SDK 可用的 Tool，核心依赖：

```bash
npm install @modelcontextprotocol/sdk
```

**STDIO 桥接流程：**

```
用户配置 MCP Server (命令/参数)
    ↓
后端 spawn() 启动子进程
    ↓
通过 StdioClientTransport 建立通信
    ↓
调用 listTools() 获取 MCP 提供的所有 Tool 列表
    ↓
将 MCP Tool 转换为 AI SDK tool() 格式
    ↓
注入到 streamText({ tools: {...} })
```

---

## 二、Skill 方案说明

Skill 分为两种类型，在本项目中都需要支持：

### 2.1 函数式 Tool Skill

Skill 本质是一段可执行函数，由 AI 根据用户意图自主决定是否调用。

**数据结构设计：**
```json
{
  "name": "calculate",
  "description": "执行数学计算，支持加减乘除",
  "parameters": {
    "type": "object",
    "properties": {
      "expression": {
        "type": "string",
        "description": "数学表达式，如 2+3*4"
      }
    },
    "required": ["expression"]
  },
  "code": "const result = eval(params.expression); return { result };"
}
```

### 2.2 Prompt 指令 Skill

Skill 是一段系统提示词，注入到 system message 中，改变 AI 的行为风格或约束。

**示例：**
```text
你是一名专业的代码审查员。
当用户提交代码时，你需要：
1. 检查代码规范和可读性
2. 识别潜在的 Bug 和安全漏洞
3. 给出具体的改进建议
回复使用中文，格式清晰。
```

---

## 三、数据库设计

### 3.1 MCP Server 配置表

```sql
CREATE TABLE mcp_servers (
  id           INT          AUTO_INCREMENT PRIMARY KEY,
  user_id      INT          NOT NULL,
  name         VARCHAR(100) NOT NULL,
  source_type  ENUM('database','file') NOT NULL DEFAULT 'database',
  server_type  VARCHAR(50)  DEFAULT '',
  command      VARCHAR(255) DEFAULT '',
  args         JSON         DEFAULT NULL,
  env          JSON         DEFAULT NULL,
  file_path    VARCHAR(500) DEFAULT '',
  enabled      TINYINT(1)   DEFAULT 1,
  config       JSON         DEFAULT NULL,
  created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

### 3.2 Skill 表

```sql
CREATE TABLE skills (
  id             INT          AUTO_INCREMENT PRIMARY KEY,
  user_id        INT          NOT NULL,
  name           VARCHAR(100) NOT NULL,
  skill_type     ENUM('tool','prompt') NOT NULL DEFAULT 'tool',
  source_type    ENUM('database','file') NOT NULL DEFAULT 'database',
  tool_schema    JSON         DEFAULT NULL,
  tool_code      TEXT         DEFAULT NULL,
  prompt_content TEXT         DEFAULT NULL,
  file_path      VARCHAR(500) DEFAULT '',
  enabled        TINYINT(1)   DEFAULT 1,
  created_at     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

---

## 四、后端开发规划

### 4.1 API 层（routes）

| 文件 | 路由前缀 | 功能 |
|------|----------|------|
| `routes/mcps.js` | `/api/mcps` | MCP 配置 CRUD + 文件上传 |
| `routes/skills.js` | `/api/skills` | Skill CRUD + 文件上传 |

**MCP 路由清单：**

```
GET    /api/mcps          - 获取当前用户所有 MCP 配置
POST   /api/mcps          - 新增 MCP 配置（database 模式）
PUT    /api/mcps/:id      - 更新 MCP 配置
DELETE /api/mcps/:id      - 删除 MCP 配置
POST   /api/mcps/upload   - 上传 MCP 配置文件（file 模式）
PATCH  /api/mcps/:id/toggle - 启用/禁用 MCP
```

**Skill 路由清单：**

```
GET    /api/skills          - 获取当前用户所有 Skill
POST   /api/skills          - 新增 Skill（database 模式）
PUT    /api/skills/:id      - 更新 Skill
DELETE /api/skills/:id      - 删除 Skill
POST   /api/skills/upload   - 上传 Skill 文件（file 模式）
PATCH  /api/skills/:id/toggle - 启用/禁用 Skill
```

### 4.2 Services 层

| 文件 | 职责 |
|------|------|
| `services/mcp-loader.js` | 启动 MCP 子进程，获取并转换 Tool 列表 |
| `services/skill-loader.js` | 加载 Skill，区分 tool 类型和 prompt 类型 |
| `services/chat.js`（改造） | 集成 MCP Tool + Skill Tool，注入 streamText |

**`mcp-loader.js` 核心逻辑：**

```javascript
import { Client } from '@modelcontextprotocol/sdk/client/index.js'
import { StdioClientTransport } from '@modelcontextprotocol/sdk/client/stdio.js'
import { tool } from 'ai'
import { z } from 'zod'

export async function loadMcpTools(mcpConfig) {
  const transport = new StdioClientTransport({
    command: mcpConfig.command,
    args: mcpConfig.args,
    env: mcpConfig.env,
  })
  const client = new Client({ name: 'kris-agent', version: '1.0.0' })
  await client.connect(transport)
  const { tools } = await client.listTools()
  // 将 MCP Tool 转换为 AI SDK tool 格式
  return convertMcpTools(tools, client)
}
```

**`skill-loader.js` 核心逻辑：**

```javascript
export function loadSkillTools(skills) {
  const tools = {}
  const promptParts = []

  for (const skill of skills) {
    if (skill.skill_type === 'tool') {
      tools[skill.name] = buildToolFromSkill(skill)
    } else if (skill.skill_type === 'prompt') {
      promptParts.push(skill.prompt_content)
    }
  }

  return { tools, systemPromptAppend: promptParts.join('\n\n') }
}
```

---

## 五、前端开发规划

### 5.1 新增页面/组件

| 组件 | 路径 | 说明 |
|------|------|------|
| `McpModal.vue` | `components/McpModal.vue` | MCP 管理弹窗 |
| `SkillModal.vue` | `components/SkillModal.vue` | Skill 管理弹窗 |
| `McpForm.vue` | `components/McpForm.vue` | MCP 配置表单 |
| `SkillForm.vue` | `components/SkillForm.vue` | Skill 配置表单 |

### 5.2 新增 API 层

| 文件 | 说明 |
|------|------|
| `api/mcp.ts` | MCP CRUD + 文件上传接口封装 |
| `api/skill.ts` | Skill CRUD + 文件上传接口封装 |

### 5.3 新增 Store

| 文件 | 说明 |
|------|------|
| `stores/mcp.ts` | MCP 列表状态管理 |
| `stores/skill.ts` | Skill 列表状态管理 |

### 5.4 聊天界面改造

- 在 `ChatInput.vue` 中增加 MCP / Skill 快速开关
- 请求体中携带 `mcpIds[]` 和 `skillIds[]`，告知后端本次对话启用哪些扩展

---

## 六、开发执行顺序

```
阶段一：数据基础
  Step 1 - 数据库迁移：新增 mcp_servers、skills 两张表
  Step 2 - server/initdb.js 更新初始化脚本

阶段二：后端 CRUD
  Step 3 - routes/mcps.js：MCP 配置 CRUD API
  Step 4 - routes/skills.js：Skill CRUD API
  Step 5 - services/mcp-loader.js：MCP 子进程加载器
  Step 6 - services/skill-loader.js：Skill 加载器

阶段三：对话集成
  Step 7 - 改造 services/chat.js：集成 MCP Tool + Skill
  Step 8 - 改造 routes/chat.js：接收 mcpIds/skillIds 参数

阶段四：前端
  Step 9  - api/mcp.ts + api/skill.ts
  Step 10 - stores/mcp.ts + stores/skill.ts
  Step 11 - McpModal.vue + SkillModal.vue
  Step 12 - ChatInput.vue 增加 MCP/Skill 开关
  Step 13 - ChatPage.vue 接入管理面板入口
```

---

## 七、本地学习练习建议

### 练习一：写一个最简单的 MCP Server

```typescript
// my-mcp-server.ts
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js'
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js'
import { z } from 'zod'

const server = new McpServer({
  name: 'my-first-mcp',
  version: '1.0.0',
})

server.tool('add', { a: z.number(), b: z.number() }, async ({ a, b }) => ({
  content: [{ type: 'text', text: String(a + b) }],
}))

const transport = new StdioServerTransport()
await server.connect(transport)
```

**运行：**
```bash
npx ts-node my-mcp-server.ts
```

### 练习二：用 AI SDK 消费这个 MCP Server

在后端 `services/chat.js` 中接入，验证 AI 能否自动调用 `add` tool 完成计算。

### 练习三：写一个 Prompt Skill

为 AI 创建一个"代码审查"Prompt Skill，存入数据库，验证注入 system message 后 AI 行为是否改变。

---

## 八、关键依赖包

```bash
# 后端新增
npm install @modelcontextprotocol/sdk zod multer

# 前端无需新增（使用已有 fetch + Vue 生态）
```

---

## 九、文件存储规范（file 模式）

上传的 MCP 配置文件和 Skill 文件统一存放在：

```
server/
  uploads/
    mcp/         - MCP 配置 JSON 文件
      {userId}/
        {timestamp}-{filename}.json
    skill/       - Skill 配置 JSON 文件
      {userId}/
        {timestamp}-{filename}.json
```

文件内容规范（MCP 配置 JSON）：
```json
{
  "name": "my-mcp-server",
  "command": "node",
  "args": ["./path/to/server.js"],
  "env": {
    "API_KEY": "xxx"
  }
}
```

文件内容规范（Skill JSON）：
```json
{
  "name": "code-review",
  "skill_type": "prompt",
  "prompt_content": "你是一名专业的代码审查员..."
}
```
