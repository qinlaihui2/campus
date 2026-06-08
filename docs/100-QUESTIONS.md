# Java 后端面试 110 题速记

> 校圈 CampusHub 项目配套 · 每题 80-200 字 · 速记体 · 非教科书
> ★ 高频必考　★★ 常考　★★★ 偶尔考

---

## 1. Java 基础语法（10 题）

### 1.1 面向对象四大特性？★★★

**答**：封装（private 隐藏细节，暴露 public API）、继承（extends 复用代码，单继承）、多态（父类引用指向子类对象，运行时绑定）、抽象（abstract/interface 定义契约）。项目中 `BaseEntity` 封装了 id/createdAt 等公共字段，`CourseService` 接口定义契约，`CourseServiceImpl` 实现多态。

**CampusHub**：`BaseEntity.java`、`CourseService.java`

---

### 1.2 final 能修饰什么？各有什么作用？★★★

**答**：final 修饰**类**→不可继承（如 String）；修饰**方法**→不可重写；修饰**变量**→不可修改（基本类型值不变，引用类型地址不变但对象内容可变）。项目中常量类用 `final` 修饰，避免被继承篡改。JWT 的 secret key 一般也用 final 常量。

**CampusHub**：`JwtService.java` 中的 secret/expiration 用 final 修饰

---

### 1.3 static 修饰的变量和方法存在哪？★★★

**答**：JDK 8 存放在**元空间（Metaspace）**，不再是永久代。static 变量类加载时初始化，所有实例共享。static 方法无 this，只能访问 static 成员。项目中 `UserContext` 的 ThreadLocal 就是 `private static`，全局唯一。

**CampusHub**：`UserContext.java` — `private static final ThreadLocal<Long> userIdHolder`

---

### 1.4 深拷贝和浅拷贝区别？★★

**答**：浅拷贝只复制引用地址，两个对象共享内部引用指向的同一块内存；深拷贝递归复制所有引用对象，完全独立。实现方式：1) 实现 Cloneable + 递归 clone()；2) 序列化/反序列化（JSON、ObjectOutputStream）；3) 手动 new + set。项目中 DTO 转换用 `BeanUtils.copyProperties`，这是浅拷贝。

---

### 1.5 内部类有哪几种？★★

**答**：1) **成员内部类**（非 static，持有外部类引用）；2) **静态内部类**（static，不持有外部类引用，推荐）；3) **局部内部类**（方法内定义）；4) **匿名内部类**（new 接口/抽象类，Lambda 替代）。项目中 `RateLimitAspect` 里用 Lambda 代替匿名内部类，静态内部类常用于 Builder 模式（Lombok @Builder）。

**CampusHub**：`RateLimitAspect.java` 中的 Lambda 表达式

---

### 1.6 异常分类？checked vs unchecked？★★★

**答**：Throwable 下分 Error（OOM/StackOverflow，不处理）和 Exception。Exception 分 checked（编译时强制 try-catch，如 IOException）和 unchecked（RuntimeException，如 NPE）。项目中自定义 `BusinessException` 继承 RuntimeException，被 `GlobalExceptionHandler` 统一捕获。

**CampusHub**：`BusinessException.java` + `GlobalExceptionHandler.java`

---

### 1.7 泛型擦除是什么？有什么影响？★★

**答**：Java 泛型只在编译期有效，编译后类型参数被擦除，替换为边界类型或 Object。影响：1) 不能 `instanceof` 泛型；2) 不能 new 泛型数组；3) 不能重载泛型不同的方法（擦除后签名相同）；4) 反射可绕过泛型检查。项目中 `R<T>` 统一返回类型就是泛型应用。

**CampusHub**：`common/R.java` — `public class R<T>`

---

### 1.8 反射怎么用？有什么场景？★★

**答**：`Class.forName()` 获取 Class，`getDeclaredMethod/Field` 获取成员，`setAccessible(true)` 绕过访问控制，`invoke()` 调用。场景：Spring 的 `@Autowired` 字段注入、AOP 动态代理、MyBatis 结果映射。项目中 `RateLimitAspect` 通过反射获取方法上的 `@RateLimit` 注解信息。

**CampusHub**：`RateLimitAspect.java` — 反射读取 `@RateLimit` 注解参数

---

### 1.9 String/StringBuilder/StringBuffer 区别？★★★

**答**：String 不可变（final char[]），每次拼接产生新对象；StringBuilder 可变，线程不安全，性能最高；StringBuffer 可变，synchronized 线程安全，性能次之。项目中日志拼接、短字符操作用 `String.format` 或 `+`（编译器优化为 StringBuilder），循环中大量拼接显式用 StringBuilder。

---

### 1.10 Integer 缓存池是什么？★★

**答**：Integer 默认缓存 -128 ~ 127 的对象（`IntegerCache`），`valueOf()` 会复用。`Integer a = 127; Integer b = 127; a == b` 是 true（缓存池同一对象）；`Integer a = 128; a == b` 是 false（new 了两个不同对象）。项目中比较 `Long` 类型的 userId，始终用 `equals` 不用 `==`。

**CampusHub**：`UserContext.getUserId()` 返回 Long，比较用 equals

---

> **面试官追问**：泛型 `<T extends Comparable<? super T>>` 是什么意思？→ PECS 原则：Producer Extends, Consumer Super。

---

## 2. Java 集合（10 题）

### 2.1 ArrayList vs LinkedList？★★★

**答**：ArrayList 底层 `Object[]`，随机访问 O(1)，尾插 O(1)，中间插入 O(n)（System.arraycopy 移动）。LinkedList 双向链表，随机访问 O(n)，头尾操作 O(1)。项目中课程列表、评论列表读多写少，全用 ArrayList。

---

### 2.2 HashMap 的 put 流程（JDK 8）？★★★

**答**：1) 对 key 做 hash（高 16 位异或低 16 位减少碰撞）；2) `(n-1) & hash` 定位数组下标；3) 该位置为空→直接放；4) 不为空→equals 判断同一 key→覆盖；5) 不是同一 key→判断是红黑树还是链表→链表尾插；6) 链表长度 ≥8 且数组 ≥64→转红黑树；7) size > threshold（容量×0.75）→扩容 2 倍 rehash。

---

### 2.3 ConcurrentHashMap 为什么线程安全？★★★

**答**：JDK 8 用 **CAS + synchronized 分段锁**。put 时：bin 为空→CAS 尝试赋值；bin 有数据→对 bin 头节点加 synchronized 锁，锁粒度在 bin 级别。get 无锁（Node 的 val 和 next 是 volatile）。项目中 `ChatWebSocketServer` 用 ConcurrentHashMap 存所有在线用户连接，多线程安全。

**CampusHub**：`ChatWebSocketServer.java` — `ConcurrentHashMap<Long, Session>`

---

### 2.4 TreeMap 和 LinkedHashMap 的区别？★★

**答**：TreeMap 基于红黑树，key 按自然顺序或 Comparator 排序，put/get O(logn)。LinkedHashMap 基于 HashMap + 双向链表，维护插入顺序或访问顺序（accessOrder=true 实现 LRU）。项目中不需要排序的映射用 HashMap，需要 LRU 缓存可考虑 LinkedHashMap。

---

