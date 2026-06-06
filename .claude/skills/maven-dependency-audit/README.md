# Maven 依赖审计

**加载**: `view .claude/skills/maven-dependency-audit/SKILL.md`

---

## 功能说明

审计 Maven 依赖的过期版本、安全漏洞和冲突。使用标准 Maven 插件，无需额外工具。

---

## 使用场景

- "检查过期的依赖"
- "发布前审计依赖"
- "查找 pom.xml 中的安全漏洞"
- "为什么 commons-logging 出现在我的项目中？"

---

## 示例

```
> view .claude/skills/maven-dependency-audit/SKILL.md
> "审计 pf4j 的依赖"
→ 运行检查，按严重程度分类更新，生成报告
```

---

## 使用的工具

| 工具 | 用途 |
|------|------|
| `mvn versions:display-dependency-updates` | 查找过期依赖 |
| `mvn dependency:tree` | 分析依赖图 |
| `mvn dependency:analyze` | 查找未使用的依赖 |
| `mvn dependency-check:check` | 安全漏洞扫描（OWASP） |

---

## 注意事项

- 每月或每次发布前运行
- 补丁更新通常安全；主要版本更新需要评审
- 使用 `-Dincludes=groupId` 过滤大型依赖树
- 考虑启用 GitHub Dependabot 进行自动告警
