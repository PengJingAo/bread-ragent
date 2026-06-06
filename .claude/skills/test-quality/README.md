# 测试质量（JUnit 5 + AssertJ）

**加载**: `view .claude/skills/test-quality/SKILL.md`

---

## 功能说明

帮助 Claude 为 Java 项目建议有意义的 JUnit 测试并提升测试覆盖率。

---

## 使用场景

- "为 PluginManager.loadAll() 添加测试"
- "审查 PluginLoaderTest 中已有的测试"
- "提升生命周期模块的测试覆盖率"

---

## 示例

```
> view .claude/skills/test-quality/SKILL.md
> "为 ExtensionFactory 添加含边界情况的单元测试"
→ 生成使用 AssertJ 断言的 JUnit 5 测试
```

---

## 注意事项

- 当类/方法签名可用时效果最佳
- 可以建议缺失的边界情况或空值检查