### 2.5 HashSet 去重原理？★★

**答**：底层就是 HashMap，元素作为 key，value 是一个固定的 PRESENT 对象（new Object()）。`add(E e)` 调用 `map.put(e, PRESENT)`，利用 HashMap 的 key 唯一性去重。所以放入 HashSet 的元素必须重写 `hashCode()` 和 `equals()`，否则去重失效。

---

### 2.6 fail-fast 机制是什么？★★

**答**：集合用 `modCount` 记录结构性修改次数。迭代器初始化时记录 `expectedModCount = modCount`，每次 `next()/remove()` 检查两者是否相等，不等则抛 `ConcurrentModificationException`。增强 for 循环底层也是迭代器。解决方案：用 `CopyOnWriteArrayList`（写时复制）或 `Iterator.remove()`。

---

### 2.7 HashMap 为什么容量是 2 的幂？★★

**答**：1) `(n-1) & hash` 等价于 `hash % n`，位运算更快；2) 扩容时只需判断 hash 新增 bit 是 0 还是 1，0→原位，1→原位+旧容量，rehash 更均匀。默认容量 16（2^4），扩容翻倍。

---

### 2.8 ArrayList 扩容机制？★★

**答**：默认容量 10，`add()` 超出时扩容为原来的 1.5 倍（`oldCapacity + oldCapacity >> 1`），用 `Arrays.copyOf()` 复制到新数组。如果知道大概元素数量，构造时指定 `new ArrayList(n)` 避免频繁扩容。

---

### 2.9 BlockingQueue 用过吗？★★

**答**：阻塞队列，常用于生产者-消费者模式。ArrayBlockingQueue（有界，数组实现，一把锁）、LinkedBlockingQueue（可选有界，链表实现，两把锁）。项目中间接用 RabbitMQ 实现异步解耦（底层也是队列思想），线程池的 `workQueue` 就是 BlockingQueue。

**CampusHub**：线程池配置中的 workQueue 就是 BlockingQueue

---

### 2.10 Collections.synchronizedMap 和 ConcurrentHashMap 区别？★★

**答**：前者用 synchronized 包裹所有方法，锁整张表，并发度低（1）。后者分段锁（JDK 7 Segment，JDK 8 bin 级别 CAS+synchronized），并发度高，读无锁。优先选 ConcurrentHashMap，除非需要 JDK 5 以下兼容。

---

> **面试官追问**：HashMap 多线程死循环怎么发生的？→ JDK 7 头插法扩容造成环形链表，put 时死循环；JDK 8 改为尾插法已修复，但多线程 put 仍有数据覆盖问题。

---

## 3. JVM（10 题）

### 3.1 JVM 内存模型？★★★

**答**：**线程共享**：堆（对象实例）、元空间 Metaspace（类信息、常量池，JDK 8 替代永久代，本地内存）。**线程私有**：虚拟机栈（栈帧=局部变量表+操作数栈+方法出口）、本地方法栈（Native 方法）、程序计数器。项目中打印 GC 日志可观察各区域变化。

---

### 3.2 类加载过程？双亲委派机制？★★★

**答**：加载→验证→准备（static 变量赋默认值）→解析（符号引用→直接引用）→初始化（static 变量赋初值+执行 static 块）。双亲委派：类加载器收到请求先委派给父加载器，父找不到才自己加载。好处：避免类重复加载、保护核心类（不能自定义 java.lang.String）。Tomcat 打破了这个机制实现应用隔离。

---

### 3.3 可达性分析算法怎么判断对象可回收？★★★

**答**：从 GC Roots（栈引用、静态变量、JNI 引用、活跃线程）出发，不可达的对象标记为可回收。`finalize()` 已废弃（JDK 9+），不推荐用。项目中要注意 `ThreadLocal.remove()` 防止 Entry 的 key 被回收后 value 泄漏。

**CampusHub**：`UserContext.java` — 请求结束调用 `remove()` 防止 ThreadLocal 内存泄漏

---

### 3.4 强软弱虚引用？★★

**答**：强引用（new）= 绝不回收；软引用（SoftReference）= OOM 前回收，适合缓存；弱引用（WeakReference）= 下次 GC 必回收，ThreadLocal 的 key 就是弱引用；虚引用（PhantomReference）= 无法获取对象，仅跟踪回收状态。

---

### 3.5 CMS 和 G1 区别？★★★

**答**：CMS 老年代并发标记清除，碎片化严重，STW 时间不可控。G1 将堆划分为大小相等的 Region，并发标记+筛选回收，可预测停顿（`-XX:MaxGCPauseMillis`）。JDK 9+ G1 为默认 GC。项目中没什么特殊配置，默认 G1 即可。

---

### 3.6 OOM 怎么排查？★★★

**答**：1) `-XX:+HeapDumpOnOutOfMemoryError` 生成 dump 文件；2) MAT/JProfiler 分析 dump，看大对象、泄漏链；3) `jstat -gc` 看各区域使用趋势；4) `jmap -histo` 查看对象直方图。常见原因：死循环创建对象、ThreadLocal 没 remove、连接没关、大文件一次性读内存。

---

### 3.7 JVM 常用调优参数？★★

**答**：`-Xms/Xmx`（堆初始/最大，建议相等防扩容抖动）、`-Xmn`（新生代大小）、`-XX:MetaspaceSize`、`-XX:+PrintGCDetails`、`-XX:+HeapDumpOnOutOfMemoryError`、`-XX:MaxGCPauseMillis`（G1 目标停顿时间）。项目部署 Docker 时在 `JAVA_OPTS` 中设置。

---

### 3.8 对象分配内存的过程？★★

**答**：1) 优先在栈上分配（逃逸分析+标量替换）；2) 大对象直接进老年代；3) TLAB（线程本地分配缓冲）避免竞争；4) Eden→Minor GC→Survivor（年龄加到 15 进老年代）。动态年龄判断：Survivor 中同龄对象超 50% 则直接晋升。

---

### 3.9 什么时候会触发 Full GC？★★

**答**：1) System.gc()（建议，不一定执行）；2) 老年代空间不足；3) 元空间不足；4) 空间分配担保失败；5) CMS GC Concurrent Mode Failure。频繁 Full GC 需要调优，先看是否有大对象不断进入老年代。

---

### 3.10 Java 程序启动慢可能是什么原因？★★

**答**：1) 类加载太多（Spring Boot 自动装配扫描大量类）；2) 初始化时执行了耗时操作（大量 DB 查询、网络调用）；3) 代理类生成（AOP 切面太多）；4) Metaspace 分配不够频繁 GC。优化：懒加载、exclude 不需要的自动配置、调大 Metaspace。

---

> **面试官追问**：类的 static 变量在哪个阶段赋初值？→ 准备阶段赋默认值（0/null），初始化阶段赋代码中的值。

---

## 4. 多线程（10 题）

### 4.1 线程生命周期？★★★

**答**：NEW → RUNNABLE → BLOCKED（等锁）/ WAITING（wait/join/park）/ TIMED_WAITING（sleep/timeout） → TERMINATED。`start()` 进入 RUNNABLE，`sleep()` 不释放锁，`wait()` 释放锁。项目中 SSE 推流用 `SseEmitter` 异步，不阻塞 Tomcat 工作线程。

