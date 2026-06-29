# OSH 上线治理台

这是给 `release/20260708` 做的独立上线治理系统，后端用 Java Spring Boot，前端用 Vue 3。

它不复用主站业务库。默认只写自己的治理库，用来管理变更单、审批、自动化测试报告、蓝绿切换记录和回滚记录。

## 默认账号

- 觉哥：`juege / Juege@2026`
- 评审 A：`reviewer_a / Review@2026`
- 评审 B：`reviewer_b / Review@2026`
- 运维：`ops / Ops@2026`

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
docker compose up -d --build
```

默认访问：

- 前端：http://localhost:18081
- 后端：http://localhost:18080/actuator/health

## 安全边界

- 生产环境写操作默认禁止。
- 系统里的切绿、回滚先只记录治理状态，不直接执行服务器命令。
- 真实执行前必须人工确认命令、备份、回滚脚本和责任人。
- 数据量报告只保存摘要，不保存课程、用户等敏感业务明细。

## 已内置的上线规则

- 每个变更单至少两个组内评审人。
- 常规上线和紧急上线都需要觉哥最终确认。
- 如果开发人参与评审，必须先记录向另一位评审演示确认。
- 绿环境先发布，功能测试和数据量对比都通过后才允许切绿。
- 发现问题可以立即记录回蓝或按节点回滚。
- 新增组件必须填写配置目录、数据目录和部署目录。

## 主项目基线

- 后端：`juege-osh/osh-backend` 的 `release/20260708`，提交 `8d2ee47cbb7dccaffabad8822ceadc8ccb7b8cb7`
- 前端：`juege-osh/osh-frontend` 的 `release/20260708`，提交 `868b87f55a7c267e40bf37d4fd0573973ae75ba3`
