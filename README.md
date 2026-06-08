# 校圈 CampusHub — 基于 RAG 的 AI 智能校园问答平台

## 项目简介

基于 **RAG（检索增强生成）** 技术的智能校园综合平台。将校园规章制度、办事流程、课程信息等文档向量化入库，学生通过自然语言提问，系统精准检索相关知识片段并利用 DeepSeek 大模型生成准确回答。集成 AI 对话、问答广场、公告通知、失物招领、私信等功能。

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue3 + TypeScript + Vite + Pinia + Element Plus |
| 后端 | Spring Boot 3.4 + Spring Security 6 + JWT + MyBatis Plus |
| AI | LangChain4j + DeepSeek API + pgvector |
| 缓存 | Redis |
| 消息队列 | RabbitMQ |
| 对象存储 | MinIO |
| 数据库 | MySQL 8.0 + PostgreSQL 16 (pgvector) |
| 部署 | Docker Compose + Nginx |

## 核心功能

- **AI 智能对话** — 基于 RAG 的精准问答，支持 SSE 流式输出
- **多轮对话** — 上下文记忆，连续追问
- **混合检索** — 向量语义检索 + 关键词检索，高召回率
- **文档知识库** — 支持 PDF/Word/Markdown/TXT 上传，自动解析入库
- **Markdown 渲染** — 代码高亮、表格、引用等富文本展示
- **用户鉴权** — JWT 双 Token 认证，角色权限控制
- **异步处理** — RabbitMQ 异步文档解析，不阻塞用户操作
- **反馈系统** — 回答点赞/踩，持续优化回答质量

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.9+
- Node.js 20+
- Docker & Docker Compose

### 1. 启动中间件

```bash
cd docker
docker-compose up -d mysql postgres redis rabbitmq minio
```

### 2. 配置 DeepSeek API Key

```bash
export DEEPSEEK_API_KEY=your-api-key
```

或在 `campus-server/campus-app/src/main/resources/application.yml` 中修改 `langchain4j.openai.api-key`。

### 3. 启动后端

```bash
cd campus-server
mvn clean install -DskipTests
cd campus-app
mvn spring-boot:run
```

### 4. 启动前端

```bash
cd campus-web
npm install
npm run dev
```

### 5. 访问

- 前端页面: http://localhost:5173
- 后端 API 文档: http://localhost:8080/doc.html
- RabbitMQ 管理: http://localhost:15672
- MinIO 控制台: http://localhost:9001

### 一键部署

```bash
docker-compose up -d
```

## 项目结构

```
campus-knowledge-assistant/
├── campus-server/                # Spring Boot 多模块后端
│   ├── campus-common/            # 公共模块（统一响应、异常处理）
│   ├── campus-security/          # 安全模块（Spring Security + JWT）
│   ├── campus-user/              # 用户模块（登录注册）
│   ├── campus-chat/              # 对话模块（SSE 流式对话）
│   ├── campus-rag/               # RAG 引擎（检索增强生成）
│   ├── campus-knowledge/         # 知识库模块
│   ├── campus-file/              # 文件模块（MinIO + RabbitMQ）
│   ├── campus-feedback/          # 反馈模块
│   └── campus-app/               # 启动模块
├── campus-web/                   # Vue3 前端
├── docker/                       # Docker 部署配置
└── docs/                         # 文档与 SQL
```

## 面试亮点

1. **RAG 完整 pipeline**：文档解析 → 文本分割 → 向量化 → 混合检索 → Prompt 组装 → LLM 生成
2. **SSE 流式输出**：ChatGPT 同款逐字输出体验
3. **企业级架构**：多模块、安全认证、异步消息、对象存储
4. **AI 工程化**：LangChain4j Java 生态，非 Python 专属
5. **产品思维**：Markdown 渲染、对话历史、反馈闭环、管理后台