---

### 4.2 synchronized vs ReentrantLock？★★★

**答**：synchronized 是 JVM 关键字，自动释放锁，不可中断，非公平，性能 JDK 6+ 优化后接近。ReentrantLock 是 API 实现（AQS），必须 finally 中 unlock，可中断、可超时、可公平/非公平、可绑定多个 Condition。简单场景用 synchronized，需要高级特性用 ReentrantLock。

---

### 4.3 volatile 有什么用？★★★

**答**：1) **可见性**：写 volatile 变量时 JVM 发 Lock 前缀指令，强制刷新到主存，读时直接读主存，跳过 CPU 缓存；2) **禁止指令重排序**（内存屏障）。经典应用：单例模式双重检查的 `private volatile static Singleton instance;`。不保证原子性（i++ 不是原子的，用 AtomicInteger）。

---

### 4.4 线程池 7 参数？★★★

**答**：corePoolSize、maximumPoolSize、keepAliveTime、TimeUnit、workQueue（BlockingQueue）、threadFactory、RejectedExecutionHandler。执行流程：核心线程→队列→最大线程→拒绝策略。4 种拒绝：AbortPolicy（抛异常，默认）、CallerRunsPolicy（调用者执行）、DiscardPolicy（丢弃）、DiscardOldestPolicy（丢最旧）。

---

### 4.5 AQS 是什么？★★

**答**：AbstractQueuedSynchronizer，JUC 框架基石。用 volatile int state + FIFO 等待队列（CLH 变体）实现。ReentrantLock 的 state 表示重入次数，CountDownLatch 的 state 表示还需要 countDown 的次数，Semaphore 的 state 表示许可证数。面试时手写一个简单锁可加分。

---

### 4.6 CAS 原理？ABA 问题？★★

**答**：Compare And Swap，比较内存值与期望值，相同则更新。底层是 CPU 的 `cmpxchg` 指令（原子操作）。ABA：线程 1 读 A→线程 2 改 A→B→A→线程 1 CAS 成功但中间已变。解决方案：AtomicStampedReference 加版本号。项目中没用 CAS 直接编程，但 ConcurrentHashMap、Redisson 底层都在用。

---

### 4.7 ThreadLocal 内存泄漏原因和解决？★★★

**答**：ThreadLocalMap 的 Entry 中 key 是弱引用（ThreadLocal），value 是强引用。ThreadLocal 被 GC 后 key 变 null，但 value 无法回收（Entry 还在 Thread 的 ThreadLocalMap 里）。解决：**每次使用完调用 `remove()`**，清空整个 Entry。项目中 `UserContext` 在 Filter 的 finally 里 remove。

**CampusHub**：`UserContext.java` — 请求结束 finally 块 `userIdHolder.remove()`

---

### 4.8 CountDownLatch 和 CyclicBarrier 区别？★★

**答**：CountDownLatch 一次性，等一个或多个线程完成（`countDown()`→减到 0 释放），不可复用。CyclicBarrier 可复用，等所有线程到达屏障点后一起继续（`await()`→计数+1）。Latch 是等别人，Barrier 是相互等。

---

### 4.9 CompletableFuture 用过吗？★★

**答**：JDK 8 异步编排工具。`supplyAsync()` 异步执行有返回值，`thenApply/thenAccept/thenRun` 串行，`thenCombine` 合并两个结果，`allOf/anyOf` 并行等全部/任一完成。项目中 RabbitMQ 消费是异步的，如果用 Java 做多数据源聚合查询可用 CompletableFuture 并行查询再合并。

---

### 4.10 怎么停止一个线程？★★

**答**：`stop()` 已废弃（释放锁但线程不确定，数据不一致）。正确方式：interrupt() + isInterrupted() 协作式停止。`sleep/wait` 状态收到 interrupt 抛 InterruptedException 并清除标记位。while 循环里检查中断标记，优雅退出。

---

> **面试官追问**：线程池提交了任务，核心线程数满了但最大线程没满，会创建新线程吗？→ 不会，先放队列，队列满了才创建新线程到最大线程数。

---

## 5. Spring Boot / Spring（12 题）

### 5.1 IoC 和 DI 是什么？★★★

**答**：IoC（控制反转）将对象创建和管理权交给 Spring 容器，DI（依赖注入）是 IoC 的实现方式。Spring 通过 `@Component/@Service` 注册 Bean，`@Autowired/@RequiredArgsConstructor` 注入。项目中所有 Service 都用 `@RequiredArgsConstructor`（Lombok 构造器注入），比字段注入更利于单元测试。

**CampusHub**：所有 ServiceImpl 顶部 `@RequiredArgsConstructor` + `private final XxxMapper`

---

### 5.2 AOP 原理？JDK 动态代理 vs CGLIB？★★★

**答**：JDK 动态代理：被代理类必须实现接口，基于 `InvocationHandler` 生成代理类，`Proxy.newProxyInstance()`。CGLIB：基于 ASM 生成被代理类的子类，不需要接口，但不能代理 final 类/method。Spring Boot 2.x 默认 CGLIB（3.x 同样）。项目中 `RateLimitAspect` 用 `@Around` 切面，底层 CGLIB。

**CampusHub**：`RateLimitAspect.java` — `@Aspect @Component @Around("@annotation(rateLimit)")`

---

### 5.3 Bean 生命周期？★★★

**答**：实例化→属性赋值→`BeanNameAware`→`BeanFactoryAware`→`BeanPostProcessor.before`→`@PostConstruct`→`InitializingBean.afterPropertiesSet`→`BeanPostProcessor.after`（AOP 在此）→使用→`@PreDestroy`→销毁。项目中的 Bean 基本都是单例，Spring 容器启动时创建。

---

### 5.4 循环依赖三级缓存怎么解决？★★★

**答**：Spring 用三级缓存：singletonObjects（一级，成品 Bean）、earlySingletonObjects（二级，提前暴露的未成品）、singletonFactories（三级，ObjectFactory 能生成代理对象）。流程：A 创建→提前暴露三级缓存→发现依赖 B→B 创建→B 依赖 A→从三级缓存拿 A 的引用→B 完成→A 拿到 B 完成。**构造器注入无法解决循环依赖**。

---

### 5.5 Spring 事务传播行为？★★★

**答**：REQUIRED（默认，有则加入无则新建）、REQUIRES_NEW（挂起当前，新建独立事务）、NESTED（嵌套，可回滚到 savepoint）、SUPPORTS（有则加入无则非事务）、NOT_SUPPORTED（挂起事务非事务运行）、NEVER（有事务抛异常）、MANDATORY（无事务抛异常）。项目中点赞 toggle 用默认 REQUIRED，先 INSERT 点赞再 UPDATE count。

**CampusHub**：`CourseServiceImpl.toggleLike()` — `@Transactional` 默认 REQUIRED

---

### 5.6 @Transactional 失效场景？★★★

**答**：1) 同类内部调用 `this.xxx()` 不走 AOP 代理；2) 非 public 方法（CGLIB 无法代理）；3) 异常被 catch 吞掉没抛出（或抛 checked exception 没指定 rollbackFor）；4) 数据库引擎不支持事务（MyISAM）；5) 多线程（子线程新开事务不在当前事务范围内）。

---

