# JPA 模式

**加载**: `view .claude/skills/jpa-patterns/SKILL.md`

---

## 功能说明

Spring 应用的 JPA/Hibernate 模式和常见陷阱。涵盖 N+1 问题、延迟加载、事务、实体关系和查询优化。

---

## 使用场景

- "执行了太多 SQL 查询"
- "LazyInitializationException 错误"
- "代码中的 N+1 问题"
- "如何优化 JPA 查询？"
- "EAGER vs LAZY 加载策略"
- "实体关系最佳实践"

---

## 示例

```
> view .claude/skills/jpa-patterns/SKILL.md
> "加载 10 个订单时执行了 100 条查询"
→ 识别 N+1 问题，建议 JOIN FETCH 或 @EntityGraph
```

---

## 涵盖主题

| 主题 | 要点 |
|------|------|
| **N+1 问题** | JOIN FETCH、@EntityGraph、@BatchSize |
| **延迟加载** | FetchType.LAZY、LazyInitializationException 解决方案 |
| **事务** | @Transactional、传播行为、只读 |
| **关系映射** | OneToMany、ManyToMany、双向同步 |
| **优化** | 分页、DTO 投影、批量操作 |
| **锁** | @Version、OptimisticLockException |

---

## 常见错误

- @ManyToOne 上使用 CascadeType.ALL
- 缺少数据库索引
- toString() 触发延迟加载
- 同一类内调用 @Transactional

---

## 相关技能

- `spring-boot-patterns` - Spring Boot 模式
- `java-code-review` - 代码审查清单

---

## 参考资料

- [Hibernate ORM 文档](https://hibernate.org/orm/documentation/)
- [Spring Data JPA 参考](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [Vlad Mihalcea 博客](https://vladmihalcea.com/) - JPA/Hibernate 深度解析
- [《高性能 Java 持久化》](https://vladmihalcea.com/books/high-performance-java-persistence/)
