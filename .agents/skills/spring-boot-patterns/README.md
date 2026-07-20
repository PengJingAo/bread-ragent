# Spring Boot 模式

**加载**: `view .claude/skills/spring-boot-patterns/SKILL.md`

---

## 功能说明

Spring Boot 应用的最佳实践和模式。涵盖项目结构、分层架构、DTO、异常处理、配置和测试。

---

## 使用场景

- "创建产品的 REST 控制器"
- "添加用户管理的服务层"
- "配置全局异常处理"
- "这个 Spring Boot 项目该怎么组织？"

---

## 示例

```
> view .claude/skills/spring-boot-patterns/SKILL.md
> "创建带 CRUD 端点的 UserController"
→ 生成遵循 REST 约定和正确状态码的控制器
```

---

## 涵盖模式

| 层 | 主题 |
|----|------|
| 控制器 | REST 约定、校验、状态码 |
| 服务 | 接口 + 实现、事务、映射器 |
| 仓库 | JPA 查询、派生方法、优化 |
| DTO | 请求/响应 record、MapStruct |
| 异常 | 自定义异常、全局处理器 |
| 配置 | 属性、Profile、校验 |
| 测试 | MockMvc、Mockito、Testcontainers |

---

## 注意事项

- 使用构造器注入（Lombok `@RequiredArgsConstructor`）
- 服务类默认 `@Transactional(readOnly = true)`
- 永远不要直接暴露实体 — 使用 DTO
- DTO 优先使用 record（Java 17+）
