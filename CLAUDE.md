# 校圈 CampusHub

基于 RAG 技术的 AI 校园综合平台。Spring Boot 3.4 + Vue 3 全栈项目。

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 + TypeScript + Vite 6 + Pinia + Element Plus |
| 后端 | Spring Boot 3.4.5 + Spring Security 6 + JWT + MyBatis Plus 3.5.9 |
| AI/RAG | LangChain4j 1.0.0-beta1 + DeepSeek API (OpenAI 兼容适配) |
| 向量库 | PostgreSQL 16 + pgvector (384-dim, all-MiniLM-L6-v2) |
| 缓存/限流 | Redis + Redisson 3.40.2 (RRateLimiter) |
| MQ | RabbitMQ (异步文档处理) |
| 实时通信 | WebSocket (私信推送) |
| 对象存储 | MinIO |
| 业务库 | MySQL 8.0 |
| API 文档 | Knife4j (Swagger 3) |
| 测试 | JUnit 5 + Mockito + AssertJ + MockMvc (32 tests) |
| 部署 | Docker Compose + Nginx |

## 项目结构

```
claude-myblog/
├── campus-server/                    # Spring Boot 多模块 Maven 后端 (com.campus)
│   ├── pom.xml                       # 父 POM，14 个子模块
│   ├── campus-common/                # 共享：BaseEntity, R<T>, ResultCode, UserContext, @RateLimit, RateLimitAspect
│   ├── campus-security/              # JWT 认证：JwtService, JwtAuthenticationFilter, LoginUser
│   ├── campus-user/                  # 用户模块：登录/注册/个人信息，登录失败锁定
│   ├── campus-chat/                  # AI 对话：SSE 流式输出，多轮上下文记忆，@RateLimit 限流
│   ├── campus-rag/                   # RAG 引擎：EmbeddingService, RetrieverService, RagService
│   ├── campus-knowledge/             # 知识库管理 + RabbitMQ 异步文档处理
│   ├── campus-file/                  # MinIO 文件存储 + RabbitMQ 配置
│   ├── campus-course/                # 课程学习：视频播放/章节管理/评论树/点赞/收藏
│   ├── campus-square/                 # 问答广场：公开问答发布/浏览/点赞/全文搜索
│   ├── campus-message/               # 私信/IM：会话式私信 + WebSocket 实时推送
│   ├── campus-feedback/              # 反馈系统：消息点赞/踩 toggle，反馈统计
│   ├── campus-my/                    # 个人中心：跨模块点赞/收藏聚合查询
│   ├── campus-announcement/          # 公告系统：CRUD + 轮播管理 + 附件
│   ├── campus-lost-found/            # 失物招领：发布/查找/管理
│   └── campus-app/                   # 启动模块：CampusApplication, OpenApiConfig, application.yml
├── campus-web/                       # Vue 3 + Vite 前端
│   └── src/views/
│       ├── chat/ChatView.vue         # 核心聊天 + 侧栏导航 (~780 行)
│       ├── course/CourseListView.vue # 课程列表（瀑布流卡片）
│       ├── course/CourseDetailView.vue # 课程详情（视频+章节+评论）
│       ├── my/MyView.vue             # 我的点赞/收藏（抖音风双列卡片）
│       └── message/MessageView.vue   # 私信（WebSocket 实时）
├── docker/                           # docker-compose.yml + Dockerfile + nginx.conf
└── docs/                             # schema.sql + 种子数据 + INTERVIEW.md
```

## 模块依赖链

```
campus-common → campus-security → campus-user
campus-common → campus-rag → campus-knowledge → campus-chat → campus-feedback
campus-common → campus-file → campus-announcement, campus-knowledge
campus-common → campus-course
campus-common → campus-square
campus-common → campus-message
campus-common → campus-my → campus-course, campus-square
campus-app 依赖所有模块
```

## 核心文件速查

