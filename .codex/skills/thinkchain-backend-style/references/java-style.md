# Java、模型、配置、异常与注释规范

## 证据样本

- `KnowledgeBaseCreateRequest`、`KnowledgeBaseUpdateRequest`、`KnowledgeDocumentUpdateRequest`、`ChatRequest`：请求字段白名单、校验和只读身份。
- `KnowledgeBase`、`ChatConversation`、`SysUser`、`BaseEntity`：领域 Entity、审计字段、验证与序列化注解。
- `ChatResponse`、`ModelSummary`、`RouterVo`：普通响应 DTO、record 公共摘要和 VO。
- `DocumentProcessingConfig`、`VectorDataSourceConfig`、`RedisConfig`、`SecurityConfig`：配置 Bean、条件配置与新旧注入风格。
- `GlobalExceptionHandler`、`ChatExceptionHandler`、`ServiceException`：全局和 AI 专用异常响应。
- `ConversationServiceImpl`、`DocumentProcessingServiceImpl`、`ModelRegistry`：有价值中文注释的主要样本。

## 强制规则

- `[用户]` Spring Bean 只有一个构造器时不写 `@Autowired`；生产依赖使用正式唯一构造器，测试按该构造器组装。
- `[用户]` Controller 不允许客户端修改服务端维护字段。请求 DTO 仅声明客户端可编辑字段；身份字段可用 `@JsonProperty(READ_ONLY)` 作为第二道防线，但仍必须由服务端覆盖。
- `[综合]` 密码、密钥和内部错误等敏感字段必须使用响应摘要、`WRITE_ONLY`/忽略序列化或显式映射避免泄露。
- `[仓库]` 业务可预期错误抛 `ServiceException`；未知异常由全局处理器记录堆栈并返回通用消息。
- `[用户]` 核心实现添加解释“为什么、边界、状态变化、恢复策略”的中文注释，禁止逐行复述和大段注释掉的旧实现。

## 项目惯例

### DTO / VO / Entity

- `[仓库]` 新 AI 代码使用无 Lombok 的普通 JavaBean，显式 getter/setter；简单不可变公开摘要可以使用 Java 21 `record`。
- `[仓库]` 持久化实体放 `domain`，请求/响应对象放 `dto`，页面展示模型放 `domain.vo`；新增类遵循目标模块的目录。
- `[仓库]` 多个持久化 Entity 继承 `BaseEntity` 获取审计时间、创建/更新人、remark 和查询 params；是否继承取决于表是否实际使用这些字段。
- `[仓库]` 请求校验消息使用中文，并设置与数据库列或业务约束一致的长度、枚举范围和数值范围。
- `[仓库]` 旧系统 Entity 同时承担查询、导入导出和部分请求模型职责；新代码在存在越权/批量赋值风险时拆分请求 DTO。

### Spring 配置类

- `[仓库]` 配置类使用 `@Configuration`，Bean 用清晰方法名；多同类型 Bean 使用 `@Bean(name=...)` 和 `@Qualifier`。
- `[仓库]` 外部开关使用 `@ConditionalOnProperty`/`@ConditionalOnBean`，类型化配置使用 `@ConfigurationProperties` 或 `@EnableConfigurationProperties`。
- `[仓库]` 线程池、数据源、序列化器等基础设施在配置类集中创建，业务代码按接口或限定名注入。
- `[综合]` Bean 方法参数优先于配置类字段注入；不要添加只为单元测试替换对象的 Bean 或构造器。

### 异常处理

- `[仓库]` `GlobalExceptionHandler` 统一处理权限、请求方法、绑定校验、JSON、上传大小、运行时和系统异常。
- `[仓库]` `ChatExceptionHandler` 仅作用于 chat Controller 包，并以最高优先级把 `ServiceException` 合法 4xx/5xx code 映射到 HTTP 状态。
- `[综合]` 已有异常体系能表达问题时不新建异常层次；只有需要稳定的独立捕获语义时才新增类型。
- `[综合]` 日志记录 URI、资源 ID、状态和经过清洗的错误信息；对外消息不包含堆栈、SQL、文件绝对路径、密钥或厂商原始敏感响应。

### 格式与命名

- `[仓库]` 包名全小写，类名 PascalCase，方法/字段 camelCase，常量大写下划线；业务类名通常包含领域名和角色后缀。
- `[仓库]` 仓库有两套大括号风格：旧模块常换行大括号，新 AI 代码常同行大括号。修改现有文件时遵循文件局部风格；新 chat 类遵循同行大括号。
- `[仓库]` import 通常按 JDK、第三方、项目包分组，但旧文件顺序并不一致。修改时让新增 import 清晰且无未使用项，不进行全仓机械排序。

## 推荐规则

- `[综合]` 创建与更新 DTO 分开，除非二者允许字段和校验完全相同且共享确实减少维护风险。
- `[综合]` 更新时构造只含主键、可信 owner 和允许变更字段的新 Entity，避免把已加载对象或请求对象整体复制后误写状态。
- `[综合]` 状态、模式等稳定闭集优先复用项目已有枚举；若表和 API 已普遍使用字符串，局部修改保持兼容并通过校验限制取值，不顺带全量枚举化。
- `[综合]` 注释重点放在资源归属、幂等抢占、事务顺序、降级/重试、敏感数据清洗和非直观兼容原因。

## 禁止事项

- `[用户]` 禁止生产类出现 `forTest`、测试专用构造器、测试分支、测试开关、仅测试可见方法。
- `[综合]` 禁止使用通用 Bean 拷贝把外部请求批量覆盖到持久化 Entity。
- `[综合]` 禁止公开返回包含 API Key、密码、内部 Base URL、物理文件路径或未清洗原始模型请求/响应的对象。
- `[用户]` 禁止“获取名称”“设置字段”这类逐行废话注释；禁止长期保留整个旧类实现为注释。
- `[综合]` 禁止仅为了减少 getter/setter 引入仓库未使用的新代码生成依赖。

## 需要根据上下文判断

- Entity 上的校验注解是旧代码事实，但安全敏感写接口仍需请求 DTO；普通内部查询对象可继续使用 Entity。
- `record` 适合不可变输出和值对象，不用于需要 MyBatis 逐属性写入、继承 `BaseEntity` 或框架要求无参构造的 Entity。
- JavaDoc 的作者/日期/版本标签在仓库中不一致。新增代码无需机械添加日期和版本；对公共契约、核心类和非显然流程写简洁说明即可。
- 旧文件的换行大括号和字段注入可保持局部一致；当任务实际触及依赖装配时，优先改善为唯一构造器，但不要扩大修改面。

## 已发现的不一致

- `ChatRequest.userId` 仍有历史注释“未接入登录时允许为空”，而当前安全链会从 JWT 强制覆盖，注释语义有漂移。
- `AiModelConfig` 和 `ScheduleConfig` 几乎全部是注释代码，与“不保留失效实现”的期望不一致。
- 老 Entity 兼作请求/查询/导出模型，新 AI 代码已转向专用请求 DTO。
- JavaDoc 同时存在 `@author`、`@Author/@Date/@Version` 和无元数据的简洁中文说明。

