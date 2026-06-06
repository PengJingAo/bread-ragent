# Ragent AI - Agentic RAG 智能平台

## 项目概述

Ragent AI 是一个企业级 Agentic RAG（检索增强生成）平台，基于 Spring Boot 3 + React 构建，提供从文档接入到智能问答的完整解决方案，集成向量数据库、智能体、会话记忆和深度思考能力。

- **代码规模**：Java 约 40,000 行 / 前端约 18,000 行
- **数据库**：PostgreSQL，20 张业务表
- **前端页面**：22 个页面/组件

## 技术栈

### 后端
- Java 17 / Spring Boot 3.5.7
- PostgreSQL + pgvector（向量存储）/ Milvus 2.6.6（可选向量库）
- Redis + Redisson（缓存/分布式限流）
- MyBatis-Plus 3.5.14（ORM）
- Sa-Token 1.43.0（认证鉴权）
- Apache Tika 3.2.3（文档解析）
- RocketMQ 2.3.5（消息队列）
- MCP SDK 1.1.2（Model Context Protocol）
- OkHttp 4.12.0 / Hutool 5.8.37

### AI 模型
- **多供应商支持**：Ollama（本地）、百炼/阿里云、AIHubMix、SiliconFlow
- **默认聊天模型**：qwen3-max（支持深度思考）
- **默认嵌入模型**：qwen-emb-8b
- **默认重排模型**：qwen3-rerank

### 前端
- React 18.3.1 + TypeScript
- Vite 5.4.3 构建
- Tailwind CSS 3.4.10 + Radix UI
- Zustand 状态管理 / React Router / Axios / Recharts

## Maven 模块结构

```
ragent (父 POM)
├── bootstrap/     # 主应用：业务逻辑、控制器、服务（入口模块）
├── framework/     # 基础设施层：缓存、数据库、Web、MQ 通用配置
├── infra-ai/      # AI 基础层：多模型适配、LLM 路由、流式处理
└── mcp-server/    # MCP 工具服务器（独立端口 9099）
```

### bootstrap 核心包结构

| 包路径 | 职责 |
|--------|------|
| `admin/` | 管理后台：系统配置、用户/知识库/模型管理、链路追踪 |
| `rag/core/intent/` | 意图识别：树状分类、置信度评分 |
| `rag/core/memory/` | 会话记忆：历史压缩、摘要生成 |
| `rag/core/retrieve/` | 多通道检索：并行搜索、去重、后处理链 |
| `rag/core/rewrite/` | 查询改写：多问题扩展 |
| `rag/core/vector/` | 向量存储：PostgreSQL/Milvus 实现 |
| `rag/core/mcp/` | MCP 工具集成 |
| `rag/core/guidance/` | 引导策略 |
| `rag/core/prompt/` | Prompt 模板管理 |
| `ingestion/` | 文档处理管线：抓取、解析、分块、入库 |
| `knowledge/` | 知识库管理：存储、过滤、调度、实时更新 |
| `user/` | 用户管理：认证、权限 |

## 构建与运行

### 后端

```bash
# 编译（跳过测试）
./mvnw clean package -DskipTests

# 运行主应用（端口 9090，上下文路径 /api/ragent）
./mvnw spring-boot:run -pl bootstrap

# 代码格式化（Spotless）
./mvnw spotless:apply

# 检查代码格式
./mvnw spotless:check
```

### 前端

```bash
cd frontend
npm install
npm run dev      # 开发服务器
npm run build    # 生产构建
npm run lint     # ESLint 检查
```

### 基础设施

```bash
# Milvus 向量数据库（含 etcd + MinIO + Attu UI）
docker compose -f resources/docker/milvus-stack-2.6.6.compose.yaml up -d

# RocketMQ 消息队列
docker compose -f resources/docker/rocketmq-stack-5.2.0.compose.yaml up -d

# 轻量级开发环境
docker compose -f resources/docker/lightweight/ up -d
```

### 数据库初始化

