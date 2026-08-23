# 架构、Controller、Service 与安全规范

## 证据样本

- `thinkchain-admin/.../system/SysConfigController.java`：旧系统 Controller，`BaseController`、权限/日志注解、Entity 入参、审计字段回填。
- `thinkchain-admin/.../chat/KnowledgeBaseController.java`、`KnowledgeDocumentController.java`、`ChatController.java`：新 AI Controller，构造器注入、请求 DTO、资源归属校验、限流和重复提交。
- `thinkchain-generator/.../GenController.java`、`thinkchain-quartz/.../SysJobController.java`：独立业务模块的 Controller、安全注解和业务前置校验。
- `thinkchain-chat/.../KnowledgeBaseServiceImpl.java`、`ConversationServiceImpl.java`、`DocumentProcessingServiceImpl.java`：新服务实现、事务、默认值、状态抢占和后台任务协调。
- `thinkchain-system/.../SysConfigServiceImpl.java`、`thinkchain-quartz/.../SysJobServiceImpl.java`、`thinkchain-generator/.../GenTableServiceImpl.java`：旧服务命名、Mapper 协作和事务用法。
- `thinkchain-framework/.../SecurityConfig.java`、`JwtAuthenticationTokenFilter.java` 与 `thinkchain-chat/.../ChatAccessService.java`：认证、授权和资源所有权完整链路。
- `VendorChatAdapter`/`VendorChatAdapterRegistry`、`AbstractQuartzJob`、`RepeatSubmitInterceptor`、各 Aspect、`AsyncFactory`：仓库已有模式。

## 强制规则

- `[用户]` 先搜索同模块和全仓库的已有实现，优先复用；只做完成任务的最小改动。
- `[用户]` 不过度封装，不为设计模式而设计模式。只有存在真实的多实现变化点、外部厂商边界或稳定的公共流程时才增加抽象。
- `[综合]` 新增 Spring Bean 使用 `private final` 加唯一构造器；唯一构造器不标 `@Autowired`。不要仅为统一风格批量改写旧字段注入类。
- `[综合]` Controller 负责 HTTP 映射、Bean Validation、可信身份绑定、资源访问校验、响应包装和轻量编排；复杂事务、状态机、级联删除和持久化默认值放入服务。
- `[用户]` 写接口不得把请求 Entity 直接无差别传给更新 Mapper。使用只包含允许字段的请求 DTO，或显式构造更新对象。
- `[仓库]` AI/RAG 资源的身份链必须是：JWT Filter 建立认证上下文 → `CurrentUserProvider` 读取当前用户 → `ChatAccessService` 覆盖请求身份并加载资源 → 比较 owner → 业务服务执行。
- `[综合]` 查询、详情、修改、删除、级联删除、RAG 引用和文件操作都必须在动作前校验资源归属；只校验“已登录”不等于校验资源所有权。
- `[仓库]` 高成本或可重复副作用接口沿用 `@RateLimiter` 和 `@RepeatSubmit`；系统管理接口沿用 `@PreAuthorize` 和 `@Log`。

## 项目惯例

### Controller

- `[仓库]` 列表接口常继承 `BaseController`，先 `startPage()`，再查询并 `getDataTable(list)`；非分页响应使用 `AjaxResult`。
- `[仓库]` 旧系统路由多为 `/system/...`、`/monitor/...`，AI 路由集中在 `/ai/...`；遵循所在功能的现有 URI 体系。
- `[仓库]` Bean Validation 使用 `@Validated` 或 `@Valid`；新 AI 请求字段常在 DTO 上使用 `@NotBlank`、`@Size`、`@Pattern` 等。
- `[仓库]` 新 AI Controller 使用 OpenAPI `@Tag`/`@Operation`；旧模块以 JavaDoc 和权限/日志注解为主。
- `[仓库]` 创建/更新通常把 `AjaxResult.success(...)` 或受影响行数的布尔值返回；旧 Controller 也使用 `toAjax(int)`。

### Service / ServiceImpl

- `[仓库]` 旧模块接口名多为 `I...Service`，实现放 `service.impl` 或同一 `service` 包；新 `thinkchain-chat` 使用无 `I` 前缀的 `...Service` 和 `service.impl`。新增代码遵循目标模块命名，不跨模块强行统一。
- `[仓库]` Service 方法通常直接返回领域对象、列表、影响行数或业务结果；Mapper CRUD 由实现类封装。
- `[仓库]` 多表写入、删除子表后删父表、数据库与调度器联动等操作在 Service 使用 `@Transactional`；需要时指定 `rollbackFor = Exception.class`。
- `[仓库]` ID、默认状态、计数初值等服务端默认值由 Service 统一设置；Controller 只绑定当前用户和允许的业务字段。
- `[仓库]` 跨资源级联删除由 `ChatResourceDeletionService` 一类协调服务完成，而非塞进单个元数据 Mapper。

