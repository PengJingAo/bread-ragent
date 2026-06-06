# SOLID 原则

**加载**: `view .claude/skills/solid-principles/SKILL.md`

---

## 功能说明

SOLID 原则检查清单及详细 Java 示例。每个原则包含违规示例、重构方案和检测模式。

---

## 使用场景

- "检查这个类是否违反 SOLID 原则"
- "这个类是否做了太多事？"（SRP）
- "如何在不修改代码的情况下添加新类型？"（OCP）
- "为什么 Square 不应该继承 Rectangle？"（LSP）
- "这个接口太大了"（ISP）
- "如何使这个可测试？"（DIP）

---

## 示例

```
> view .claude/skills/solid-principles/SKILL.md
> "审查 UserService 的 SOLID 原则"
→ 识别 SRP 违规，建议提取校验和通知逻辑
```

---

## 涵盖原则

| 原则 | 核心问题 |
|------|----------|
| **单**一职责 | 它是否只有一个变更原因？ |
| **开**闭原则 | 我能否在不修改的情况下扩展？ |
| **里**氏替换 | 子类型能否替换基类型？ |
| **接**口隔离 | 客户端是否被迫实现不用的方法？ |
| **依**赖倒置 | 它是否依赖于抽象？ |

---

## 相关技能

- `design-patterns` - 实现模式
- `clean-code` - DRY、KISS、YAGNI
- `java-code-review` - 完整审查清单

---

## 参考资料

- [SOLID（维基百科）](https://en.wikipedia.org/wiki/SOLID)
- [《代码整洁之道》Robert C. Martin](https://www.oreilly.com/library/view/clean-code-a/9780136083238/)
- [SOLID 原则 Java 实践（Baeldung）](https://www.baeldung.com/solid-principles)