SQL 脚本位于 `resources/database/`：
- `schema_pg.sql` — 建表脚本
- `init_data_pg.sql` — 初始数据
- `upgrade_v1.0_to_v1.1.sql` / `upgrade_v1.1_to_v1.2.sql` — 版本升级

## 核心设计模式

- **策略模式**：检索通道、后处理器、MCP 工具的可插拔实现
- **工厂模式**：复杂对象创建
- **注册表模式**：组件自动发现与注册
- **模板方法**：Ingestion Pipeline 节点
- **装饰器模式**：首包探测（First-packet probing）
- **责任链模式**：后处理链（Post-processor chain）
- **观察者模式**：事件通知
- **三态熔断器**：模型健康检查与故障转移

## 生产级特性

- 多模型路由与故障转移（三态熔断器）
- 基于 Redis 的分布式限流（全局 + 用户级）
- AOP 链路追踪（Trace）
- SSE 流式响应 + 首包检测
- 会话记忆压缩（长对话摘要）
- MCP 工具调用（非知识型查询）
- 8 个独立线程池隔离不同工作负载

## 代码规范

- **代码格式化**：使用 Spotless Maven 插件，版权头模板在 `resources/format/copyright.txt`
- **Lombok**：已配置 `config.stopBubbling=true`，`@Data` 默认 `callSuper=false`，`copyableAnnotations` 支持 `@Qualifier`
- **包命名**：`com.nageoffer.ai.ragent.*`
- **分层架构**：Controller → Service → DAO/Mapper，严格单向依赖
- **请求/响应对象**：`request/` 包放请求 DTO，`vo/` 包放响应 VO
- **MyBatis Mapper 扫描**：4 个包 — `rag.dao.mapper`, `ingestion.dao.mapper`, `knowledge.dao.mapper`, `user.dao.mapper`

## 测试

测试位于 `bootstrap/src/test/`，覆盖：
- 向量存储（PgVectorStoreServiceTest / MilvusCollectionTests）
- 意图识别（IntentTreeServiceTests / SimpleIntentClassifierTests / VectorTreeIntentClassifierTests）
- 查询改写（QueryRewriteTests / MultiQuestionRewriteServiceTests）
- 嵌入服务（SiliconFlowEmbeddingServiceTests）
- 会话服务（ConversationMessageServiceTests）
- 知识调度（ScheduleRefreshProcessorTest）

```bash
# 运行全部测试
./mvnw test

# 运行单个测试类
./mvnw test -pl bootstrap -Dtest=IntentTreeServiceTests
```

## 关键配置项（application.yaml）

| 配置 | 默认值 | 说明 |
|------|--------|------|
| `server.port` | 9090 | 后端端口 |
| `server.servlet.context-path` | /api/ragent | API 前缀 |
| `ragent.vector-store.type` | pg | 向量存储类型（pg/milvus） |
| `ragent.vector-store.dimension` | 1536 | 向量维度 |
| `ragent.rate-limit.global-max-concurrent` | 10 | 全局最大并发 |
| `ragent.memory.history-keep-turns` | 4 | 历史保留轮次 |
| `ragent.memory.summary-start-turns` | 5 | 摘要启动轮次 |

## 前端页面

| 路径 | 功能 |
|------|------|
| `/` | 聊天主界面 |
| `/login` | 登录页 |
| `/admin/dashboard` | 管理仪表盘 |
| `/admin/knowledge` | 知识库管理 |
| `/admin/ingestion` | 文档管线管理 |
| `/admin/intent-tree` | 意图树管理 |
| `/admin/settings` | 系统配置 |
| `/admin/users` | 用户管理 |
| `/admin/traces` | 链路追踪 |
| `/admin/query-term-mapping` | 查询词映射 |
| `/admin/sample-questions` | 示例问题 |

## 外部依赖要求

- PostgreSQL（5432）+ pgvector 扩展
- Redis（6379）
- Ollama（11434，本地模型）
- Milvus（可选，19530）
- RocketMQ（可选，9876）
