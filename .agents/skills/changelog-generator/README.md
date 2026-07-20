# 变更日志生成器

**加载**: `view .claude/skills/changelog-generator/SKILL.md`

---

## 功能说明

根据 git 提交记录生成变更日志，遵循已有规范。自动从项目文件（CLAUDE.md、git 标签、CHANGELOG.md）中检测版本控制风格和变更日志格式。

---

## 使用场景

- "生成自上次发布以来的变更日志"
- "v3.14.0 以来有什么变化？"
- "更新版本 3.16 的 CHANGELOG.md"
- "预览未发布的变更"

---

## 示例

```
> view .claude/skills/changelog-generator/SKILL.md
> "为 pf4j 生成变更日志"
→ 检测 pf4j 格式和 SemVer 风格，输出匹配的变更日志
```

---

## 核心特性

- **版本检测**: SemVer（x.y.z）、双段式（x.y）、CalVer（YYYY.MM）
- **格式检测**: 自适应已有 CHANGELOG.md 风格
- **引用式链接**: 简洁的 `[#123]` 格式，底部附定义
- **版本比较链接**: 自动生成 GitHub 比较链接
- **遗留项目支持**: 适用于无明确规范的项目

---

## 检测优先级

1. CLAUDE.md `## Versioning` 部分
2. Git 标签模式分析
3. 已有 CHANGELOG.md 格式
4. 询问用户（最后手段）

---

## 注意事项

- 与约定式提交配合使用效果最佳（搭配 git-commit 技能）
- 对于遗留项目，建议在 CLAUDE.md 中添加版本控制规范
- 更新时保留已有的链接定义
