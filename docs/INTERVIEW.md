# 校圈 CampusHub — 面试参考文档

> 面试前一天翻一遍，把关键代码位置记一记。

---

## 一、项目速览（1 分钟电梯演讲）

**校圈 CampusHub** 是一个基于 RAG（检索增强生成）技术的 AI 校园综合平台。Spring Boot 3.4 多模块后端 + Vue 3 前端，集成 DeepSeek 大模型、pgvector 向量检索、SSE 流式输出。覆盖 AI 对话、课程学习、问答广场、知识库管理、私信 IM、失物招领、二手交易市场等 16 个业务模块。32 个自动化测试（JUnit 5 + Mockito + MockMvc）。

```
技术栈一句话：
Spring Boot 3.4 + Vue 3 + MyBatis Plus + LangChain4j + DeepSeek + pgvector + Redis + RabbitMQ + MinIO + Docker
```

**关键数字**：
- 16 个 Maven 模块
- 32 个测试全部通过
- 7 种中间件（MySQL / PG+pgvector / Redis / RabbitMQ / MinIO / Nginx / Docker）
- SSE 流式 AI 对话，20 条上下文窗口
- Redisson 分布式限流 + JWT 双 Token 认证
- WebSocket 实时私信推送
- 完整 CI/CD：Git push → 服务器 git pull → Maven 重编 → 前端 npm build → Nginx 热更

---

## 二、引导面试官的 3 个钩子

> 主动提这些点，把面试官引向你准备过的方向：

1. **"我们这个项目最核心的是 RAG pipeline，从文档上传到 AI 回答，中间经过了 5 个环节……"** → 引到 RAG 原理
2. **"做评论点赞功能时，从数据库表设计到前端 UI 完整链路都是自己开的……"** → 引到全栈能力
3. **"踩过 Redis 缓存反模式的坑，知道了简单 CRUD 不该用缓存……"** → 引到工程经验

---

## 三、面试 Q&A（14 题）

---

### Q1: 为什么用 Maven 多模块？怎么划分的？

**面试官意图**：看你是不是只会写代码，有没有架构思维。

**答**：
按业务领域 + 通用能力分层拆分，14 个模块依赖关系是单向的：

```
campus-common（公共层：统一响应、异常、实体基类）
  ├── campus-security（JWT 认证）
  ├── campus-rag（RAG 引擎）
  ├── campus-file（MinIO 文件）
  └── ...
campus-app（启动模块，依赖所有模块）
```

核心原则：
- `campus-common` 零业务依赖，只放 BaseEntity / R<T> / UserContext / 全局异常处理器
- 业务模块之间通过接口解耦（如 ChatController 注入 FeedbackService 来自 feedback 模块）
- `campus-app` 只管启动和全局配置，不写业务代码

**延伸**：如果要拆微服务，可以把 chat + rag 拆成 AI 服务，user + security 拆成用户服务。

**关键文件**：`campus-server/pom.xml` 的 `<modules>` 列表

---

### Q2: RAG 完整 Pipeline 是什么？

**面试官意图**：验证你真的理解 RAG，不是调了个 API。

**答**（顺着 pipeline 说）：
```
文档上传 → 文本解析 → Chunk 分割 → 向量化 → 存入 pgvector
  ↓
用户提问 → 向量检索(top_k=5) → Prompt 组装 → DeepSeek 生成 → SSE 流式返回
```

具体技术细节：
1. **文档解析**：LangChain4j 的 PdfBox/WordParser，支持 PDF/DOCX/MD/TXT
2. **分割策略**：`chunk_size=500, overlap=50`，防止上下文截断（`application.yml` 中配置）
3. **向量化**：all-MiniLM-L6-v2，384 维，本地 ONNX 运行，不调 API 省钱
4. **检索**：pgvector `cosine_similarity` + MySQL FULLTEXT 关键词兜底（混合检索）
5. **Prompt 组装**：`RagService.java` 中把检索片段 + 对话历史 + 用户问题拼成 prompt
6. **流式输出**：`ChatController.send()` 返回 `SseEmitter`，前端 `AbortController` 支持停止生成

