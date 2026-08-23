# 测试与构建验证规范

## 证据样本

- `AiChatServiceTest`：JUnit 5、Mockito、AssertJ、手工 fixture、通过正式构造器组装服务。
- `ChatAccessServiceTest`：当前用户覆盖、404/403 和会话文档归属等安全不变量。
- `DocumentProcessingServiceImplTest`：同步 Executor 替身、状态抢占和处理流水线。
- `DashScopeEmbeddingClientTest`：`MockRestServiceServer` 验证重试、响应维度和真实 HTTP 构建点。
- `DashScopeBeanConstructionTest`：用 Spring 上下文验证唯一正式构造器可装配。
- `KnowledgeDocumentControllerTest`：standalone MockMvc、恶意额外字段、服务端维护字段不可修改。
- `ChatExceptionHandlerTest`：真实 HTTP 状态与 JSON 业务码一致。

## 强制规则

- `[用户]` 测试适配正式代码，正式代码不适配测试。禁止增加测试专用构造器、setter、分支、开关、静态后门或降低可见性。
- `[用户]` 修改后必须运行测试和构建验证；失败必须报告命令、失败点和是否可能由环境或既有改动导致。
- `[综合]` 安全相关变更至少覆盖可信身份覆盖、越权资源拒绝、缺失资源、服务端维护字段不可批量赋值等适用不变量。
- `[综合]` 异步、重试、状态机和多步事务至少覆盖成功路径与一个关键拒绝/失败路径。
- `[综合]` 测试不得调用真实厂商接口、使用真实密钥或依赖个人机器绝对路径。

## 项目惯例

- `[仓库]` 当前新增测试以 JUnit Jupiter、Mockito、AssertJ 为主；Controller 使用 standalone `MockMvc`，HTTP 客户端使用 `MockRestServiceServer`。
- `[仓库]` 纯单元测试直接调用正式构造器并传入 mock；依赖很多时使用局部 `fixture()` 和测试内 record 聚合相关对象。
- `[仓库]` 方法名使用英文行为描述，如 `prepareAlwaysOverridesClientSuppliedUserId`，断言聚焦可观察结果和关键协作。
- `[仓库]` 对 Spring 装配疑问使用最小 `AnnotationConfigApplicationContext` 验证实际 Bean 构造，而不是给生产类补无参/测试构造器。
- `[仓库]` 对异步服务可传 `Runnable::run` 作为正式 `Executor` 依赖的测试实现，使测试确定性执行。

## 推荐规则

- `[综合]` 优先写不加载完整 Spring Boot 的快速单元测试；只有需要验证安全过滤链、条件 Bean、配置绑定或模块装配时使用更大范围集成测试。
- `[综合]` Controller 输入安全测试应提交恶意或多余的服务端字段，并捕获传给 Service 的对象验证字段未被写入。
- `[综合]` Mapper/XML 改动若缺少数据库测试环境，至少运行模块测试和编译，并人工核对 namespace、statement ID、参数和 resultMap；有测试数据库时补真实 SQL 验证。
- `[综合]` 修复缺陷时先用失败测试表达可观察问题，再做最小实现；不要锁死私有方法或日志文本等实现细节。

## 验证范围

根据改动选择最小但充分的命令：

```text
# 单模块及其依赖
mvn -pl thinkchain-chat -am test
mvn -pl thinkchain-admin -am test

# 仅编译/打包且仍运行测试
mvn -pl <module> -am package

# 公共契约、父 POM 或跨模块改动
mvn test
mvn package
```

- `[综合]` 不要默认使用 `-DskipTests` 作为完成验证。若只为区分编译问题临时跳过测试，最终仍需补跑测试。
- `[综合]` 运行命令前检查工作树，避免把用户未提交配置或测试误判为本次变更。

## 禁止事项

- `[用户]` 禁止为了 mock 方便修改生产构造器或添加无参构造器。
- `[综合]` 禁止只断言“方法被调用”而不检查关键业务结果、安全字段或状态变化。
- `[综合]` 禁止测试真实 API Key、真实外部服务和不可恢复的数据库脚本。
- `[综合]` 禁止因完整构建耗时就声称未执行的测试已经通过。
- `[综合]` 禁止修改或删除用户已有测试来让构建变绿，除非测试本身确实与明确的新需求冲突且变更在任务范围内。

## 需要根据上下文判断

- 小 getter/setter、纯 DTO 可不单独测试；校验注解、序列化权限或安全字段契约发生变化时应测试。
- 纯格式/注释改动可只做轻量编译或相关校验；行为、依赖、SQL、配置和公共 API 改动需要更完整测试。
- 外部依赖导致构建无法联网时，记录为环境限制；能使用本地缓存完成的模块测试仍应继续执行。
- SQL migration 是否实库执行取决于是否有明确、安全、可恢复的测试数据库。不得在身份不明的数据库上试跑破坏性脚本。

## 已发现的不一致

- 测试主要集中在 `thinkchain-chat` 与少量 `thinkchain-admin` chat Controller；system/framework/generator/quartz 目前未见同等覆盖。
- 新测试普遍按唯一正式构造器组装对象，而大量旧生产 Bean 仍使用字段注入，导致旧模块单元测试隔离成本更高。
- 工作树中的 `thinkchain-chat/src/test/` 当前为未跟踪内容；执行或修改测试时必须先确认归属，不能擅自覆盖。

