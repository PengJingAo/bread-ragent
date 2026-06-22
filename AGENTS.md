# 仓库指南

## 项目结构与模块组织

这是一个 Java 17 Maven 多模块项目，并包含 React/Vite 前端。

- `bootstrap/`: 主 Spring Boot RAG 应用、提示词和后端测试。
- `framework/`: 共享 Web、异常、缓存、数据库和约定工具。
- `infra-ai/`: 模型路由、Token 计数、重排和 HTTP 基础设施。
- `mcp-server/`: 独立 MCP Server 工具。
- `frontend/`: React 18 + TypeScript 管理/聊天界面，源码位于 `frontend/src`。
- `resources/`: 数据库结构、Docker Compose 编排、格式文件和示例文档。
- `docs/` 和 `assets/`: 架构说明、示例，以及文档中使用的图片。

## 构建、测试与开发命令

- `./mvnw clean package`: 构建所有后端模块。
- `./mvnw test`: 运行后端单元测试/集成测试。
- `./mvnw -pl bootstrap spring-boot:run`: 在本地运行主后端服务。
- `./mvnw spotless:apply`: 根据根目录 `pom.xml` 应用 Java 许可证头。
- `cd frontend && npm install`: 根据 `package-lock.json` 安装前端依赖。
- `cd frontend && npm run dev`: 在 `5173` 启动 Vite；`/api` 代理到 `localhost:8080`。
- `cd frontend && npm run build`: 生成前端生产构建。
- `cd frontend && npm run lint`: 对 TypeScript/React 文件运行 ESLint。

## 编码风格与命名约定

后端使用 Java 17、Spring Boot 3、Lombok，根包名为 `com.nageoffer.ai.ragent`。保持后端后缀一致：`Controller`、`Service`、`ServiceImpl`、`Mapper`、`DO`、`VO`、`Request` 和 `Properties`。测试代码放在匹配的 `src/test/java` 包路径下，并命名为 `*Tests` 或 `*Test`。

每次优化或调整代码后，将所改动的地方和原因按照顺序保存为.md文件，总结一段与修改内容相关的小标题作为文件的命名，放到change文件夹中。

前端代码使用 TypeScript、React 函数组件、Tailwind CSS、Radix UI 和 lucide 图标。组件命名为 `PascalCase.tsx`，Hooks 命名为 `useX.ts`，服务命名为 `xService.ts`，状态存储命名为 `xStore.ts`。

## 测试指南

后端测试使用 Spring Boot Test、JUnit 和 Mockito。业务规则优先编写聚焦的 Service 测试；只有在数据库、向量存储或外部配置行为确实相关时才编写集成测试。部分测试需要 PostgreSQL、Redis、Milvus、RocketMQ 或模型凭证；请在测试或 PR 中说明前置条件。

前端目前使用 lint/build 检查，而不是测试运行器。涉及 UI 修改时，运行 `npm run lint` 和 `npm run build`，然后手动验证受影响流程。

## 提交与 Pull Request 指南

当前 shell 中无法获得 Git 历史，因此未能确认仓库特定提交规范。请使用简洁的祈使句提交信息，例如 `Add ingestion schedule retry` 或 `Fix chat stream timeout`。

Pull Request 应包含问题描述、实现方式、已运行的测试命令，以及任何配置/结构变更。前端改动应附截图，并关联相关 issue。

## 安全与配置提示

不要提交 API Key、模型凭证、数据库密码或本地覆盖配置。可复用部署配置放在 `resources/docker/`，迁移文件放在 `resources/database/`。将 `bootstrap/src/main/resources/application.yaml` 视为共享默认配置；环境特定覆盖配置应保留在版本控制之外。
