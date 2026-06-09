# Kris Agent - Java 后端项目描述

> 本文档面向前端开发者，用前端概念类比 Java 后端架构，帮助快速理解项目结构。

## 技术栈

| 技术 | 作用 | 前端类比 |
|------|------|----------|
| Spring Boot 2.7 | Web 框架 | 相当于 Next.js / Nuxt.js（全栈框架） |
| Spring Security | 认证授权 | 相当于前端的路由守卫 + axios 拦截器 |
| MyBatis-Plus 3.5 | ORM（数据库操作） | 相当于 Prisma / Sequelize |
| JWT (jjwt) | Token 认证 | 相当于前端的 jsonwebtoken 库 |
| OpenAI Java SDK | 调用 AI 接口 | 相当于前端的 openai npm 包 |
| Lombok | 减少样板代码 | 自动生成 getter/setter（TypeScript 没有这个烦恼） |
| Flyway | 数据库迁移 | 相当于前端的数据库 schema 版本管理 |

## 项目结构

```
kris-agent-server/src/main/java/com/kris/agent/
├── KrisAgentApplication.java    # 启动入口（相当于 main.ts）
├── config/                      # 配置层（相当于 vite.config.ts + 全局配置）
│   ├── EncryptionConfig.java    #   AES-GCM 加解密工具
│   ├── JwtTokenProvider.java    #   JWT 令牌生成/验证
│   ├── SecurityConfig.java      #   安全配置（白名单、过滤器链）
│   └── WebConfig.java           #   静态资源映射
├── security/                    # 安全层（相当于 axios 请求拦截器）
│   ├── JwtAuthenticationFilter.java  # 从请求头提取并验证 JWT
│   └── UserPrincipal.java       #   当前登录用户对象
├── controller/                  # 控制层（相当于 Express 路由 handler）
│   ├── AuthController.java      #   登录/注册/用户信息
│   ├── ChatController.java      #   AI 对话（SSE 流式）
│   ├── SkillController.java     #   Skill CRUD
│   ├── McpController.java       #   MCP 服务器 CRUD
│   ├── ApiKeyController.java    #   API Key 管理
│   ├── AiGenController.java     #   AI 辅助生成配置
│   ├── DebugController.java     #   Skill/MCP 调试
│   ├── FileController.java      #   文件上传/下载
│   └── HealthController.java    #   健康检查
├── service/                     # 服务层（相当于 composable/hook 的业务逻辑）
│   ├── AuthService.java         #   注册/登录逻辑
│   ├── ChatService.java         #   AI 对话核心（流式调用 OpenAI）
│   ├── SkillService.java        #   Skill CRUD 逻辑
│   ├── McpService.java          #   MCP CRUD 逻辑
│   ├── ApiKeyService.java       #   API Key 管理逻辑
│   ├── AiGenService.java        #   AI 生成配置逻辑
│   ├── DebugService.java        #   调试逻辑
│   └── FileService.java         #   文件管理逻辑
├── dto/                         # 数据传输对象（相当于前端的 Request/Response 类型）
│   ├── LoginRequest.java        #   登录请求体
│   ├── AuthResponse.java        #   登录响应体
│   ├── ChatRequest.java         #   聊天请求体
│   ├── SkillRequest.java        #   Skill 请求体
│   ├── McpRequest.java          #   MCP 请求体
│   ├── ApiKeyRequest.java       #   API Key 请求体
│   ├── AiGenRequest.java        #   AI 生成请求体
│   └── DebugRequest.java        #   调试请求体
├── entity/                      # 实体层（相当于 Prisma Schema / 数据库表映射）
│   ├── User.java                #   users 表
│   ├── ApiKey.java              #   api_keys 表
│   ├── Skill.java               #   skills 表
│   ├── McpServer.java           #   mcp_servers 表
│   └── FileRecord.java          #   file_records 表
└── mapper/                      # 数据访问层（相当于 prisma.user.findMany()）
    ├── UserMapper.java          #   用户 CRUD
    ├── ApiKeyMapper.java        #   API Key CRUD
    ├── SkillMapper.java         #   Skill CRUD
    ├── McpServerMapper.java     #   MCP CRUD
    └── FileRecordMapper.java    #   文件记录 CRUD
```

## 核心概念映射

### 分层架构（请求处理流程）

```
前端请求
  ↓
JwtAuthenticationFilter（从 header 取 token 验证身份）
  ↓
Controller（接收请求参数，类似 router.post('/api/xxx', handler)）
  ↓
Service（业务逻辑，类似 composable 里的函数）
  ↓
Mapper（数据库操作，类似 prisma.xxx.findMany()）
  ↓
返回 JSON 响应
```