### 5.7 Spring Boot 自动配置原理？★★★

**答**：`@SpringBootApplication` 包含 `@EnableAutoConfiguration`→`@Import(AutoConfigurationImportSelector)`→读取 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`→按条件（@ConditionalOnClass/OnBean/OnProperty）自动装配 Bean。我项目引入 Redisson，Spring Boot 自动创建 RedissonClient，直接注入用。

**CampusHub**：`CampusApplication.java` — `@SpringBootApplication` 入口

---

### 5.8 过滤器 vs 拦截器？★★★

**答**：过滤器（Filter）：Servlet 规范，在 DispatcherServlet 之前，可修改 request/response，只拦截 HTTP 请求。拦截器（Interceptor）：Spring 机制，在 Controller 前后，可拿到 Handler（哪个 Controller 方法），不能修改 request。项目中 `JwtAuthenticationFilter` 在请求最外层解析 JWT，设置 UserContext。

**CampusHub**：`JwtAuthenticationFilter.java` + `SecurityConfig.java` 过滤器链配置

---

### 5.9 Spring MVC 执行流程？★★★

**答**：请求→DispatcherServlet→HandlerMapping（找 Controller）→HandlerAdapter（执行）→Controller（返回 ModelAndView 或 @ResponseBody JSON）→ViewResolver 渲染 或 HttpMessageConverter（Jackson 序列化 JSON）→响应。项目中前后端分离，Controller 返回 `R<T>` JSON 格式，走 `HttpMessageConverter` 序列化。

---

### 5.10 @Configuration 和 @Component 区别？★★

**答**：两者都能被 Spring 扫描为 Bean。但 `@Configuration` 标记的类是 Full 模式（CGLIB 增强），内部 `@Bean` 方法间调用会走容器代理保证单例；`@Component` 是 Lite 模式，内部 `@Bean` 方法间调用就是普通 Java 调用，多次 new。建议配置类用 `@Configuration`。

---

### 5.11 Spring Boot 启动流程？★★

**答**：`SpringApplication.run()` → 1) 创建 SpringApplication（推断 web 类型、加载 Initializer/Listener）；2) 准备 Environment；3) 创建并刷新 ApplicationContext → `invokeBeanFactoryPostProcessors`（扫描包注册 BeanDefinition）→注册 BeanPostProcessor→初始化事件广播→finishRefresh（启动内嵌 Tomcat）。项目启动类 `CampusApplication` 一行 `SpringApplication.run()`。

**CampusHub**：`CampusApplication.java` 启动类

---

### 5.12 @Value 注入原理？怎么读复杂配置？★★

**答**：`@Value("${xxx}")` 通过 `PropertySourcesPlaceholderConfigurer` 在 Bean 创建时解析占位符，从 Environment 中取值（优先级：命令行 > 环境变量 > application.yml）。复杂配置用 `@ConfigurationProperties(prefix="xxx")` + `@Component` 绑定到 POJO，类型安全+自动校验。

---

> **面试官追问**：@Transactional 是 Spring 的还是 JDK 的？→ Spring 的（`org.springframework.transaction.annotation.Transactional`），底层用了 ThreadLocal 绑定数据库连接来保证同一事务用同一连接。

---

## 6. MyBatis / MyBatis Plus（6 题）

### 6.1 `#{}` 和 `${}` 的区别？★★★

**答**：`#{}` 是预编译占位符（?），防 SQL 注入，MyBatis 会做类型转换和引号处理。`${}` 是字符串拼接，直接替换到 SQL 中，有注入风险。项目中所有参数绑定用 `#{}`，仅在动态表名/排序列（order by）等必须拼接的场景用 `${}`。MyBatis Plus 的 `eq()` 等方法底层走 `#{}`。

---

### 6.2 MyBatis 一级缓存和二级缓存？★★

**答**：一级缓存：SqlSession 级别，默认开启，同一 session 相同查询走缓存。二级缓存：Mapper 级别，跨 SqlSession 共享，需显式配置 `<cache/>`，POJO 必须序列化。项目中实际业务更依赖 Redis 缓存（数据实时性要求高），基本不开启 MyBatis 二级缓存。

---

### 6.3 MyBatis Plus 分页怎么实现的？★★★

**答**：配置 `MybatisPlusInterceptor` 添加 `PaginationInnerInterceptor`，调用 `Page<T>` 对象即可。原理：拦截器把 count 查询和分页查询拼接出来（各数据库方言不同，MySQL 就是 LIMIT）。项目中课程列表、公告列表都用分页，前端传 pageNum/pageSize。

**CampusHub**：`MybatisPlusConfig.java` — 注册分页拦截器

---

### 6.4 MyBatis 插件机制？★★

**答**：基于拦截器（Interceptor）和责任链模式，可拦截 4 个接口：Executor（执行器）、ParameterHandler（参数处理）、ResultSetHandler（结果集处理）、StatementHandler（SQL 语句处理）。分页插件就是拦截 Executor 的 query 方法。项目中自定义了分页拦截器和防全表更新插件。

**CampusHub**：`MybatisPlusConfig.java` 中的拦截器链配置

---

### 6.5 动态 SQL 怎么写？★★

**答**：MyBatis 用 `<if>/<choose>/<when>/<otherwise>/<foreach>/<where>/<trim>` 标签。MyBatis Plus 用 `QueryWrapper.lambda()` 的条件 `eq/like/between/orderBy` 链式调用，代码可读性好。项目中 90% 用 LambdaQueryWrapper 拼接条件，不用手写标签式动态 SQL。

---

### 6.6 MyBatis Plus 相比原生 MyBatis 优势在哪？★★

**答**：1) 内置 CRUD（BaseMapper），省掉简单 SQL；2) LambdaQueryWrapper 避免字段名硬编码，IDE 有提示+编译期校验；3) 自动分页插件；4) 逻辑删除 `@TableLogic`（deleted=1 的自动过滤）；5) 自动填充（`@TableField(fill=...)` createTime/updateTime）。项目中 `BaseEntity` 搭配 `@TableLogic` 实现软删除。

**CampusHub**：`BaseEntity.java` — `@TableLogic private Integer deleted;`

---

> **面试官追问**：MyBatis 的 Mapper 怎么和 XML 绑定的？→ namespace 对应 Mapper 全限定名，select id 对应方法名，parameterType/resultType 对应入参/返回类型。

---

## 7. MySQL（12 题）

### 7.1 B+ 树为什么适合做索引？★★★

**答**：1) 非叶子节点只存 key，一个节点能存更多 key，**树更矮，IO 更少**（一次 IO 读一页 16KB）；2) 所有叶子节点形成**有序双向链表**，范围查询直接遍历链表；3) 查询稳定性好（每次都到叶子节点）。相比 B 树，B 树数据分散在所有节点，范围查询要中序遍历，更慢。

---

### 7.2 聚簇索引 vs 非聚簇索引？★★★

**答**：聚簇索引叶子节点存**完整行数据**（InnoDB 用主键聚簇，只有一个）。非聚簇索引（二级索引）叶子节点存**索引列+主键**，需要回表查聚簇索引拿整行数据——这叫回表。项目中 `course.id` 是聚簇索引，`course.category` 是二级索引。

