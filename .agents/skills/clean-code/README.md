# 整洁代码

**加载**: `view .claude/skills/clean-code/SKILL.md`

---

## 功能说明

整洁代码原则及 Java 示例：DRY、KISS、YAGNI、命名规范、函数设计、代码异味和重构技巧。

---

## 使用场景

- "整理这段代码"
- "重构这个方法"
- "提高可读性"
- "这个函数太长了"
- "这个变量该怎么命名？"
- "这段代码是否过于复杂？"

---

## 示例

```
> view .claude/skills/clean-code/SKILL.md
> "这个方法有 100 行，帮我重构"
→ 识别代码异味，建议提取方法、卫语句等
```

---

## 涵盖原则

| 原则 | 核心问题 |
|------|----------|
| **DRY** | 这段逻辑是否在其他地方重复了？ |
| **KISS** | 是否有更简单的方式？ |
| **YAGNI** | 我们现在需要这个，还是"以防万一"？ |

---

## 主题

- 命名规范（变量、方法、类）
- 函数设计（大小、参数、抽象层次）
- 注释（何时好，何时坏）
- 代码异味检测
- 重构技巧

---

## 相关技能

- `solid-principles` - 类设计原则
- `design-patterns` - 常见解决方案
- `java-code-review` - 完整审查清单

---

## 参考资料

- [《代码整洁之道》Robert C. Martin](https://www.oreilly.com/library/view/clean-code-a/9780136083238/)
- [《重构》Martin Fowler](https://refactoring.com/)
- [Refactoring Guru - 代码异味](https://refactoring.guru/refactoring/smells)