### 模块依赖关系

仓库 POM 当前形成以下方向：

```text
thinkchain-common
├── thinkchain-system
│   └── thinkchain-framework
│       └── thinkchain-chat
├── thinkchain-generator
└── thinkchain-quartz

thinkchain-admin -> framework + chat + generator + quartz
```

- `[仓库]` `thinkchain-common` 是底层公共模型、注解、异常和工具，不依赖其他业务模块。
- `[仓库]` `thinkchain-system`、`generator`、`quartz` 依赖 `common`；`framework` 依赖 `system`；`chat` 直接依赖 `common` 与 `framework`；`admin` 是 Web/启动聚合层。
- `[综合]` 新代码放到拥有该业务概念的模块。低层模块不得为调用高层功能新增反向依赖；跨模块入口在 `admin` 装配，公共契约只有确实被多个模块复用时才下沉。

## 推荐规则

- `[综合]` Controller 中出现多步状态转换、重试、事务或外部系统协调时，提取到已有 Service 或一个职责清晰的协调服务；简单字段映射不再额外包装一层。
- `[综合]` 新业务异常使用 `ServiceException` 携带对外消息和可选业务/HTTP 码；技术细节写入 `detailMessage` 或服务端日志，不返回给客户端。
- `[综合]` 新资源优先采用 `requireXxx` 风格的“加载并校验”方法，避免 Controller 先查后重复查。
- `[综合]` 外部厂商或存储实现确有多个实现时，复用接口加注册表/条件 Bean；单实现、短期逻辑不预先建设扩展框架。

## 禁止事项

- `[用户]` 禁止为“看起来更架构化”把简单 CRUD 拆成多余的 Facade、Manager、Assembler、Factory。
- `[用户]` 禁止顺带大规模重命名旧 `IService`、统一括号风格或把所有字段注入改成构造器注入。
- `[综合]` 禁止把请求里的 `userId`、owner、角色、创建/更新时间、计数、处理状态、文件路径当成可信数据。
- `[仓库]` 禁止只依赖 `SecurityConfig.anyRequest().authenticated()` 保护用户资源；对象级授权必须继续执行。
- `[综合]` 禁止在 Controller 直接拼 SQL、直接操作 Mapper 以绕过已有 Service，或在无事务保护时完成多表级联写入。
- `[综合]` 禁止向公开模型列表或日志暴露 API Key、Base URL、Token、完整敏感内容。

## 需要根据上下文判断

- `BaseController`/`AjaxResult` 是当前主流 HTTP 约定；若既有接口已使用 `ResponseEntity`（如 AI 专用异常处理），保持其真实 HTTP 状态语义，不为统一而回退。
- 简单 CRUD 可直接返回 Entity；涉及输入安全时必须单独请求 DTO。是否增加响应 VO 取决于是否需要隐藏字段、聚合展示或稳定外部契约。
- `@PreAuthorize` 在旧系统接口广泛使用；AI 用户自有资源当前主要靠认证加 owner 校验。是否再增加权限字符串取决于该接口是否属于后台管理能力。
- 设计模式只在已有变化轴上使用：厂商适配器与注册表是 Adapter/Registry；Quartz 和重复提交拦截器是 Template Method；日志、数据源、数据范围和限流是 AOP；异步日志已有 Factory/Singleton。不要把这些模式推广到无关业务。

## 已发现的不一致

- 旧模块广泛使用字段 `@Autowired`，新 `thinkchain-chat` 大量使用唯一构造器注入。
- 旧 Controller 常直接接收 Entity，新 AI 更新接口使用字段白名单 DTO。
- 旧接口多为 `I...Service` 且 Java 接口方法显式写 `public`；新接口无 `I` 前缀并省略 `public`。
- 旧异常处理通常仅在 JSON 中返回业务码；`ChatExceptionHandler` 同步设置真实 HTTP 4xx/5xx。
- 注释既有 RuoYi 的 `@author` 风格，也有日期/版本标签和新的说明性中文 JavaDoc；部分配置类保留整块注释代码。

