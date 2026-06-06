# Java 代码审查

**加载**: `view .claude/skills/java-code-review/SKILL.md`

---

## 功能说明

Java 项目的系统性代码审查清单。涵盖空值安全、异常处理、集合、并发、惯用写法、资源管理、API 设计和性能。

---

## 使用场景

- "审查这个类"
- "检查这个 PR 的问题"
- "代码审查 PluginManager 的变更"
- "这段代码有什么问题？"

---

## 示例

```
> view .claude/skills/java-code-review/SKILL.md
> "审查 src/main/java/org/example/UserService.java 的变更"
→ 按严重程度分组返回发现（严重 → 轻微）
```

---

## 清单类别

1. **空值安全** - NPE 风险、Optional 使用
2. **异常处理** - 异常吞没、堆栈跟踪
3. **集合与流** - 迭代、可变性
4. **并发** - 线程安全、竞态条件
5. **Java 惯用写法** - equals/hashCode、建造者
6. **资源管理** - try-with-resources
7. **API 设计** - 布尔参数、校验
8. **性能** - 字符串拼接、N+1 查询

---

## 注意事项

- 适用于聚焦的变更（单个类或 PR）效果最佳
- 包含对良好实践的正向反馈
- 为审查中发现的边界情况建议测试
