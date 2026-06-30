# 上线平台开发清单

## 目标

这套平台要能承接真实上线，不只是登记流程。觉哥后面要上线 SQL、ES 索引、HBase DDL、Kafka topic、Nacos 配置、XXLJob 任务、代码、组件，都要能在平台里拆开、评审、dry-run、执行留痕、验证和回滚。

生产默认只读。没有觉哥明确确认，平台不直接改生产业务库、网关或真实流量。

## 一次性要补齐的功能

1. 上线项分析
   - SQL：识别 DDL/DML、无 WHERE、drop/truncate/delete、课程/用户表风险。
   - ES：识别 index、mapping、settings、alias、reindex、回切风险。
   - HBase：识别 namespace、table、列族、预分区、disable/drop 回滚。
   - Kafka：识别 topic、分区、副本、retention、消费组 lag。
   - Zookeeper：识别 `zoo.cfg`、quorum、session、Kafka 依赖。
   - XXLJob：识别 jobHandler、cron、路由策略、阻塞策略、停用回滚。
   - Flink：识别 jar、并发、checkpoint/savepoint、状态兼容。
   - Qdrant：识别 collection、向量维度、alias 和回滚 collection。
   - Filebeat/OTel/Secret：识别配置 diff、采集路径、采样率、脱敏密钥配置。
   - 配置：识别配置路径、配置 diff、刷新方式、回滚配置。
   - 代码：记录分支、commit 范围、改动大纲、疑似 bug、构建产物。

2. 分步上线
   - 每个上线项都有独立状态。
   - 状态按 `已分析 -> dry-run 通过 -> 已记录执行 -> 已验证 -> 可回滚` 推进。
   - 每一步写入操作链，保留操作者、环境、颜色、安全模式和详情。

3. 后端闸门
   - 没有规范报告，不能测试。
   - 没有双人评审，不能测试。
   - 没有单项 dry-run、执行记录、验证记录，不能切绿。
   - 没有回滚内容，不能进入上线。

4. 前端工作台
   - Change 详情页要直接看到组件动作入口。
   - 支持 MySQL SQL、ES 索引/配置、HBase DDL/配置、Kafka topic/配置、Nacos 配置、XXLJob 任务、Redis 变更、代码发布、Compose 组件。
   - 组件目录要展示测试服、生产蓝、生产绿只读盘点结果。
   - HBase/MongoDB 这类未发现运行实例的组件，要明确显示“支持治理，实例待确认”。
   - 每行能点分析、dry-run、记录执行、验证、回滚。
   - 表格里显示当前状态、载荷路径、风险摘要。

5. 报告
   - 功能测试报告按上线项列出验证命令和结果。
   - 数据量报告按上线项列出影响模块。
   - 课程模块、用户模块始终单独列 0 变化，除非变更单明确批准。

6. 自动化验证
   - 本地冒烟必须新增 SQL、配置、代码上线项。
   - 冒烟必须走完整分步操作。
   - CI 继续跑 preflight 和 local smoke。

## 不做的事

- 不把 SSH 密码、数据库密码、token 写进仓库。
- 不自动执行生产 SQL。
- 不自动切真实生产网关。
- 不绕过觉哥确认。