**关键文件**：
- RAG 配置：`campus-server/campus-app/src/main/resources/application.yml`
- Prompt 组装：`campus-server/campus-rag/src/main/java/com/campus/rag/llm/RagService.java`
- SSE 接口：`campus-server/campus-chat/src/main/java/com/campus/chat/controller/ChatController.java:32`

**追问预警**："为什么 chunk_size 选 500？"
→ DeepSeek 上下文 128K，500 字符一个 chunk 配合 overlap=50，检索精度和速度的折中。

---

### Q3: 为什么对话用 SSE 而不是 WebSocket？

**面试官意图**：考察技术选型能力，不被"WebSocket 最高级"忽悠。

**答**：
AI 对话是**单向推送**（服务端→客户端），不是双向通信。SSE 的优势：
1. **更轻量**：基于 HTTP/1.1，不需要升级协议、不需要心跳 ping/pong
2. **浏览器原生支持**：`EventSource` API，断线自动重连
3. **穿透代理**：HTTP 协议天然过 Nginx/代理，WebSocket 要额外配置
4. **我们这个项目私信模块用的是 WebSocket**（`ChatWebSocketServer.java`），因为私信需要双向实时——正好说明我没有一刀切

**关键文件**：
- SSE：`ChatController.java:32` → `SseEmitter`
- WebSocket：`campus-message/.../websocket/ChatWebSocketServer.java`

---

### Q4: JWT 双 Token 怎么设计的？

**面试官意图**：看你对认证流程的理解深度。

**答**：
- **Access Token**：有效期 30 分钟，用于所有 API 鉴权
- **Refresh Token**：有效期 7 天，只用于换新 Access Token
- 登录时两个 Token 一起返回，前端存 localStorage
- `JwtAuthenticationFilter` 拦截所有请求，从 `Authorization: Bearer xxx` 中提取并验证
- `UserContext`（ThreadLocal）存储当前用户信息，请求结束自动清理

**为什么双 Token**：
- Access Token 短期降低泄露风险
- Refresh Token 长期避免频繁登录
- 如果只用长 Token → 泄露后无法撤销；只用短 Token → 每半小时重新登录

**异常处理**：
- 账号锁定 15 分钟（`UserServiceImpl.java` 中登录失败计数）
- 密码 BCrypt 加密

**关键文件**：
- `campus-security/.../JwtService.java` — Token 生成/验证
- `campus-security/.../JwtAuthenticationFilter.java` — 拦截器
- `campus-common/.../UserContext.java` — ThreadLocal 上下文

---

### Q5: 点赞/浏览量的并发竞态怎么解决？

**面试官意图**：考察并发编程和数据库功底。

**答**：
有两个场景：

**场景 1：浏览量递增**
- 问题：`course.viewCount = course.viewCount + 1; updateById(course)` 存在 Read-Modify-Write 竞态
- 解决：用 MySQL 原子操作 `UPDATE course SET view_count = view_count + 1 WHERE id = ?`
- 代码：`CourseServiceImpl.java` 中 `LambdaUpdateWrapper.setSql("view_count = view_count + 1")`

**场景 2：点赞 Toggle**
- 问题：同一用户重复点怎么办？
- 解决：`UNIQUE INDEX(course_id, user_id)` + INSERT/DELETE toggle 模式
  - 点 → INSERT（成功=已点赞）
  - 再点 → DELETE（取消点赞）
  - 同时更新 `like_count` 字段冗余（读多写少，避免 JOIN）
- 这个方案保证了**幂等性**——同一用户同一资源只会有一条记录

**关键文件**：`campus-course/.../CourseServiceImpl.java` 的 `toggleLike()` 和 `getDetail()` 方法

---

### Q6: RabbitMQ 做了什么？为什么用消息队列？