**CampusHub**：`CREATE INDEX idx_category ON course(category)` — 非聚簇索引

---

### 7.3 最左前缀原则？★★★

**答**：联合索引 `(A, B, C)` 相当于建了 `(A)`、`(A,B)`、`(A,B,C)` 三个索引。`WHERE A=1 AND B=2 AND C=3` 全命中；`WHERE B=2 AND C=3` 不命中（最左缺 A）；`WHERE A=1 AND C=3` 只走 A（跳过 B 后 C 不能走）。项目中 `course_like(course_id, user_id)` 联合唯一索引，必须两个条件一起查才全命中。

**CampusHub**：`UNIQUE KEY (course_id, user_id)` — 联合唯一索引

---

### 7.4 覆盖索引是什么？★★★

**答**：查询字段全在索引里，不需要回表。`SELECT course_id, user_id FROM course_like WHERE course_id=?` 如果 (course_id, user_id) 有联合索引，直接从索引页出结果，少一次回表 IO。EXPLAIN 的 Extra 列显示 `Using index` 就是覆盖索引。这是最经济的优化手段。

---

### 7.5 索引下推（ICP）是什么？★★

**答**：MySQL 5.6+ 优化。`WHERE name LIKE '张%' AND age=20`，联合索引 (name, age)。没有 ICP 时先根据 name 查所有行再回表按 age 过滤；有 ICP 时在索引层先过滤 age=20，减少回表次数。EXPLAIN Extra 显示 `Using index condition`。

---

### 7.6 事务隔离级别？★★★

**答**：读未提交（脏读）→读已提交（RC，不可重复读）→可重复读（RR，InnoDB 默认，幻读靠 gap lock 解决）→串行化。MVCC 解决快照读的幻读，临键锁解决当前读的幻读。项目中 Spring Boot 默认用 RR，`@Transactional` 中一致性读用快照读。

---

### 7.7 MVCC 原理？★★★

**答**：多版本并发控制，读写不冲突。每行有 `DB_TRX_ID`（最后一次修改的事务 ID）和 `DB_ROLL_PTR`（回滚指针指向 undo log）。ReadView 记录活跃事务列表，判断某版本是否可见：`trx_id < min_trx_id` 可见，`trx_id >= max_trx_id` 不可见。RC 每次快照读建 ReadView，RR 第一次建后复用。

---

### 7.8 行锁/间隙锁/临键锁区别？★★

**答**：行锁（Record Lock）：锁具体行；间隙锁（Gap Lock）：锁索引间隙，防幻读插入；临键锁（Next-Key Lock）= 行锁 + 间隙锁，InnoDB 默认（RR 级别下）。项目中课程评论的更新用 `SELECT ... FOR UPDATE` 会加临键锁，但 MyBatis Plus 的 `updateById` 直接用行锁。

---

### 7.9 慢查询怎么排查？★★★

**答**：1) `EXPLAIN` 看 type（all>index>range>ref>const）、key（命中索引）、rows（扫描行数）；2) 开启 `slow_query_log` + `long_query_time`；3) `SHOW PROFILES` 看各阶段耗时。优化三板斧：加索引→改 SQL（覆盖索引/减少回表）→数据归档/分表。

---

### 7.10 EXPLAIN 各字段含义？★★

**答**：type（访问类型，range/ref/const 是好，ALL 是全表扫要优化）、key（实际用的索引）、rows（估算扫描行数）、Extra（Using index=覆盖索引；Using filesort=额外排序，看是否命中索引排序；Using temporary=临时表，GROUP BY 没走索引）。项目中查课程列表一般要走 type=ref。

---

### 7.11 分库分表怎么搞？★★

**答**：垂直拆分（按业务模块拆库，如用户库/课程库）→水平拆分（单表数据大，按 userId hash 取模分 16 个库 32 张表，ShardingSphere 代理）。查询带上分片键能精确路由，不带分片键要全库广播。项目中数据量还没到分库分表阶段，但表结构设计上已预留扩展（userId 在所有业务表都有）。

---

### 7.12 一条 SQL 在 InnoDB 中怎么执行的？★★

**答**：连接器（权限验证）→分析器（词法/语法分析）→优化器（选索引/Join 顺序）→执行器→调用 InnoDB 引擎。InnoDB：查 Buffer Pool（内存缓存页），未命中→从磁盘读入 16KB 页→索引定位→回表（如需要）→返回。`SELECT * FROM course WHERE id=1` 走聚簇索引，最快情况一次 Buffer Pool 命中。

---

> **面试官追问**：为什么尽量不要用 `SELECT *`？→ 1) 不能走覆盖索引，多一次回表；2) 网络传输数据量大；3) 表结构变更（加列）可能影响结果。

---

## 8. Redis（8 题）

### 8.1 Redis 5 种基本数据结构？应用场景？★★★

**答**：String（分布式锁/计数器）、List（消息队列/最新列表）、Set（点赞去重/共同关注）、ZSet（排行榜/延时队列）、Hash（对象缓存/购物车）。项目中用 String 存 JWT 黑名单，用 ZSet 做延时队列。

---

### 8.2 RDB vs AOF 持久化？★★★

**答**：RDB 定时快照（bgsave fork 子进程写磁盘），恢复快但可能丢最近数据。AOF 记录每条写命令（appendfsync everysec），数据安全性高但恢复慢、文件大。生产推荐 **RDB + AOF 混合**（Redis 4.0+），项目中主要用 Redis 做缓存和限流，可接受重启丢失。

---

### 8.3 缓存穿透/击穿/雪崩？★★★

**答**：**穿透**（查不存在的数据穿透到 DB）→布隆过滤器或缓存空值；**击穿**（热点 key 过期大量请求到 DB）→互斥锁或"永不过期"（后台异步刷新）；**雪崩**（大量 key 同时过期）→过期时间加随机值、多级缓存、限流熔断。项目中 `@RateLimit` 注解在缓存失效时防 DB 被打爆。

**CampusHub**：`@RateLimit` — 限流兜底

---

### 8.4 Redisson 分布式锁原理？★★★

**答**：基于 Redis + Lua 脚本实现。加锁：`SET lockKey UUID:threadId NX EX 30` + hash 结构存重入次数。看门狗（WatchDog）：每 10 秒续期 30 秒，防止业务没执行完锁过期。释放锁：Lua 判断 value 是否匹配，防误删。Redisson 自带 `RLock` 实现了 `java.util.concurrent.locks.Lock` 接口。

**CampusHub**：`RateLimitAspect.java` 中 RedissonClient 实例 — 项目用 RRateLimiter 而非分布式锁

---

### 8.5 Redis 过期删除策略？★★

**答**：惰性删除（访问时检查过期）+ 定期删除（每 100ms 随机抽一批 key 检查删除）。内存满（maxmemory）后的淘汰策略：allkeys-lru（最近最少用）、volatile-lru（过期 key 中 LRU）、allkeys-random、noeviction（不淘汰，写报错）。项目中缓存用 LRU 淘汰策略。

---

### 8.6 缓存和数据库双写一致性？★★

**答**：方案：**先更新 DB 再删缓存**（Cache Aside 模式）。延迟双删：先删缓存→更新 DB→延迟再删缓存。最终一致性可以用 Canal 监听 binlog 异步更新缓存。项目中课程列表是先更新 DB，再手动调用缓存清除方法，容忍短暂不一致。

