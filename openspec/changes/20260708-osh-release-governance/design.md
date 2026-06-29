# Design: osh-release-governance

## 总体思路

把上线治理拆成四层：

1. 组件目录
2. 变更单
3. 审批与测试
4. 发布与回滚

这样后面加新组件，不需要改整个流程。

## 数据模型

### 组件目录

- `ComponentDefinition`
- 记录组件名、类型、依赖、上线顺序、回滚顺序、配置目录、数据目录、是否支持蓝绿和增量发布

### 变更单

- `ReleaseChange`
- 记录标题、主项目分支、上线类型、目标环境、当前颜色、目标颜色、状态、负责人、风险说明

### 节点计划

- `ReleaseNode`
- 每个变更单拆成多个节点，比如配置、数据库、缓存、消息队列、索引、应用、前端

### 审批记录

- `ReviewRecord`
- 记录评审人、是否通过、是否做过演示、备注、时间

### 测试报告

- `TestReport`
- 记录功能测试、数据量比对、AI 判断、差异摘要

### 快照

- `EnvironmentSnapshot`
- 记录上线前后关键数据快照，只保存摘要和差异，不写业务明细

## API

### 登录

- `POST /api/auth/login`
- `GET /api/auth/me`

### 基础数据

- `GET /api/environments`
- `GET /api/components`
- `GET /api/dashboard/summary`

### 变更单

- `GET /api/changes`
- `POST /api/changes`
- `GET /api/changes/{id}`
- `POST /api/changes/{id}/submit`
- `POST /api/changes/{id}/approve`
- `POST /api/changes/{id}/demo`
- `POST /api/changes/{id}/test/function`
- `POST /api/changes/{id}/test/data`
- `POST /api/changes/{id}/switch/green`
- `POST /api/changes/{id}/switch/blue`
- `POST /api/changes/{id}/rollback`
- `GET /api/changes/{id}/reports`

### 发现和对比

- `GET /api/source/latest`
- `POST /api/source/refresh`
- `POST /api/compare/environment`

## 前端页面

- 登录页
- 总览页
- 变更列表页
- 变更详情页
- 报告页
- 组件目录页

变更详情页要有三种看法：

- 表格
- 树
- 图

## 发布策略

- 常规上线先过组内两审，再过觉哥。
- 紧急上线多一次觉哥确认。
- 功能测试和数据对比都过了，才允许切蓝绿。
- 切换失败时立即回滚到上一色。

## 安全边界

- 只读查看生产环境快照。
- 不把生产密码写进前端。
- 不在公开日志里打印敏感值。