**面试官意图**：MQ 使用场景和原理。

**答**：
**唯一用途**：文档异步处理。

流程：
```
用户上传文档 → MinIO 存储 → 发送 documentId 到 RabbitMQ → 
Consumer 异步消费 → 下载文件 → 解析文本 → Chunk 分割 → 
向量化 → 写入 pgvector → 更新文档状态为 COMPLETED
```

**为什么用 MQ**：
1. **削峰**：大文件解析可能耗时数分钟，不能让用户 HTTP 请求干等
2. **解耦**：上传服务和解析服务分离，解析失败不影响上传成功返回
3. **重试**：RabbitMQ 自带消息持久化和重试机制
4. **状态回写**：解析完成后 Consumer 更新 `document.parse_status`，前端轮询或查状态

**关键文件**：
- 生产者：`campus-file/.../FileService.java` → `sendDocumentProcessMessage()`
- 消费者：`campus-knowledge/.../consumer/DocumentProcessConsumer.java`

---

### Q7: MySQL 和 PostgreSQL 分别存什么？

**面试官意图**：数据库选型依据。

**答**：

| 数据库 | 用途 | 原因 |
|--------|------|------|
| **MySQL 8.0** | 所有业务数据（用户、课程、帖子、私信……） | 生态成熟、团队熟悉、MyBatis Plus 支持好 |
| **PostgreSQL 16 + pgvector** | 仅向量数据（`document_chunk.embedding`） | pgvector 是目前最成熟的 Postgres 向量插件 |

**为什么不用 MySQL 的向量功能？**
- MySQL 没有原生向量索引，pgvector 支持 IVFFlat/HNSW 索引，cosine 相似度查询性能好得多

**配置**：Spring Boot 双数据源，`primary` 指向 MySQL，`secondary` 指向 PG。

---

### Q8: Redis 用了哪些场景？

**面试官意图**：缓存不是只有 `cache aside`。

**答**：
1. **会话缓存（待扩展）**：可缓存 JWT Token 黑名单
2. **接口限流**：`@RateLimit` 注解 + Redisson `RRateLimiter`，AOP 切面实现
   - 例如聊天接口：每人每秒最多 5 次 `@RateLimit(key = "chat_send", permitsPerSecond = 5)`
   - `RateLimitAspect.java` 中按 `类名:方法名:userId` 粒度限流
3. **分布式锁（备用）**：Redisson 自带，后续可用于防止重复提交

**关键文件**：
- 注解：`campus-common/.../annotation/RateLimit.java`
- AOP：`campus-common/.../aspect/RateLimitAspect.java`

---

### Q9: 怎么保证代码质量？

**面试官意图**：有没有工程化意识。

**答**：
三层测试体系：

| 层级 | 框架 | 数量 | 覆盖内容 |
|------|------|------|----------|
| 单元测试 | JUnit 5 + Mockito + AssertJ | 25 | JWT 生成/验证、反馈 toggle/统计、课程 CRUD/点赞/收藏 |
| 集成测试 | MockMvc + standalone setup | 7 | Course API、Feedback API、My API 的 HTTP 请求链路 |
| API 文档 | Knife4j（Swagger 3） | — | `/doc.html` 在线接口文档，自动扫描 `@RestController` |

**其他质量手段**：
- 统一异常处理（`GlobalExceptionHandler.java` + `BusinessException`）
- 统一响应格式（`R<T>` + `ResultCode` 枚举）
- 参数校验（`@Valid` + JSR-303）
- MyBatis Plus `@TableLogic` 软删除（所有 delete 操作实际是 `update deleted = 1`）

---

### Q10: LangChain4j vs Python LangChain？

**面试官意图**：技术选型的思考深度。

**答**：
**选 LangChain4j 的原因**：
1. **Java 原生**：无需引入 Python 服务，部署成本低，Spring Boot 直接注入
2. **项目场景匹配**：校园系统用 Java 全栈，不需要 Python 生态的 numpy/pandas
3. **性能**：本地 ONNX embedding 在 JVM 中运行，避免跨语言调用开销
4. **1.0.0-beta1**：虽然不是正式版，但核心功能（文档加载器、向量存储、链式调用）已稳定