---

### 8.7 Redis 集群模式有哪些？★★

**答**：主从（读写分离，手动切换）→哨兵 Sentinel（自动故障转移，但只有一个 master 写）→Redis Cluster（多 master 多 slave，16384 hash slot 分片，自动 failover，去中心化 gossip 协议）。项目中单机 Redis 足够，生产部署用 Sentinel。

---

### 8.8 Pipeline 和事务？★★

**答**：Pipeline 打包多条命令一次发送，减少 RTT，非原子（中间一条失败不影响前后）。事务 MULTI/EXEC 原子执行一批命令，但无回滚（语法错全体不执行，运行时错继续执行）。Redis 事务用的少，替代方案用 Lua 脚本（原子+可逻辑判断）。

---

> **面试官追问**：Redis 为什么快？→ 1) 纯内存操作；2) 单线程避免上下文切换和锁竞争（IO 多路复用）；3) 高效数据结构（压缩列表、跳表）。

---

## 9. 消息队列 RabbitMQ（5 题）

### 9.1 为什么用消息队列？★★★

**答**：解耦（A 不直接调 B）、削峰（瞬时流量排队慢慢处理）、异步（非核心流程不阻塞主流程返回）。项目中知识库上传文档后，发送消息到 RabbitMQ，异步消费做 embedding+入库，上传接口立即返回，用户体验好。

**CampusHub**：`DocumentProcessConsumer.java` — 消费文档处理消息，异步做向量化

---

### 9.2 消息怎么保证可靠投递？★★★

**答**：生产端：confirm 回调（投递成功/失败应答）+ 失败重试。Broker 端：持久化（queue/message 都 durable）+ 镜像集群。消费端：手动 ACK（basicAck，处理完才确认），自动 ACK 可能丢消息。项目中 RabbitMQ 配置了 `publisher-confirm-type: correlated` 和 `acknowledge-mode: manual`。

**CampusHub**：`application.yml` 中 RabbitMQ 配置

---

### 9.3 消费幂等怎么保证？★★

**答**：消费者可能重复消费（网络抖动导致 ACK 失败重发）。方案：1) 数据库唯一索引（如消息 ID 设 unique，插入重复则忽略）；2) Redis setnx（消息 ID 缓存，消费前判断）；3) 业务状态机（已处理的订单不再处理）。项目中知识文档处理可以用 document_id + 处理状态做幂等判断。

---

### 9.4 死信队列怎么用？★★

**答**：消息变成死信的情况：1) 被拒绝（reject/nack且 requeue=false）；2) TTL 过期；3) 队列满了。死信会被路由到配置的 Dead Letter Exchange，进死信队列。用场景：延迟任务（消息设 TTL，过期后进死信被消费）+ 失败重试机制（重试 N 次后进死信，运维告警人工处理）。

---

### 9.5 RabbitMQ vs Kafka？★★

**答**：RabbitMQ：基于 Exchange 路由，低延迟，消费完可删除，适合事务消息/业务解耦（日均百万级）。Kafka：基于 Topic 分区，高吞吐顺序写入磁盘，消息持久化不删除，支持回溯重放，适合日志/流计算/大数据（日均亿级）。项目中选 RabbitMQ 因为业务消息量不大，路由灵活，部署简单。

---

> **面试官追问**：消息堆积了怎么办？→ 1) 增加消费者并发；2) 批量消费（prefetch count 调大）；3) 临时扩容消费者实例；4) 紧急方案：写脚本批量消费后直接入库跳过耗时逻辑。

---

## 10. 网络 & HTTP（5 题）

### 10.1 TCP 三次握手四次挥手？★★★

**答**：握手：Client→SYN→Server→SYN+ACK→Client→ACK（双方确认收发能力）。挥手：Client→FIN→Server→ACK→Server→FIN→Client→ACK（2MSL TIME_WAIT，确保 Server 收到最后 ACK）。项目中 WebSocket 升级前先走 TCP 握手+HTTP 升级协议（101 状态码）。

---

### 10.2 HTTP 1.1 vs HTTP 2 vs HTTP 3？★★

**答**：1.1：keep-alive 复用连接但队头阻塞、明文。2：二进制分帧、多路复用（同一连接并行请求）、头部压缩（HPACK）、Server Push。3（QUIC）：基于 UDP，0-RTT 握手、无队头阻塞、连接迁移。项目中 Nginx 配置 HTTP/2，前端 Vite 开发服务器默认 HTTP/1.1。

---

### 10.3 HTTPS 加密过程？★★★

**答**：1) Client Hello（支持的加密套件+随机数）→2) Server Hello（选定加密套件+证书+随机数+公钥）→3) Client 验证证书→4) 生成 Pre-Master Secret 用公钥加密发过去→5) 双方根据三个随机数生成会话密钥→6) 后续对称加密通信。项目中用 Nginx 做 SSL 终结。

---

### 10.4 Cookie vs Session vs JWT？★★★

**答**：Cookie 是浏览器存储机制，自动携带，4KB。Session 服务端存储会话，通过 Cookie 中的 SessionId 关联，服务端有状态。JWT 无状态，服务端不存，Header.Payload.Signature 三段 Base64，自包含用户信息。项目中用 JWT（access 30min + refresh 7d），Session 不适用前后端分离。

**CampusHub**：`JwtService.java` — 签发/验证 access token 和 refresh token

---

### 10.5 HTTP 状态码？★★★

**答**：200 OK、301 永久重定向、302 临时重定向、304 Not Modified（缓存）、400 Bad Request（参数错误）、401 Unauthorized（未认证）、403 Forbidden（无权限）、404 Not Found、500 Internal Server Error、502 Bad Gateway（网关错误）、503 Service Unavailable。项目中 `R.fail(ResultCode.UNAUTHORIZED, "请先登录")` 返回 401。

**CampusHub**：`ResultCode.java` 统一错误码枚举

---

> **面试官追问**：JWT 怎么防篡改？→ Signature 段用 Header 指定的算法（HS256/RS256）+ 服务端的 secret 对 Header+Payload 签名，客户端篡改 Payload 后签名不匹配，服务端验证失败。

---

## 11. 设计模式（5 题）

### 11.1 单例模式双重检查为什么要 volatile？★★★

**答**：`instance = new Singleton()` 分三步：1) 分配内存→2) 初始化对象→3) instance 指向内存地址。JVM 可能重排序为 1→3→2，另一个线程在步骤 2 前读到非 null 的单例（但未初始化完）就出问题了。`volatile` 禁止 2 和 3 重排序，保证其他线程读到的要么是 null 要么是完全初始化好的对象。

---

### 11.2 工厂模式 vs 抽象工厂？★★

**答**：工厂方法：一个工厂创建一种产品（如 `ChatServiceFactory` 创建不同模型适配器）。抽象工厂：一个工厂创建一组相关产品（如 Dao 层有 UserDao+MenuDao，用抽象工厂让 MySQL/PG 实现各一套）。Spring 的 `BeanFactory` 本质是抽象工厂（一个工厂创建所有 Bean）。

---

### 11.3 策略模式怎么消除 if-else？★★★

