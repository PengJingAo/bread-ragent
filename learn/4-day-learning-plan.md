# Ragent 项目 4 天系统学习大纲

## 学习目标

4 天后，你应该能够清楚回答这些问题：

1. 项目为什么拆成 `bootstrap`、`framework`、`infra-ai`、`mcp-server` 和 `frontend`。
2. 一次用户提问如何从前端进入后端，再经过意图识别、问题重写、检索、Prompt 构造、模型调用和结果返回。
3. 文档如何从上传、解析、切分、向量化，最终进入知识库并变成可检索内容。
4. 多路检索、重排、模型路由、容错和 MCP 调用分别解决什么工程问题。
5. 如果要新增检索策略、模型供应商、文档入库节点或管理页面，应该从哪里下手。

## 第 1 天：项目全貌与本地启动

### 学习主题

先建立项目地图，理解各模块边界和本地运行方式。

### 上午：阅读文档与模块结构

重点阅读：

- `README.md`
- `docs/quick-start.md`
- `docs/multi-channel-retrieval.md`
- `AGENTS.md`
- 根目录 `pom.xml`

需要理解的模块：

- `bootstrap/`：主 Spring Boot 应用，包含 RAG、知识库、文档入库、用户和后台管理等业务。
- `framework/`：通用基础能力，例如 Web 响应、异常、缓存、数据库、MQ、Trace 和上下文。
- `infra-ai/`：AI 基础设施，例如模型调用、模型路由、Embedding、Token 统计和重排。
- `mcp-server/`：独立 MCP Server 工具。
- `frontend/`：React + TypeScript 管理端和聊天界面。
- `resources/`：数据库、Docker Compose、示例文档和部署资源。

### 下午：跑通基础命令

建议执行：

```bash
./mvnw clean package
./mvnw test
./mvnw -pl bootstrap spring-boot:run
```

前端命令：

```bash
cd frontend
npm install
npm run dev
```

重点查看：

- `bootstrap/src/main/resources/application.yaml`
- `resources/docker/`
- `bootstrap/pom.xml`
- `infra-ai/pom.xml`
- `frontend/package.json`

### 当天产出

- 画一张模块依赖图。
- 用 3 句话说明每个模块的职责。
- 记录本地启动依赖的中间件和配置，例如 PostgreSQL、Redis、Milvus、RocketMQ、模型 Key 等。

## 第 2 天：RAG 问答主链路

### 学习主题

从一次用户提问出发，追完整条 RAG 问答链路。

### 上午：阅读 RAG 主干代码

重点目录：

- `bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/controller`
- `bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/service`
- `bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core`
- `bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/intent`
- `bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/rewrite`
- `bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/prompt`
- `bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/memory`
- `bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/vector`
- `bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/trace`

建议阅读顺序：

1. 从 Controller 找用户提问入口。
2. 进入 Service，看一次问答如何被编排。
3. 看意图识别，理解系统如何判断用户需求。
4. 看问题重写，理解多轮上下文如何补全问题。
5. 看 Prompt 构造，理解检索结果如何进入模型上下文。
6. 看 Trace，理解一次 RAG 调用如何被观测和排查。

### 下午：深入多路检索

重点目录和文档：

- `bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/retrieve`
- `bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/retrieve/channel`
- `bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/retrieve/channel/strategy`
- `bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/retrieve/postprocessor`
- `docs/multi-channel-retrieval.md`

需要理解：

- 为什么不能只做单一路向量检索。
- 多个检索通道如何并行执行。
- 检索结果如何去重、合并和排序。
- 重排发生在哪一层。
- `topK`、score、rerank、过滤条件分别在哪里生效。

### 当天产出

- 画一张用户提问链路图：

```text
Controller -> Service -> Intent -> Rewrite -> Retrieve -> Rerank -> Prompt -> LLM -> Trace -> Response
```

- 用自己的话解释多路检索相比普通 RAG 的价值。
- 找出 3 个最核心的 RAG 类，并写下它们的职责。

## 第 3 天：文档入库 Pipeline 与 AI 基础设施

### 学习主题

理解知识如何进入系统，并变成可检索内容。

### 上午：阅读文档入库链路

重点目录：

