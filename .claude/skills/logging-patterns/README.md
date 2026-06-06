# 日志模式

**加载**: `view .claude/skills/logging-patterns/SKILL.md`

---

## 功能说明

Java 日志最佳实践，使用 SLF4J、结构化日志（JSON）和 MDC 进行请求追踪。包含为 Claude Code 分析优化的 AI 友好日志格式。

---

## 使用场景

- "为这个服务添加日志"
- "调试这个流程"（AI 读取日志）
- "配置结构化日志"
- "为什么这个请求失败了？"（分析日志）
- "添加请求追踪"

---

## 核心洞察：JSON 适合 AI

**JSON 日志更适合 AI/Claude Code 分析：**

| 方面 | 文本日志 | JSON 日志 |
|------|----------|-----------|
| 解析 | 正则表达式解读 | 直接字段访问 |
| Token 消耗 | 较高 | 较低 |
| 过滤 | grep 模式 | jq 查询 |

```bash
# AI 可以轻松过滤 JSON
cat app.log | jq 'select(.requestId == "abc123")'
```

---

## 涵盖主题

| 主题 | 说明 |
|------|------|
| **AI 友好日志** | 为 Claude Code 优化的 JSON 格式 |
| **Spring Boot 3.4+** | 原生结构化日志支持 |
| **Logstash 编码器** | 适用于 Spring Boot < 3.4 |
| **SLF4J/MDC** | 请求上下文、关联 ID |
| **日志级别** | 何时使用 ERROR、WARN、INFO、DEBUG |
| **该记录什么** | 业务事件、耗时、流程步骤 |
| **不该记录什么** | 密码、个人隐私信息、敏感数据 |

---

## 快速配置（Spring Boot 3.4+）

```yaml
logging:
  structured:
    format:
      console: logstash
```

无需额外依赖！

---

## 相关技能

- `spring-boot-patterns` - Spring 配置
- `jpa-patterns` - 数据库日志

---

## 参考资料

- [Spring Boot 3.4 结构化日志（spring.io）](https://spring.io/blog/2024/08/23/structured-logging-in-spring-boot-3-4/)
- [Spring Boot 结构化日志（Baeldung）](https://www.baeldung.com/spring-boot-structured-logging)
- [Java 日志 10 大最佳实践（Better Stack）](https://betterstack.com/community/guides/logging/how-to-start-logging-with-java/)
- [Booking.com - 结构化日志](https://medium.com/booking-com-development/unlocking-observability-structured-logging-in-spring-boot-c81dbabfb9e7)
- [SLF4J 手册](https://www.slf4j.org/manual.html)