**答**：定义策略接口，每种情况一个实现类，Map 存对应关系，根据 key 取策略。如支付方式：`Map<PayType, PayStrategy>`，`payStrategyMap.get(type).pay()`，新增支付方式加个实现类即可，不用改原有 if-else。项目中 `FeedbackService` 的 like/dislike toggle 本质上是一个策略选择（点赞=插入，取消=删除）。

**CampusHub**：`FeedbackServiceImpl.java` — toggle 模式逻辑

---

### 11.4 代理模式：JDK vs CGLIB？★★★

**答**：JDK 动态代理：必须实现接口，`Proxy.newProxyInstance()` 生成 `$Proxy` 类，通过反射调用。CGLIB：继承目标类，ASM 生成子类字节码，性能更好。Spring 中，**有接口默认用 JDK，无接口用 CGLIB**；Spring Boot 默认全用 CGLIB（`spring.aop.proxy-target-class=true`）。项目中 AOP 切面用 CGLIB。

**CampusHub**：`RateLimitAspect` — Spring AOP，Spring Boot 默认 CGLIB

---

### 11.5 观察者模式在 Spring 中的应用？★★

**答**：Spring 事件机制：`ApplicationEventPublisher.publishEvent()` → `@EventListener` 或 `ApplicationListener` 监听。本质是观察者模式，发布者和监听者解耦。项目中暂未直接使用，但 Spring 容器启动事件（`ApplicationReadyEvent`）就是观察者。跨服务场景升级为消息队列（RabbitMQ）。

---

> **面试官追问**：Spring 中有哪些用到的设计模式？→ 单例（Bean）、工厂（BeanFactory）、代理（AOP）、模板方法（JdbcTemplate/RestTemplate）、观察者（事件监听）、适配器（HandlerAdapter）、策略（Resource 加载）。

---

## 12. 微服务 & 分布式（6 题）

### 12.1 CAP 定理？★★★

**答**：一致性（C）、可用性（A）、分区容错性（P）三者最多同时满足两个。P 是必选的（网络分区不可避免），所以实际是 CP 或 AP。Zookeeper 选 CP（leader 挂了选举期间不可用），Eureka/Nacos 选 AP（优先可用，最终一致）。项目中单体架构复用经验，但设计上 Controller→Service→Mapper 分层利于未来拆分。

---

### 12.2 分布式事务怎么解决？★★

**答**：Seata AT（2PC，自动回滚，性能损耗 30%）→ TCC（Try-Confirm-Cancel，侵入业务代码但性能好）→ 可靠消息最终一致（RocketMQ 事务消息，本地事务+MQ 同发，消费端幂等）。项目中涉及跨模块数据一致性（点赞+更新 count），在同一数据库用 `@Transactional` 本地事务即可。

---

### 12.3 服务注册与发现怎么理解？★★

**答**：每个服务启动时向注册中心（Nacos/Eureka/Consul）注册自己的 IP+端口，定期心跳续约。调用方从注册中心获取服务列表，通过负载均衡选择一个实例调用。项目中暂时单体应用不需要，但如果有 5 个 chat 实例，就需要注册中心统一管理。

---

### 12.4 限流算法有哪些？★★★

**答**：计数器（固定窗口，临界问题）→滑动窗口（精细，Redis ZSet 实现）→漏桶（固定速率，平滑输出）→令牌桶（固定速率放令牌，允许突发，Guava RateLimiter）。项目中用 Redisson 的 `RRateLimiter`（基于令牌桶），`@RateLimit(qps=5)` 限制 AI 聊天每秒 5 次。

**CampusHub**：`RateLimit.java` + `RateLimitAspect.java` — 令牌桶限流

---

### 12.5 负载均衡策略？★★

**答**：轮询（Round Robin）→加权轮询（性能好的机器多分）→最少连接→一致性哈希（同用户到同机器，适合有状态服务）→随机。项目中 Nginx 反向代理是最简单的轮询。微服务中 Ribbon/LoadBalancer 选策略：`@LoadBalanced` RestTemplate 默认轮询。

---

### 12.6 分布式 ID 怎么生成？★★

**答**：UUID（太长无序不适合主键）→数据库自增（单点瓶颈）→号段模式（Leaf-segment，每次取一段 ID）→雪花算法（Snowflake：1bit+41bit 毫秒+10bit 机器+12bit 序列号，趋势递增+高性能）。项目中 MySQL 自增 ID 足够，MyBatis Plus 的 `IdType.ASSIGN_ID` 默认用雪花算法。

---

> **面试官追问**：雪花算法时钟回拨怎么办？→ 1) 等待时钟追上；2) 用之前的最大序列号+1；3) 换用备用 workerId；4) Leaf 方案用号段模式规避。

---

## 13. Linux & DevOps（4 题）

### 13.1 常用 Linux 命令？★★★

**答**：top（CPU/内存进程排行）、df -h（磁盘）、free -h（内存）、netstat -tlnp（端口监听）、ps -ef | grep java（进程）、tail -f（实时日志）、grep -n "ERROR"（搜关键字）、find / -name "*.log"（找文件）、kill -9（强杀）或 kill -15（优雅停）。项目中 Docker 部署后 `docker logs -f campus-server` 看日志。

---

### 13.2 Docker 核心概念？★★★

**答**：镜像（Image）：只读模板（`Dockerfile` 构建）；容器（Container）：镜像的运行实例（隔离的进程空间）；仓库（Registry）：存储镜像。项目中写了 `Dockerfile`（多阶段构建，mvn package→jre 运行），`docker-compose.yml` 编排 MySQL/Redis/PG/RabbitMQ/MinIO 6 个容器。

**CampusHub**：`docker/Dockerfile` + `docker/docker-compose.yml`

---

### 13.3 CI/CD 流程？★★

**答**：Push 代码→GitHub Actions/GitLab CI 触发→1) 编译+单元测试→2) 构建 Docker 镜像→3) 推送到镜像仓库（Harbor/DockerHub）→4) SSH 到服务器 pull 镜像→5) docker-compose up -d 滚动更新。项目中暂未配置 CI/CD，手动 `mvn package` + docker compose 部署。

---

### 13.4 线上日志怎么排查？★★

**答**：1) `tail -f app.log` 实时看；2) `grep -n "ERROR" app.log` 搜错误；3) `grep -B5 -A5 "Exception"` 看异常上下文；4) Sentry/ELK 收集告警（配好 error 级别邮件通知）；5) 分布式链路追踪（SkyWalking 看调用链）。项目中日志写 `logs/campus.log`，日切分归档。

---

> **面试官追问**：Docker 和虚拟机区别？→ Docker 共享宿主机内核，秒级启动，MB 级；虚拟机独立 OS，分钟级启动，GB 级。Docker 是进程隔离，虚拟机是系统隔离。

---

## 14. 算法思路（3 题）

### 14.1 常用数据结构复杂度？★★★

| 结构 | 查找 | 插入 | 删除 | 场景 |
|------|------|------|------|------|
| 数组 | O(1) | O(n) | O(n) | 随机访问 |
| 链表 | O(n) | O(1) | O(1) | 频繁增删 |
| 哈希表 | O(1) | O(1) | O(1) | 快速查找 |
| 二叉搜索树 | O(logn) | O(logn) | O(logn) | 有序+动态 |
| 堆 | O(1) | O(logn) | O(logn) | TopK/优先级 |
| 跳表 | O(logn) | O(logn) | O(logn) | Redis ZSet |