- `bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion`
- `bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/controller`
- `bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/service`
- `bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/engine`
- `bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/node`
- `bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/domain/pipeline`
- `bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/strategy`
- `bootstrap/src/main/java/com/nageoffer/ai/ragent/core/parser`
- `bootstrap/src/main/java/com/nageoffer/ai/ragent/core/chunk`
- `bootstrap/src/main/java/com/nageoffer/ai/ragent/core/chunk/strategy`

建议阅读顺序：

1. 找到文档上传和入库入口。
2. 理解文件如何被解析成文本。
3. 理解文本如何切块。
4. 理解 Pipeline 节点如何编排。
5. 理解节点之间的输入和输出如何传递。
6. 理解执行日志、失败重试和状态流转如何处理。

### 下午：阅读 `infra-ai`

重点关注：

- 模型调用抽象。
- 模型路由。
- 模型健康检查。
- 降级容错。
- Embedding。
- Token 统计。
- Rerank。

结合依赖理解这些组件的角色：

- Milvus：向量数据库。
- Tika：文档解析。
- OkHttp：模型 HTTP 调用。
- Redisson / Redis：缓存、状态和分布式能力。
- RocketMQ：异步消息。
- MCP SDK：工具调用协议。

### 当天产出

- 画一张文档入库链路图：

```text
上传 -> 解析 -> 清洗 -> 切块 -> Embedding -> 存储 -> 可检索
```

- 总结 5 个工程难点：
  - 文档格式复杂。
  - 切块粒度取舍。
  - 向量化失败处理。
  - 异步任务状态管理。
  - 增量更新和重复入库。

- 找一个 Pipeline 节点，说明它的输入、处理逻辑和输出。

## 第 4 天：前端、MCP、扩展点与实战改造

### 学习主题

把项目从能读懂推进到能动手改。

### 上午：阅读前端结构

重点目录：

- `frontend/src/pages`
- `frontend/src/pages/admin`
- `frontend/src/components`
- `frontend/src/components/chat`
- `frontend/src/services`
- `frontend/src/stores`
- `frontend/src/types`

重点页面：

- 聊天界面。
- 知识库管理：`frontend/src/pages/admin/knowledge`
- 入库管理：`frontend/src/pages/admin/ingestion`
- 意图树：`frontend/src/pages/admin/intent-tree`
- Trace：`frontend/src/pages/admin/traces`
- 设置：`frontend/src/pages/admin/settings`

需要理解：

- 前端路由如何组织。
- API 服务层如何封装。
- Zustand 状态存储用在哪里。
- 管理端页面如何对应后端 Controller。
- Trace 页面如何帮助调试 RAG 链路。

### 下午：阅读 MCP 与扩展点

重点目录：

- `mcp-server/`
- `bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/mcp`
- RAG 意图识别与工具调用相关代码。

建议做一个小实战，三选一：

1. 新增一个简单后端查询接口，并在前端管理页展示。
2. 新增一个检索后处理器，例如按分数阈值过滤。
3. 新增一个文档入库 Pipeline 节点，例如简单文本清洗或日志记录。

优先推荐第 2 个，因为它最贴近 RAG 核心链路。

### 当天产出

写一份项目复盘笔记，包含：

- 项目定位。
- 技术栈。
- 核心链路。
- 关键模块。
- 你能扩展的 3 个点。
- 面试中可以怎么讲。

## 推荐每日节奏

每天按这个节奏学习：

```text
上午：读文档和主干代码
下午：画链路图、跑功能、跟断点、写总结
晚上：用 30 分钟复述当天内容
```

## 最重要的阅读顺序

1. `README.md`
2. `docs/quick-start.md`
3. 根目录 `pom.xml`
4. `bootstrap` 的 RAG 主链路
5. `bootstrap` 的文档入库链路
6. `infra-ai` 的模型抽象和路由
7. `frontend` 的页面和 API 调用
8. `mcp-server`

## 学习完成标准

完成 4 天学习后，你应该能够：

- 独立画出 RAG 问答链路和文档入库链路。
- 讲清楚多模块分层的原因。
- 找到一次问答请求涉及的核心类。
- 找到一次文档入库任务涉及的核心类。
- 解释多路检索、模型路由、MCP 和 Trace 的工程价值。
- 完成一个小扩展，并知道如何验证它。