**什么场景会用 Python LangChain**：
- 需要频繁切换 embedding/chunking 策略的实验阶段
- 团队以 Python 为主
- 需要 LangChain 最新的 Agent/Tool 生态（Java 版滞后半年左右）

**关键文件**：`campus-server/campus-rag/src/main/java/com/campus/rag/config/RagConfig.java`

---

### Q11: 如果要拆成微服务，怎么拆？

**面试官意图**：架构演进思维。

**答**：

| 服务 | 包含模块 | 原因 |
|------|----------|------|
| **AI 服务** | chat + rag + knowledge | 计算密集，独立扩缩容 |
| **用户服务** | user + security + feedback | 核心认证，独立安全策略 |
| **内容服务** | course + square + announcement + lost-found | 内容管理集群 |
| **消息服务** | message（已有 WebSocket） | 长连接独立部署 |
| **文件服务** | file | 已有 MinIO，可独立 CDN |
| **网关** | campus-app → Spring Cloud Gateway | 统一入口、鉴权、限流 |

**需要补充的技术**：
- Spring Cloud Alibaba（Nacos 注册中心/配置中心）
- Sentinel 熔断限流（替代 Redisson）
- OpenFeign 服务调用
- Seata 分布式事务（课程点赞 + 积分发放的跨服务场景）

---

### Q12: 项目中遇到最难的问题是什么？

**面试官意图**：问题定位和解决能力，不是问"有没有 bug"。

**答（选一个）**：

**选择 1：双重 UTF-8 编码乱码**
- 现象：课程中文标题在数据库里变成 MojiBake 乱码
- 排查：HEX 看了发现是 UTF-8 字节被当作 Latin-1 再编码（`C3A7C2BC` 特征）
- 原因：`docker cp` SQL 文件进容器后 `source` 命令用了默认 charset
- 解决：改用 `mysql --default-character-set=utf8mb4 < file.sql`

**选择 2：SSE 流式输出被 Nginx 缓冲**
- 现象：前端收不到逐字推送，而是一次性收到完整回答
- 原因：Nginx 默认开启 proxy_buffering
- 解决：Nginx location 加 `proxy_buffering off; proxy_cache off; X-Accel-Buffering: no`

**选择 3：axios 把 JavaScript undefined 当字符串发给后端**
- 现象：公告管理"全部"标签页和后台列表查询为空，但按分类筛选却正常
- 排查：抓包发现请求 URL 是 `/api/announcements?category=undefined&page=1`，`undefined` 被 axios 序列化成了字符串
- 原因：`axios.get('/api', { params: { category: undefined } })` 中 `undefined` 没有被过滤，URLSearchParams 将其转为字符串 `"undefined"`
- 解决：前端 API 层用 `if (category) params.category = category` 条件添加参数，而不是直接传 `undefined`
- 教训：永远不要信任前端传参，后端也要防御空值（`listByCategory` 增加 `"undefined".equalsIgnoreCase(category)` 兜底）

**选择 4：Redis 缓存用于简单 CRUD 的反模式**
- 现象：公告管理页面删除后数据仍然显示，用户体验"删不掉"
- 排查：后端 `removeById` 成功（deleted=1），但前端列表 API 返回的数据里被删记录仍然存在
- 原因：`@Cacheable` 缓存了公告列表（TTL 5分钟），`@CacheEvict` 在 Controller 层未正确清 Redis 缓存，AOP 代理链顺序问题导致 `@CacheEvict` 不生效
- 解决：彻底移除公告模块的 `@Cacheable`/`@CacheEvict`，公告列表直接查 MySQL——数据量小、查询简单，缓存弊大于利
- 教训：不是所有场景都该用缓存。**简单 CRUD 用缓存 = 过度设计**，缓存适用于读多写少、查询昂贵的场景