---

### 14.2 排序算法对比？★★

| 算法 | 时间(平均) | 空间 | 稳定 | 特点 |
|------|-----------|------|------|------|
| 快排 | O(nlogn) | O(logn) | 否 | 实际最快，分区思想 |
| 归并 | O(nlogn) | O(n) | 是 | 外排序，分治 |
| 堆排 | O(nlogn) | O(1) | 否 | 原地，TopK 用最小堆 |
| 冒泡 | O(n²) | O(1) | 是 | 入门用 |

JDK 的 `Arrays.sort` 对基本类型用双轴快排，对象用 TimSort（归并变种+稳定性）。

---

### 14.3 算法刷题策略？★★

**答**：按标签刷，不按题号：数组/链表→栈/队列→哈希→二叉树遍历（前中后序/层序）→回溯（排列组合）→动规（背包/打家劫舍/最长子序列）→图（DFS/BFS/拓扑排序）。每道题先手写思路再写代码，10 分钟内想不出直接看题解。面试常考：LRU（HashMap+双向链表）、反转链表、三数之和、层序遍历、手写快排。

---

> **面试官追问**：HashMap 为什么链表转红黑树的阈值是 8？→ 泊松分布概率：链表长度=8 的概率不到千万分之一，此时转为红黑树避免退化到 O(n)。

---

## 15. CampusHub 项目实战（4 题）

### 15.1 项目最大亮点怎么介绍？★★★

**答**：（按面试 2 分钟自我陈述准备）
"我做了一个 AI 校园综合平台，技术栈是 Spring Boot 3 + Vue 3 + LangChain4j。核心亮点：1) 基于 RAG 的 AI 智能问答——本地上传校园知识文档，用 pgvector 做向量检索，DeepSeek 大模型生成回答，SSE 流式输出，用户体验接近 ChatGPT；2) 全站功能完整——课程学习、问答广场、私信(WebSocket 实时推送)、失物招领、公告系统、个人中心等 14 个模块；3) 工程化细节到位——@RateLimit 接口限流、RabbitMQ 异步文档处理、JWT 双 token 认证、MyBatis Plus 分页+逻辑删除、32 个单元测试。4) 全套 Docker Compose 一键部署，6 个中间件编排。"

---

### 15.2 遇到的最难 Bug？★★★

**答**："SSE 流式输出时，前端接收到的中文是乱码。排查过程：1) 先用 curl 直接调接口看原始响应——乱码，定位是后端问题；2) 检查 Content-Type 头——`text/event-stream` 没加 UTF-8；3) 加上 `produces = "text/event-stream;charset=UTF-8"` 后还是乱码；4) 最后发现是 Spring Boot 的 `SseEmitter` 需要用 `MediaType.TEXT_EVENT_STREAM`（含 UTF-8 的一个常量），改完就好了。教训：字符编码要从头到尾检查——SSE 的 UTF-8、HTTP 头的 charset、Jackson 序列化的编码。"

**CampusHub**：`ChatController.java` — SSE 接口

---

### 15.3 如果用户量翻了 100 倍，架构怎么演进？★★★

**答**："1) 读多写少 → Redis 热点缓存（课程列表、公告），Caffeine 本地缓存；2) 单库瓶颈 → MySQL 主从读写分离 + 分库分表（ShardingSphere）；3) 单体瓶颈 → 微服务拆分（chat/user/course 各独立部署，Feign 调用，Nacos 注册中心）；4) 搜索场景 → Elasticsearch 替代 MySQL LIKE 全文搜索；5) 并发请求 → Nginx + 网关（Spring Cloud Gateway）+ 限流降级（Sentinel）；6) AI 接口慢 → 异步化（请求入队→后台生成→WebSocket 推送结果）。每一步都有明确指标驱动：接口 P99 > 500ms 就加缓存，DB CPU > 70% 就读写分离。"

---

### 15.4 RAG Pipeline 怎么实现的？★★★

**答**："1) 文档上传→MinIO 对象存储→返回 URL；2) 异步处理：RabbitMQ 消息通知→消费者下载文档→LangChain4j DocumentParser 解析文本→按 chunk_size=500/overlap=50 切块→SentenceTransformersEmbeddingModel（all-MiniLM-L6-v2，384 维）做向量化→批量写入 pgvector；3) 用户提问→同样 Embedding 转向量→pgvector 的 cosine_distance 做 Top-K=5 近似检索→召回相关文档片段→拼接到 Prompt 模板（'你是一个校园助手，请根据以下参考资料回答...'）→DeepSeek API 流式生成→SSE 逐字推送到前端。相似度阈值设 0.0 保证召回，生产应调到 0.6+。"

**CampusHub**：`RagService.java` + `RetrieverService.java` + `DocumentProcessConsumer.java`

---

> **面试官追问**：你项目中的 RAG 和直接调 ChatGPT 有什么不同？→ ChatGPT 不知道校园内部信息，RAG 把知识库文档作为上下文注入 Prompt，让大模型在给定资料范围内回答，减少幻觉，且不泄露隐私。

---

## 附录：面试速查清单

面试前一天花 1 小时过一遍：

- [ ] HashMap put 流程能画出来
- [ ] ConcurrentHashMap get 为什么无锁（volatile）
- [ ] ThreadLocal 为什么内存泄漏 + remove 时机
- [ ] `@Transactional` 失效 5 种场景
- [ ] B+ 树和 B 树的区别
- [ ] 缓存穿透/击穿/雪崩 一字不漏
- [ ] CAP 定理选型
- [ ] RAG Pipeline 流程图
- [ ] 项目 2 分钟介绍
- [ ] 打开 IDEA 找到 15 个考点对应的源码位置

---

**总计 110 题，按板块分类：**

| 板块 | 题数 | 核心考点 |
|------|------|----------|
| Java 基础语法 | 10 | 面向对象、泛型、反射、异常 |
| Java 集合 | 10 | HashMap、ConcurrentHashMap、ArrayList |
| JVM | 10 | 内存模型、GC、类加载 |
| 多线程 | 10 | 线程池、AQS、ThreadLocal、volatile |
| Spring Boot/Spring | 12 | IoC/AOP、事务、Bean 生命周期、自动配置 |
| MyBatis/Plus | 6 | #{}/动态SQL/分页/插件 |
| MySQL | 12 | B+树/索引/事务/锁/MVCC/慢查询 |
| Redis | 8 | 数据结构/持久化/缓存问题/分布式锁 |
| RabbitMQ | 5 | 削峰解耦/可靠投递/幂等/死信 |
| 网络 & HTTP | 5 | TCP/HTTPS/JWT/状态码 |
| 设计模式 | 5 | 单例/策略/代理/工厂/观察者 |
| 微服务 & 分布式 | 6 | CAP/分布式事务/限流/ID 生成 |
| Linux & DevOps | 4 | Docker/命令/CI/CD/日志 |
| 算法思路 | 3 | 数据结构/排序/刷题策略 |
| CampusHub 实战 | 4 | 亮点/最难的bug/架构演进/RAG |
| **合计** | **110** | |
