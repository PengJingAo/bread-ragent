# Java 迁移

**加载**: `view .claude/skills/java-migration/SKILL.md`

---

## 功能说明

Java 项目在主要 LTS 版本之间升级（8→11→17→21→25）的分步指南。包含破坏性变更、移除的 API、应采用的新特性，以及框架特定迁移（Spring Boot、Hibernate）。

---

## 使用场景

- "升级项目到 Java 25"
- "从 Java 21 迁移到 25"
- "Spring Boot 3 迁移"
- "升级到 Java 25 会破坏什么？"
- "修复 javax.xml.bind 找不到"

---

## 示例

```
> view .claude/skills/java-migration/SKILL.md
> "将这个项目从 Java 11 升级到 21"
→ 分析代码，识别破坏性变更，提供分步修复方案
```

---

## 涵盖的迁移路径

| 从 | 到 | 关键变更 |
|----|-----|----------|
| Java 8 | Java 11 | JAXB 移除、模块系统、内部 API |
| Java 11 | Java 17 | Records、密封类、强封装 |
| Java 17 | Java 21 | 虚拟线程、模式匹配、有序集合 |
| Java 21 | Java 25 | Security Manager 移除、Unsafe 移除、Scoped Values 定版 |
| Spring Boot 2.x | 3.x | javax.* → jakarta.*、需要 Java 17 |
| Hibernate 5 | 6 | 查询 API 变更、ID 生成 |

---

## 使用的工具

| 工具 | 用途 |
|------|------|
| `grep` | 查找已废弃的 API 使用 |
| `mvn compile` | 识别编译错误 |
| OpenRewrite | 自动化 Spring Boot 3 迁移 |
| `--add-opens` | 修复反射访问问题 |

---

## 注意事项

- 始终在 LTS 之间迁移（8→11→17→21→25）
- 先更新 Lombok、Mockito 到最新版本
- 使用 OpenRewrite 进行自动化迁移
- 每步之后充分测试
- Java 25 LTS 支持到 2033 年 9 月

## 参考资料

- [Oracle JDK 25 迁移指南](https://docs.oracle.com/en/java/javase/25/migrate/)
- [Oracle JDK 25 发行说明](https://www.oracle.com/java/technologies/javase/25-relnote-issues.html)
- [OpenRewrite Java 迁移配方](https://docs.openrewrite.org/recipes/java/migrate)
- [Spring Boot 3.0 迁移指南](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide)
