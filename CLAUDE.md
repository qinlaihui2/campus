# 校圈 CampusHub

基于 RAG 技术的 AI 校园问答系统。Spring Boot 3.4 + Vue 3 全栈项目。

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 + TypeScript + Vite 6 + Pinia + Element Plus |
| 后端 | Spring Boot 3.4.5 + Spring Security 6 + JWT + MyBatis Plus 3.5.9 |
| AI/RAG | LangChain4j 1.0.0-beta1 + DeepSeek API (OpenAI 兼容适配) |
| 向量库 | PostgreSQL 16 + pgvector (384-dim, all-MiniLM-L6-v2) |
| 缓存 | Redis (Redisson 3.40.2) |
| MQ | RabbitMQ (异步文档处理) |
| 对象存储 | MinIO |
| 业务库 | MySQL 8.0 |
| 部署 | Docker Compose + Nginx |

## 项目结构

```
claude-myblog/
├── campus-server/                    # Spring Boot 多模块 Maven 后端 (com.campus)
│   ├── pom.xml                       # 父 POM，12 个子模块
│   ├── campus-common/                # 共享：BaseEntity, R<T>, ResultCode, BusinessException, UserContext
│   ├── campus-security/              # JWT 认证：JwtService, JwtAuthenticationFilter, LoginUser
│   ├── campus-user/                  # 用户模块：登录/注册/个人信息
│   ├── campus-chat/                  # AI 对话：SSE 流式输出，多轮上下文记忆
│   ├── campus-rag/                   # RAG 引擎：EmbeddingService, RetrieverService, RagService
│   ├── campus-knowledge/             # 知识库管理 + 文档异步处理 (RabbitMQ Consumer)
│   ├── campus-file/                  # MinIO 文件存储 + RabbitMQ 配置
│   ├── campus-feedback/              # 反馈模块（POM 已声明，但无源码，待实现）
│   ├── campus-announcement/          # 公告系统：CRUD + 轮播管理 + 附件
│   ├── campus-message/               # 私信/IM：会话式私信，文本/图片
│   ├── campus-lost-found/            # 失物招领：发布/查找/管理
│   ├── campus-square/               # 问答广场：公开问答发布/浏览/点赞/搜索
│   └── campus-app/                   # 启动模块：CampusApplication, application.yml
├── campus-web/                       # Vue 3 + Vite 前端
│   └── src/views/chat/ChatView.vue   # 核心聊天组件 (~760 行)
├── docker/                           # docker-compose.yml + Dockerfile + nginx.conf
├── docs/                             # schema.sql + 种子数据
└── test-docs/                        # 测试文档 (PDF/MD/TXT)
```

## 模块依赖链

```
campus-common → campus-security → campus-user
campus-common → campus-rag → campus-knowledge → campus-chat
campus-common → campus-file → campus-announcement, campus-knowledge
campus-app 依赖所有模块
```

## 核心文件速查

| 用途 | 路径 |
|------|------|
| 主启动类 | `campus-server/campus-app/src/main/java/com/campus/CampusApplication.java` |
| 全部配置 | `campus-server/campus-app/src/main/resources/application.yml` |
| RAG Bean 配置 | `campus-server/campus-rag/src/main/java/com/campus/rag/config/RagConfig.java` |
| RAG Prompt 组装 | `campus-server/campus-rag/src/main/java/com/campus/rag/llm/RagService.java` |
| 向量检索 | `campus-server/campus-rag/src/main/java/com/campus/rag/retriever/RetrieverService.java` |
| 文档处理消费者 | `campus-server/campus-knowledge/src/main/java/com/campus/knowledge/consumer/DocumentProcessConsumer.java` |
| SSE 聊天接口 | `campus-server/campus-chat/src/main/java/com/campus/chat/controller/ChatController.java` |
| 聊天服务(核心) | `campus-server/campus-chat/src/main/java/com/campus/chat/service/ChatService.java` |
| JWT 过滤器 | `campus-server/campus-security/src/main/java/com/campus/security/filter/JwtAuthenticationFilter.java` |
| 登录逻辑(含锁定) | `campus-server/campus-user/src/main/java/com/campus/user/service/impl/UserServiceImpl.java` |
| 数据库 Schema | `docs/schema.sql` |
| 前端路由 | `campus-web/src/router/index.ts` |
| 前端聊天页 | `campus-web/src/views/chat/ChatView.vue` |
| 前端广场页 | `campus-web/src/views/square/SquareView.vue` |
| Docker 编排 | `docker/docker-compose.yml` |

## 关键配置项 (application.yml)

- JWT: access 30min / refresh 7d
- RAG: chunk_size=500, overlap=50, top_k=5, similarity_threshold=0.0
- DeepSeek: `base-url: https://api.deepseek.com`, `model-name: deepseek-chat`，通过 LangChain4j OpenAI 兼容适配器调用
- 嵌入模型: all-MiniLM-L6-v2 (384维)，本地 ONNX 运行
- PostgreSQL 数据源名为 `secondary`，专用于 pgvector
- 对话上下文: 最近 20 条消息 (10 轮)

## 启动命令

```bash
# 1. 中间件
cd docker && docker-compose up -d mysql postgres redis rabbitmq minio

# 2. 后端 (需先设置 DEEPSEEK_API_KEY 环境变量)
cd campus-server && mvn clean install -DskipTests
cd campus-app && mvn spring-boot:run

# 3. 前端
cd campus-web && npm install && npm run dev
```

## 注意事项

- campus-feedback 模块在 POM 中声明但没有任何 Java 源码，是空壳模块
- 消息反馈（点赞/踩）目前直接写在 ChatController 的 message 表字段上，不在 feedback 模块
- similarity_threshold=0.0 意味着所有 top-K 结果都会返回，生产环境应调高
- 会话删除是软删除（status=0），非物理删除
- 私信模块是 REST 轮询模式，非 WebSocket 实时推送
- 前端 SSE 使用 AbortController 支持停止生成