**选择 3（旧）：Service Impl 继承 MyBatis Plus ServiceImpl 导致 Mock 困难**
- 现象：单元测试中 `@InjectMocks` 无法注入 `baseMapper`
- 解决：`ReflectionTestUtils.setField(courseService, "baseMapper", courseMapper)` 手动注入

---

### Q13: 从零开发一个全栈功能（评论点赞）的全流程是什么？

**面试官意图**：看你有没有独立负责一个完整功能的经验。

**答（以二手市场评论点赞为例，6 层贯通）**：

```
数据库层: CREATE TABLE market_comment_like (id, comment_id, user_id, UNIQUE INDEX)
     ↓
实体层:   MarketCommentLike.java (@TableName("market_comment_like"))
     ↓
持久层:   MarketCommentLikeMapper.java (extends BaseMapper<MarketCommentLike>)
     ↓
服务层:   MarketService.toggleCommentLike() — toggle 模式：INSERT/DELETE + like_count 原子更新
     ↓
控制器层: MarketController.likeComment() — POST /api/market/comments/{commentId}/like
     ↓
前端 API: likeMarketComment(id) — POST /market/comments/{id}/like
     ↓
前端 UI: CommentItem.vue 点赞按钮 + onLikeComment() 响应式更新 liked + likeCount
```

核心设计：
- **Toggle 模式**：同一用户再点 = 取消点赞（幂等）
- **UNIQUE INDEX(comment_id, user_id)**：数据库层面保证不重复
- **冗余 like_count**：避免每次查评论列表都要 COUNT 子查询
- **前端即时响应**：`comment.liked = true; comment.likeCount += 1` 不刷新列表

**关键文件**（6 个文件）：
- DDL: `docs/schema.sql` → `market_comment_like`
- Entity: `campus-market/.../entity/MarketCommentLike.java`
- Mapper: `campus-market/.../mapper/MarketCommentLikeMapper.java`
- Service: `campus-market/.../service/impl/MarketServiceImpl.java:228-260`
- Controller: `campus-market/.../controller/MarketController.java:118-122`
- Frontend: `campus-web/src/api/market.ts` + `src/views/market/MarketDetailView.vue`

---

### Q14: 多模块 Maven 项目怎么增量编译一个模块？

**面试官意图**：考察 Maven 工程化和模块化理解。

**答**：

```bash
# 只编译 campus-market 模块及它在当前 reactor 中的依赖
mvn clean install -pl campus-market -am -DskipTests

# 完整部署流程
mvn clean install -pl campus-market,campus-app -am -DskipTests
kill $(ps aux | grep CampusApplication | grep -v grep | awk '{print $2}')
nohup mvn -pl campus-app spring-boot:run --profile=server > /tmp/campus.log 2>&1 &
```

关键点：
- `-pl` 指定目标模块
- `-am` (also-make) 自动编译依赖链
- 改了一个模块的代码，只重编受影响的模块 + 启动模块
- `spring-boot:run` 使用 `-pl campus-app` 需要先 `install` 目标模块到本地仓库，否则 running class 还是旧 JAR

**常见坑**：改了子模块代码直接用 `-pl campus-app spring-boot:run` 启动，Maven 不会自动重编依赖模块——必须先用 `install` 把新 JAR 打到本地仓库。

---

## 四、面试前 Checklist

- [ ] 能画出 14 模块的依赖关系图
- [ ] 能背出技术栈（Spring Boot 3.4 + Vue 3 + DeepSeek + pgvector）
- [ ] 能讲清楚 RAG 5 步 pipeline
- [ ] 知道关键代码在哪个文件哪一行
- [ ] 准备好 3 个"钩子问题"
- [ ] 32 个测试全部通过（`mvn test` 确认）
- [ ] `/doc.html` API 文档能打开
- [ ] 知道怎么回答"你在这个项目里具体做了什么"