| 用途 | 路径 |
|------|------|
| 主启动类 | `campus-server/campus-app/src/main/java/com/campus/CampusApplication.java` |
| 全部配置 | `campus-server/campus-app/src/main/resources/application.yml` |
| OpenAPI 文档配置 | `campus-server/campus-app/src/main/java/com/campus/config/OpenApiConfig.java` |
| RAG Prompt 组装 | `campus-server/campus-rag/src/main/java/com/campus/rag/llm/RagService.java` |
| 向量检索 | `campus-server/campus-rag/src/main/java/com/campus/rag/retriever/RetrieverService.java` |
| 文档处理消费者 | `campus-server/campus-knowledge/src/main/java/com/campus/knowledge/consumer/DocumentProcessConsumer.java` |
| SSE 聊天接口(含限流) | `campus-server/campus-chat/src/main/java/com/campus/chat/controller/ChatController.java` |
| 聊天服务(含反馈) | `campus-server/campus-chat/src/main/java/com/campus/chat/service/ChatService.java` |
| JWT 服务 | `campus-server/campus-security/src/main/java/com/campus/security/service/JwtService.java` |
| JWT 过滤器 | `campus-server/campus-security/src/main/java/com/campus/security/filter/JwtAuthenticationFilter.java` |
| 登录逻辑(含锁定) | `campus-server/campus-user/src/main/java/com/campus/user/service/impl/UserServiceImpl.java` |
| 课程服务(核心) | `campus-server/campus-course/src/main/java/com/campus/course/service/impl/CourseServiceImpl.java` |
| 反馈服务 | `campus-server/campus-feedback/src/main/java/com/campus/feedback/service/impl/FeedbackServiceImpl.java` |
| 个人中心服务 | `campus-server/campus-my/src/main/java/com/campus/my/service/impl/MyServiceImpl.java` |
| 限流注解 | `campus-server/campus-common/src/main/java/com/campus/common/annotation/RateLimit.java` |
| 限流 AOP | `campus-server/campus-common/src/main/java/com/campus/common/aspect/RateLimitAspect.java` |
| WebSocket 服务 | `campus-server/campus-message/src/main/java/com/campus/message/websocket/ChatWebSocketServer.java` |
| 数据库 Schema | `docs/schema.sql` |
| 面试文档 | `docs/INTERVIEW.md` |
| 前端路由 | `campus-web/src/router/index.ts` |
| 前端聊天页 | `campus-web/src/views/chat/ChatView.vue` |
| 前端课程页 | `campus-web/src/views/course/CourseDetailView.vue` |
| 前端我的页 | `campus-web/src/views/my/MyView.vue` |
| Docker 编排 | `docker/docker-compose.yml` |

## 关键配置项 (application.yml)

- JWT: access 30min / refresh 7d
- RAG: chunk_size=500, overlap=50, top_k=5, similarity_threshold=0.0
- DeepSeek: `base-url: https://api.deepseek.com`, `model-name: deepseek-chat`
- 嵌入模型: all-MiniLM-L6-v2 (384维)，本地 ONNX 运行
- PostgreSQL 数据源名为 `secondary`，专用于 pgvector
- 对话上下文: 最近 20 条消息 (10 轮)
- Knife4j: enabled, 中文, `/doc.html` 可访问

## 测试

```bash
# 全部测试
cd campus-server && mvn test

# 指定模块
mvn test -pl campus-course,campus-feedback,campus-security

# 集成测试
mvn test -pl campus-app
```

| 测试类型 | 数量 | 覆盖 |
|----------|------|------|
| JWT 服务 | 6 | Token 生成/验证/过期 |
| 反馈服务 | 7 | toggle/统计/取消 |
| 课程服务 | 12 | CRUD/点赞/收藏/评论 |
| API 集成 | 7 | Course/Feedback/My 控制器 |
| **合计** | **32** | **全部通过** |

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

## 访问地址

| 服务 | URL |
|------|-----|
| 前端 | http://localhost:5173 |
| API 文档 | http://localhost:8080/doc.html |
| RabbitMQ | http://localhost:15672 (guest/guest) |
| MinIO | http://localhost:9001 (minioadmin/minioadmin) |

## 注意事项

- 私信已从 REST 轮询升级为 WebSocket 实时推送（`/ws/message/{userId}`），3 秒自动重连
- 接口限流通过 `@RateLimit` 注解声明，基于 Redisson RRateLimiter，AOP 实现
- 反馈模块已完整实现（entity/mapper/service/controller），toggle 模式防重复
- 课程视频默认使用 sample-video.mp4 占位，替换真实视频只需改 course_chapter.video_url
- 课程封面已上传 13 张壁纸到 MinIO，每门课不同封面
- `similarity_threshold=0.0` 意味着所有 top-K 结果都会返回，生产环境应调高
- 会话删除是软删除（status=0），非物理删除
- 课程章节删除是软删除（deleted=1）
- 前端 SSE 使用 AbortController 支持停止生成
