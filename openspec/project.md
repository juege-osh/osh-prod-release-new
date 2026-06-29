# OSH Release Governance Project

## 项目定位

这是 OSH 平台的上线治理系统，用来管理主项目 `osh-backend` 和 `osh-frontend` 的发布、审核、测试、蓝绿切换、回滚和报告。

主项目基线来自：

- `osh-backend` 的最新 `release/20260708`
- `osh-frontend` 的最新 `release/20260708`

## 核心目标

- 管理变更单、组件清单、发布节点和责任人。
- 支持 MySQL、Redis、ES、Kafka、HBase、Nacos 等组件的增量上线。
- 支持新增组件，比如 MongoDB。
- 支持细粒度回滚，按节点回滚，按顺序执行。
- 支持蓝绿发布，一键切换。
- 支持双人评审和上线人工复核。
- 支持自动化功能测试、数据量对比和报告汇总。

## 技术边界

- 后端用 Java。
- 前端用 Vue。
- 数据库优先用 PostgreSQL。
- 不直接修改主项目的业务库数据。
- 生产环境的真实数据对比只做读操作和快照比对。

## 变更约定

所有涉及以下内容的修改，都要先建 OpenSpec change：

- 接口
- 数据表
- 权限
- 审批流程
- 发布流程
- 回滚流程
- 自动化测试流程
- 部署方式

