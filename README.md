# AI Agent Demo

Vue3 + TypeScript + Vite 前端，Node.js BFF 代理大模型 API（Key 不暴露给浏览器）。

## 快速启动

### 1. 配置 API Key

```bash
cd server
cp .env.example .env
# 编辑 .env，填入 DEEPSEEK_API_KEY 或 OPENAI_API_KEY
```

DeepSeek 注册：https://platform.deepseek.com/

### 2. 安装依赖

```bash
# 根目录（可选，用于一条命令同时启动前后端）
npm install

cd web && npm install
cd ../server && npm install
```

### 3. 启动

**方式 A：根目录一条命令**

```bash
npm run dev
```

**方式 B：两个终端分别启动**

```bash
# 终端 1
cd server && npm run dev

# 终端 2
cd web && npm run dev
```

浏览器打开 http://localhost:5173

## 项目结构

```
ai-agent-demo/
├── web/                 # Vue3 + TS 前端
│   ├── src/
│   │   ├── api/         # 请求封装（流式 fetch）
│   │   ├── stores/      # Pinia 会话状态
│   │   ├── components/  # Chat UI
│   │   └── types/
│   └── vite.config.ts   # /api 代理到 3001
└── server/              # Node BFF
    ├── index.js           # POST /api/chat 流式接口
    └── .env               # API Key（勿提交）
```

## 本周学习计划（工作日全程开发）

| 天 | 目标 | 验收 |
|----|------|------|
| **周一** | 跑通流式 Chat（当前） | 能对话、打字机效果 |
| **周二** | Markdown 渲染 + 代码高亮 | AI 返回代码块可读 |
| **周三** | Tool Calling：`getWeather` | 前端展示工具调用过程 |
| **周四** | 第二个 Tool + Human 确认 | 敏感操作需用户点确认 |
| **周五** | 会话持久化 + 错误重试 + README | 刷新可恢复 / 体验完整 |

## 环境变量

| 变量 | 说明 |
|------|------|
| `AI_PROVIDER` | `deepseek`（默认）或 `openai` |
| `DEEPSEEK_API_KEY` | DeepSeek API Key |
| `OPENAI_API_KEY` | OpenAI API Key |
| `PORT` | BFF 端口，默认 3001 |

## 打卡模板

```text
【Agent 打卡】Day X
完成：
问题：
明天：
```
