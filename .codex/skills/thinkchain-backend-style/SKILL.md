---
name: thinkchain-backend-style
description: Implement, modify, refactor, or review Java/Spring backend code in the ThinkChain repository using conventions derived from its existing modules. Use for controllers, services, MyBatis mappers, DTO/VO/entities, configuration, exceptions, security, tests, and SQL migrations; do not use as a generic Java style guide outside this repository.
---

# ThinkChain Backend Style

以当前仓库实现为首要依据，在保持相邻代码兼容的同时遵守本项目已明确的安全、可测试性和最小改动约束。

## 执行流程

1. 先读目标类、同包 2 至 3 个相邻类、对应接口/实现、Mapper XML、测试和配置；搜索已有同类能力。
2. 确认改动所在模块及其依赖方向，不通过反向依赖绕过模块边界。
3. 区分请求字段、领域字段和服务端维护字段；沿 JWT/权限注解、当前用户注入、资源归属校验追踪安全边界。
4. 选择与目标区域兼容的最小实现。旧代码的格式可以就近保持，但新增依赖注入、安全边界和测试不得复制已知旧问题。
5. 只为真实变化增加抽象；优先复用已有 `BaseController`、`AjaxResult`、`ServiceException`、Mapper/XML、校验器、访问校验服务、注册表或适配器。
6. 添加或更新覆盖关键分支和安全不变量的测试。测试通过正式构造器或 Spring 正常装配点适配生产类。
7. 运行受影响模块测试和编译；跨模块公共契约改动运行根聚合构建。报告实际执行的命令、结果和未验证项。

## 强制规则

- 采用最小改动，不做无关重构，不过度封装，不为使用模式而使用模式。
- 正式代码不得为测试增加专用构造器、方法、逻辑、开关或可见性。
- Spring Bean 只有一个构造器时不加 `@Autowired`；新增或重写依赖优先使用 `private final` 加唯一构造器。
- Controller 不接收或透传客户端可伪造的服务端维护字段。身份取自认证上下文；读取、修改、删除和引用资源前校验资源归属。
- 参数使用 `#{}` 绑定；不得把原始客户端文本放入 MyBatis `${}`。现有 `${params.dataScope}` 只允许沿已清理和服务端生成的数据权限链路使用。
- 核心状态转换、事务边界、安全原因和非显然兼容逻辑使用简洁中文注释；禁止逐行解释和失效代码块。
- 修改后执行匹配范围的测试和 Maven 构建验证。

## 规则来源与冲突处理

- `[仓库]` 表示多个真实实现支持的现有事实。
- `[用户]` 表示项目所有者明确要求，即使旧代码存在反例也应遵守。
- `[综合]` 表示以仓库事实为基础、结合用户要求得出的新代码决策。
- 优先级：安全/数据正确性与本文件强制规则 > 用户明确要求 > 目标模块稳定约定 > 推荐规则。
- 不批量“统一”旧模块。若本次任务正好修改冲突区域，只修复任务涉及的范围并补测试。

## 按任务读取详细规范

- Controller、Service、模块关系、安全边界或设计模式：读 [references/architecture.md](references/architecture.md)。
- Java 格式、DTO/VO/Entity、配置类、异常和中文注释：读 [references/java-style.md](references/java-style.md)。
- Mapper、Mapper XML、MyBatis、事务或 SQL migration：读 [references/database-style.md](references/database-style.md)。
- 编写、修改或评审测试，以及选择验证命令：读 [references/testing-style.md](references/testing-style.md)。

## 完成前检查

- 是否复用了已有能力并限制了改动范围？
- 是否阻止客户端覆盖身份、归属、审计、统计、状态和路径字段？
- 是否保持 Mapper 接口、XML namespace、statement ID、参数和 resultMap 一致？
- 是否只在有真实变化点时引入接口、适配器、注册表或协调服务？
- 是否没有生产代码测试钩子和唯一构造器上的 `@Autowired`？
- 是否有针对关键行为而非实现细节的测试，并完成相应构建？

