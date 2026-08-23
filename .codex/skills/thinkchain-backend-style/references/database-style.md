# Mapper、MyBatis 与 SQL Migration 规范

## 证据样本

- `KnowledgeBaseMapper.java/xml`、`ChatConversationMapper.java/xml`、`KnowledgeDocumentMapper.xml`：新 chat 模块的经典 MyBatis XML。
- `SysUserMapper.java/xml`、`GenTableMapper.java/xml`：旧模块的关联 resultMap、动态条件、批量参数和数据范围。
- `MybatisPlusConfig.java`：MyBatis-Plus 插件已配置，但业务 Mapper 未发现 `BaseMapper`、`ServiceImpl`、Wrapper 或表映射注解的实际使用。
- `BaseController` 与 `PageUtils`：Controller 侧 PageHelper 分页。
- `ConversationServiceImpl`、`SysJobServiceImpl`、`GenTableServiceImpl`：多写操作的事务边界。
- `sql/chat_phase1.sql`、`rag_phase2_mysql.sql`、`rag_phase2_pgvector.sql`、`backend_async_rag_consistency_mysql.sql`、`backend_security_acceptance_mysql.sql`：基线建表、MySQL 增量与 PostgreSQL pgvector 脚本。

## 强制规则

- `[仓库]` Mapper 接口全限定名必须与 XML `namespace` 一致；方法名、statement `id`、参数名/类型、返回值和 resultMap 保持同步。
- `[仓库]` 复杂或动态 SQL 放 Mapper XML，显式维护 `resultMap` 和可复用列清单 `<sql>`；不要在 Controller 拼 SQL。
- `[综合]` 所有外部值使用 `#{}`。禁止把客户端输入放进 `${}`；`${params.dataScope}` 仅允许接收 `DataScopeAspect` 清理后生成的片段，生成器 `${sql}` 仅限已受控的管理功能链路。
- `[综合]` 多表写入、父子级联删除、数据库与调度器/向量库协调必须明确事务边界和失败语义。
- `[综合]` 修改表结构时同步检查 Entity/DTO、Mapper interface/XML、查询列、resultMap、状态机、测试和目标数据库脚本。
- `[用户]` SQL 和 Mapper 改动后必须运行相关模块测试与构建；能连接数据库时再执行专用验收脚本，不能连接时明确说明未实际迁移。

## 项目惯例

### Mapper / Mapper XML

- `[仓库]` Mapper 是普通接口，XML 位于 `src/main/resources/mapper/<domain>/`；CRUD 方法常返回对象、列表或受影响行数。
- `[仓库]` 单参数可直接使用 `#{id}` 等名称；多参数在旧 Mapper 中使用 `@Param`，批量数组常以 `array` 配合 `<foreach>`。
- `[仓库]` 查询列常抽为 `select...Vo` SQL 片段，结果使用显式 `<id>`/`<result>`；关联对象使用 `<association>`，集合使用 `<collection>`。
- `[仓库]` 列表查询使用 `<where>` 和非空 `<if>`，更新时间通常由 XML 的 `sysdate()` 写入，更新语句用 `<set>` 处理可选字段。
- `[仓库]` 分页主要由 `BaseController.startPage()`/PageHelper 在查询前启动，不在 Mapper 方法上新增分页框架参数。
- `[仓库]` 数据范围通过 `@DataScope` 写入 `BaseEntity.params.dataScope`，XML 使用受控 `${params.dataScope}`。

### MyBatis 使用方式

- `[仓库]` 当前实际业务代码是经典 MyBatis Mapper + XML，不是 MyBatis-Plus ActiveRecord/通用 Service 风格。
- `[综合]` 新增相邻 CRUD 优先延续 Mapper/XML。不要仅因 `MybatisPlusConfig` 存在就混入 `BaseMapper`、Wrapper 或第二套分页方式。
- `[仓库]` MyBatis-Plus 现有配置提供分页、乐观锁和全表更新/删除阻断插件；在没有实际调用证据前，不把这些配置等同于已采用 Plus 编码范式。

### SQL migration

- `[仓库]` SQL 脚本集中在根 `sql/`，当前没有 Flyway/Liquibase 版本命名约定。
- `[仓库]` MySQL 表使用小写 snake_case、`utf8mb4_unicode_ci`、表/列中文 comment、显式主键和查询路径索引。
- `[仓库]` PostgreSQL pgvector 使用独立脚本、`create ... if not exists`、列 comment、向量唯一索引/HNSW，并限制 Supabase 前端角色权限。
- `[仓库]` 较新的增量脚本通过 information_schema、过程或 `if not exists` 做可重复执行；文件头用中文说明目的和幂等性。
- `[综合]` 新增升级脚本采用增量、可审查、尽量幂等的写法，并按 MySQL/PostgreSQL 分文件；不要把基线重建脚本当线上升级脚本。

## 推荐规则

- `[综合]` 索引应对应真实的 owner/status/时间排序、外键关联或向量检索路径；唯一业务不变量用唯一索引兜底。
- `[综合]` 状态抢占用带旧状态条件的原子 `update ... where status in (...)`，通过受影响行数判断是否取得任务执行权。
- `[综合]` 先删除子记录再删除父记录，并在同一数据库事务中执行；涉及 MySQL、pgvector 和物理文件时明确无法跨资源原子提交的补偿/顺序策略。
- `[综合]` SQL 注释解释用途、兼容性、幂等性和危险操作；普通列定义保持简洁。

## 禁止事项

- `[综合]` 禁止把未经白名单校验的排序、列名、表名、where 片段或 DDL 通过 `${}` 拼接。
- `[综合]` 禁止 Mapper XML 更新服务端维护字段，除非调用方明确构造了可信值且该方法职责就是状态/审计更新。
- `[综合]` 禁止在已有 Mapper/XML 功能旁重复增加注解 SQL 或 MyBatis-Plus Wrapper 实现同一查询。
- `[综合]` 禁止在增量 migration 中无迁移方案地 `drop table`、清空数据或重建生产表。
- `[综合]` 禁止把 PostgreSQL pgvector 语法混入 MySQL 脚本，或假定两个数据源共享事务。

## 需要根据上下文判断

- `rag_phase2_mysql.sql` 包含多个 `drop table`，更像阶段性基线重建脚本；只有明确要求重建环境时才执行。后续升级优先新增增量脚本。
- 数据库时间当前多使用 `sysdate()`；同一表的新增/更新应保持一致，除非任务明确迁移时间策略。
- 外键约束在现有 AI 表脚本中并不普遍，代码通过事务和删除顺序维持一致性。新增外键需评估既有数据、双数据源和删除流程，不能作为通用强制项。
- Mapper 方法是否写 JavaDoc 取决于语义是否显然；复杂状态更新、受控动态 SQL和非标准返回值应说明。

## 已发现的不一致

- 已配置 MyBatis-Plus 插件，但抽样和全仓搜索未发现业务类实际采用 Plus Mapper/Service API。
- 旧 XML 大量使用 Tab 和大写 `AND/SELECT`，新 chat XML 使用空格和小写 SQL；修改时遵循文件局部格式。
- SQL 同时包含可破坏的阶段基线脚本和可重复执行的增量脚本，文件名没有统一版本号。
- MySQL 元数据与 PostgreSQL 向量数据分属两个数据源，应用层一致性无法依赖单一事务。

