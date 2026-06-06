# 设计模式

**加载**: `view .claude/skills/design-patterns/SKILL.md`

---

## 功能说明

常见设计模式及实用 Java 示例。涵盖创建型、行为型和结构型模式，使用现代 Java 语法和 Spring 集成。

---

## 使用场景

- "用工厂模式实现通知"
- "用建造者模式构建这个复杂对象"
- "如何在不修改类的情况下添加功能？"（装饰器）
- "多种支付方式，运行时切换"（策略）
- "下订单时通知多个服务"（观察者）

---

## 示例

```
> view .claude/skills/design-patterns/SKILL.md
> "我需要创建不同的报告类型（PDF、Excel、CSV）"
→ 建议工厂模式并提供实现示例
```

---

## 涵盖模式

| 类别 | 模式 |
|------|------|
| **创建型** | 建造者、工厂方法、单例 |
| **行为型** | 策略、观察者、模板方法 |
| **结构型** | 装饰器、适配器 |

---

## 快速选择指南

| 问题 | 模式 |
|------|------|
| 构造器参数过多 | 建造者 |
| 创建对象时不指定具体类 | 工厂 |
| 运行时切换算法 | 策略 |
| 动态添加行为 | 装饰器 |
| 通知多个对象 | 观察者 |
| 集成遗留代码 | 适配器 |

---

## 相关技能

- `solid-principles` - 模式所实现的原则
- `clean-code` - 代码级实践
- `spring-boot-patterns` - Spring 实现

---

## 参考资料

- [Refactoring Guru - 设计模式](https://refactoring.guru/design-patterns)
- [《设计模式》GoF](https://www.oreilly.com/library/view/design-patterns-elements/0201633612/)
- [Java 设计模式（java-design-patterns.com）](https://java-design-patterns.com/)
