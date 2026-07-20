# API 契约审查技能

> 审计 REST API 的 HTTP 语义、版本控制和一致性

## 功能说明

审查 REST API 设计的以下方面：
- HTTP 动词正确性（GET vs POST vs PUT vs PATCH）
- API 版本控制策略
- 请求/响应结构（DTO vs 实体）
- 状态码使用（不应出现 200 携带错误体）
- 向后兼容性问题

## 使用场景

- "审查这个 API" / "检查 REST 端点"
- 发布 API 变更之前
- 审查 Controller 的 PR
- 检查 API 是否遵循 REST 最佳实践

## 核心概念

### 审计 vs 模板

| spring-boot-patterns | api-contract-review |
|---------------------|---------------------|
| 如何编写控制器 | 审查已有 API |
| 模板和示例 | 检查清单和反模式 |
| 创建新代码 | 审计已有代码 |

### 常见问题捕获

| 问题 | 示例 |
|------|------|
| 动词使用错误 | 搜索使用 POST 而非 GET |
| 无版本控制 | `/users` 而非 `/v1/users` |
| 实体泄露 | 直接返回 JPA 实体 |
| 200 携带错误 | HTTP 200 返回 `{"status": "error"}` |
| 破坏性变更 | 请求中新增必填字段 |

## 使用示例

```
你: 审查 UserController 中的 API

Claude: [检查 HTTP 动词使用]
        [验证版本控制]
        [查找实体泄露]
        [审查错误处理]
        [识别破坏性变更]
```

## 检查内容

1. **HTTP 语义** - 操作对应的动词是否正确
2. **URL 设计** - 版本控制、命名规范
3. **请求处理** - 校验、DTO
4. **响应设计** - DTO、分页、一致性
5. **错误处理** - 状态码、错误格式
6. **兼容性** - 破坏性 vs 非破坏性变更

## 相关技能

- `spring-boot-patterns` - 编写控制器的模板（本技能用于审查）
- `security-audit` - API 的安全方面
- `java-code-review` - 通用代码审查（本技能专注于 API）

## 参考资料

- [REST API 设计最佳实践](https://restfulapi.net/)
- [HTTP 状态码](https://httpstatuses.com/)
- [API 版本控制](https://www.baeldung.com/rest-versioning)
