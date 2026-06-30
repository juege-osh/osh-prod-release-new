# OSH 上线治理台

这是给 `release/20260708` 做的独立上线治理系统，后端用 Java Spring Boot，前端用 Vue 3。

它不复用主站业务库。默认只写自己的治理库，用来管理变更单、子 change、审批、评审测试证据、自动化测试报告、蓝绿切换记录和回滚记录。

完整操作说明见 [docs/USAGE.md](docs/USAGE.md)。

## 默认账号

- 觉哥：`juege`
- 评审 A：`reviewer_a`
- 评审 B：`reviewer_b`
- 运维：`ops`

本地开发 profile 带默认演练密码；Docker/生产部署必须在 `.env` 里显式配置。

## 本地运行

后端：

```bash
mvn -pl backend spring-boot:run
```

前端：

```bash
cd frontend
npm install
npm run dev
```

Docker：

```bash
cp .env.example .env
# 编辑 .env，填好数据库、JWT 和 4 个治理台账号密码
docker compose up -d --build
```

默认访问：

- 前端：http://localhost:18081
- 后端：http://localhost:18080/actuator/health

API 冒烟：

```bash
scripts/preflight.sh
scripts/local_smoke.sh
OSH_RELEASE_PASSWORD='治理台密码' python3 scripts/smoke_release_flow.py
```

`preflight.sh` 会先跑构建、测试、compose 配置和敏感信息扫描。`local_smoke.sh` 会用临时库启动本地后端并跑完整 API 冒烟。API 冒烟脚本默认只打 `http://127.0.0.1:18080/api`，会验证缺报告不能切绿、报告齐全后才能切绿、回蓝和回滚记录是否正常。

生产建议挂独立路径，不要覆盖 `https://osh.lol/` 主站首页。推荐路径：`/release-console/`，详细反代配置见 [DEPLOY.md](docs/DEPLOY.md)。

## 安全边界

- 生产环境写操作默认禁止。
- 系统里的切绿、回滚先只记录治理状态，不直接执行服务器命令。
- 真实执行前必须人工确认命令、备份、回滚脚本和责任人。
- 数据量报告只保存摘要，不保存课程、用户等敏感业务明细。

## 已内置的上线规则

- 每个变更单会拆成多个子 change，每个组件都有负责人、增量计划、回滚计划、测试计划和数据采集计划。
- 每个组件必须有两个评审人的测试证据，证据里会记录环境、结果、演示确认和责任确认。
- 常规上线需要两个组内评审人和觉哥最终确认。
- 紧急上线除了常规规则，还要求每个子 change 都有觉哥确认。
- 如果开发人参与评审，必须先记录向另一位评审演示确认。
- 绿环境先发布，组件规范、评审测试证据、功能测试和数据量对比都通过后才允许切绿。
- 切绿后必须负责人在生产绿系统人工验证；通过后才能记录同步到蓝系统。
- 发现问题可以立即记录回蓝，或按节点 rollback_order 回滚。
- 新增组件必须填写配置目录、数据目录和部署目录。

## 已覆盖组件

- MySQL
- Redis
- Nacos
- Kafka
- Elasticsearch
- HBase
- Java 后端
- Vue 前端
- Nginx
- Docker Compose
- MongoDB 扩展组件

## 报告能力

- 组件规范校验：配置目录、数据目录、部署目录、增量计划、回滚计划、测试计划、数据采集计划。
- 功能测试：按节点覆盖数据库、缓存、消息、搜索、配置中心、存储、Java 接口、前端入口和网关。
- 数据量对比：单独列出课程模块、用户模块和治理库变化。
- 环境差异：记录测试环境单套部署和生产蓝绿部署的区别。
- announce 检查：把测试环境 announce 文件检查纳入上线前报告。
- 操作链：创建、测试、切绿、回蓝、同步蓝、回滚都会留记录。

## 安全模式

当前版本是治理闭环加安全执行器。按钮会生成完整计划、报告和操作记录，但默认不会直接 SSH 到生产改网关、改业务库或写课程/用户数据。

要打开真实执行，需要再接入白名单命令执行器，并在变更单里保留 dry-run、备份、回滚脚本和觉哥二次确认。

## 主项目基线

- 后端：`juege-osh/osh-backend` 的 `release/20260708`，提交 `8d2ee47cbb7dccaffabad8822ceadc8ccb7b8cb7`
- 前端：`juege-osh/osh-frontend` 的 `release/20260708`，提交 `868b87f55a7c267e40bf37d4fd0573973ae75ba3`