### 关键注解说明

| 注解 | 作用 | 前端类比 |
|------|------|----------|
| `@SpringBootApplication` | 标记启动类 | `createApp().mount()` |
| `@RestController` | 标记控制器（返回 JSON） | Express 的 `router` |
| `@Service` | 标记服务层 | composable 函数 |
| `@Component` | 标记通用组件 | 全局单例工具 |
| `@Configuration` | 标记配置类 | `vite.config.ts` |
| `@Bean` | 注册到 Spring 容器 | `app.use()` |
| `@GetMapping/@PostMapping` | 定义路由 | `router.get()` / `router.post()` |
| `@RequestBody` | 解析请求体 JSON | `req.body` |
| `@PathVariable` | 从 URL 取参数 | `req.params.id` |
| `@Data` (Lombok) | 自动生成 getter/setter | TypeScript 不需要（JS 对象天然有） |

### 数据库查询（MyBatis-Plus）

```java
// Java（MyBatis-Plus）
LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(User::getUsername, "admin");
User user = userMapper.selectOne(wrapper);

// 前端类比（Prisma）
const user = await prisma.user.findFirst({
  where: { username: "admin" }
});
```

### SSE 流式响应

```
后端（SseEmitter）                    前端（fetch stream）
emitter.send(event)  ─── SSE ───>    reader.read() 逐块解析
emitter.complete()   ─── 结束 ───>    done 事件
```

## API 接口清单

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | /api/auth/register | 注册 | 否 |
| POST | /api/auth/login | 登录 | 否 |
| GET | /api/auth/me | 获取当前用户 | 是 |
| POST | /api/chat | AI 对话（SSE 流式） | 是 |
| GET | /api/skills | Skill 列表 | 是 |
| POST | /api/skills | 创建 Skill | 是 |
| PUT | /api/skills/:id | 更新 Skill | 是 |
| DELETE | /api/skills/:id | 删除 Skill | 是 |
| PATCH | /api/skills/:id/toggle | 切换启用 | 是 |
| GET | /api/mcps | MCP 列表 | 是 |
| POST | /api/mcps | 创建 MCP | 是 |
| PUT | /api/mcps/:id | 更新 MCP | 是 |
| DELETE | /api/mcps/:id | 删除 MCP | 是 |
| PATCH | /api/mcps/:id/toggle | 切换启用 | 是 |
| GET | /api/apikeys | API Key 列表 | 是 |
| POST | /api/apikeys | 创建 API Key | 是 |
| PUT | /api/apikeys/:id | 更新 API Key | 是 |
| DELETE | /api/apikeys/:id | 删除 API Key | 是 |
| POST | /api/ai-gen/skill | AI 生成 Skill 配置 | 是 |
| POST | /api/ai-gen/mcp | AI 生成 MCP 配置 | 是 |
| POST | /api/debug/skill | 调试 Skill（SSE） | 是 |
| POST | /api/debug/mcp | 调试 MCP（SSE） | 是 |
| POST | /api/files | 上传文件 | 是 |
| GET | /api/files | 文件列表 | 是 |
| GET | /api/files/:id/download | 下载文件 | 是 |
| DELETE | /api/files/:id | 删除文件 | 是 |
| GET | /api/health | 健康检查 | 否 |

## 安全机制

1. **JWT Token 认证**：登录后返回 token，前端存在 localStorage，每次请求带 `Authorization: Bearer xxx`
2. **BCrypt 密码加密**：密码存入数据库前做哈希处理，不可逆
3. **AES-GCM 加密 API Key**：用户的 API Key 加密后存储，使用时解密
4. **数据权限隔离**：每个用户只能操作自己的数据（SQL 条件里带 userId）
5. **敏感字段过滤**：全局 MCP/Skill 不暴露 command/args/env 等敏感字段

## 配置文件

`application.yml`（相当于前端的 `.env`）：

```yaml
server:
  port: 3001                    # 后端端口

spring:
  datasource:
    url: jdbc:mysql://...       # 数据库连接（相当于 DATABASE_URL）

app:
  jwt:
    secret: xxx                 # JWT 签名密钥（相当于 VITE_JWT_SECRET）
    expiration: 21600000        # Token 有效期（6 小时）
  encryption:
    key: 0123456789012345...    # AES 加密密钥（32 字节）
  run-env: local                # 运行环境标识
```
