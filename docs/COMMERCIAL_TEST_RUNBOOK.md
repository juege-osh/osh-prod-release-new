# 完整测试操作文档

这份文档给觉哥自己验收用。目标很简单：你能照着它在治理台里测试 SQL、配置、代码、中间件、新组件、蓝绿切换和回滚。

当前平台默认安全模式。页面里的执行、切绿、同步蓝和回滚只写治理库证据，不会直接 SSH 改生产业务库、网关或真实流量。以后接真实执行器，也必须保留这些闸门。

## 1. 登录和准备

1. 打开 `https://osh.lol/release-console/`。
2. 用 `juege` 登录。
3. 进 `操作手册`，先看一遍上线总顺序。
4. 进 `Change`。
5. 点 `小步演示单`。
6. 右侧详情出现后，看 `上线前还差这些`。

看到以下内容，说明前端是新版：

- 左侧有 `操作手册`。
- Change 详情里有 `上线项`、`新增动作`、`审批测试`、`报告闸门`、`操作链`。
- 每个节点卡片有 `分析`、`dry-run`、`执行`、`验证`、`回滚`、`编辑`。

## 2. 通用上线闭环

每条上线项都按这个顺序走：

1. 新增上线项。
2. 填执行内容。
3. 填回滚内容。
4. 填风险分析。
5. 填疑似 bug 分析。
6. 填验证命令。
7. 点 `分析`。
8. 点 `dry-run`。
9. 点 `执行`。
10. 点 `验证`。
11. 点 `提交评审`。
12. 点 `演示确认`。
13. 点 `评审 A 通过`。
14. 点 `评审 B 通过`。
15. 点 `觉哥确认`。
16. 点 `规范校验`。
17. 点 `补齐评审测试`。
18. 点 `环境差异`。
19. 点 `announce`。
20. 点 `功能测试`。
21. 点 `数据对比`。
22. 点 `切绿`。
23. 点 `生产人工验证`。
24. 点 `同步蓝`。

如果任意一步失败，先看页面顶部错误和 `报告闸门` 的缺口，不要跳步骤。

## 3. SQL 上线测试

入口：`Change -> 新增动作 -> 新增 MySQL SQL`。

示例填写：

```sql
-- 执行 SQL
ALTER TABLE order_info ADD COLUMN release_demo_status VARCHAR(32) DEFAULT 'INIT';
```

```sql
-- 回滚 SQL
ALTER TABLE order_info DROP COLUMN release_demo_status;
```

字段怎么填：

- `载荷路径`：`mysql/release/20260708/order-demo.sql`
- `上线内容`：新增订单演示字段，不影响课程和用户模块。
- `增量计划`：先 explain 或事务回滚演练，再绿环境执行。
- `回滚计划`：确认代码已回旧版本，再执行回滚 SQL。
- `风险分析`：DDL 可能锁表；课程和用户模块变化必须为 0。
- `疑似 bug 分析`：字段默认值、老代码兼容、重复执行、锁等待。
- `验证命令`：查询字段是否存在，检查订单行数，检查课程和用户表变化。
- `数据采集计划`：采集上线前后订单表行数；课程和用户表 added/removed/changed 必须为 0。

测试步骤：

1. 保存子 change。
2. 点 `分析`，看系统是否生成 SQL 风险。
3. 点 `dry-run`。
4. 点 `执行`。
5. 点 `验证`。
6. 点 `规范校验`。
7. 点 `数据对比`。

回滚测试：

1. 在该 SQL 节点点 `回滚`。
2. 再点 `数据对比`。
3. 看课程和用户模块变化是否为 0。

## 4. Nacos 配置上线测试

入口：`新增动作 -> 新增 Nacos 配置`。

示例：

```yaml
order:
  releaseDemoEnabled: false
  timeoutSeconds: 900
```

回滚配置：

```yaml
order:
  releaseDemoEnabled: false
  timeoutSeconds: 600
```

字段怎么填：

- `载荷路径`：`order-service.yaml/DEFAULT_GROUP/prod-green`
- `上线内容`：调整订单超时时间，只先上绿环境。
- `执行内容`：粘贴新配置或 diff。
- `回滚内容`：粘贴上一版配置。
- `风险分析`：配置 key 写错、服务未刷新、蓝绿配置不一致。
- `验证命令`：读取 Nacos 配置，调用订单健康检查接口。

回滚测试：

1. 点该配置项 `回滚`。
2. 确认回滚内容是上一版配置。
3. 再跑 `功能测试`。

## 5. ES 索引上线测试

入口：`新增动作 -> 新增 ES 索引`。

示例执行内容：

```json
PUT /order_search_v20260708
{
  "settings": {
    "number_of_shards": 1
  },
  "mappings": {
    "properties": {
      "orderId": { "type": "keyword" },
      "status": { "type": "keyword" }
    }
  }
}
```

回滚内容：

```text
POST /_aliases 切回旧索引。
确认无流量后删除 order_search_v20260708。
```

必须检查：

- mapping 类型兼容。
- alias 能切回旧索引。
- 文档数一致。
- 搜索接口结果正常。

验证命令：

```text
GET _cluster/health
GET _cat/indices/order_search*
GET _alias/order_search
```

## 6. Kafka Topic 上线测试

入口：`新增动作 -> 新增 Kafka Topic`。

示例执行内容：

```bash
kafka-topics --create \
  --topic osh.order.event.v1 \
  --partitions 3 \
  --replication-factor 1
```

回滚内容：

```bash
先停生产者和消费者。
确认没有业务依赖后删除或保留空 topic。
```

必须检查：

- topic 是否已存在。
- 分区数和副本数。
- retention。
- 消费组 lag。

验证命令：

```bash
kafka-topics --describe --topic osh.order.event.v1
kafka-consumer-groups --all-groups --describe
```

## 7. HBase DDL 上线测试

入口：`新增动作 -> 新增 HBase DDL`。

示例执行内容：

```ruby
create_namespace 'osh'
create 'osh:order_detail_v1', {NAME => 'cf', VERSIONS => 1}
```

回滚内容：

```ruby
disable 'osh:order_detail_v1'
drop 'osh:order_detail_v1'
```

必须检查：

- namespace 是否存在。
- table 是否存在。
- column family 是否正确。
- 回滚不会误删旧表。

验证命令：

```ruby
exists 'osh:order_detail_v1'
describe 'osh:order_detail_v1'
```

当前 docker 盘点没发现 HBase 容器。真实执行前要先确认 HBase 集群位置。

## 8. XXLJob 任务上线测试

入口：`新增动作 -> 新增 XXLJob 任务`。

示例执行内容：

```text
jobHandler=orderCompensateJob
cron=0 */5 * * * ?
route=FIRST
block=SERIAL_EXECUTION
默认状态=停用
```

回滚内容：

```text
停用 orderCompensateJob。
恢复旧 cron、handler 和路由策略。
```

必须检查：

- 默认不要自动启用。
- 手动触发一次。
- 看执行日志。
- 看失败重试策略。
- 任务逻辑必须幂等。

## 9. Redis 变更测试

入口：

- key 或脚本：`新增 Redis 变更`
- 配置：`新增 Redis 配置`

示例执行内容：

```bash
redis-cli --scan --pattern 'order:cache:*'
redis-cli SET order:cache:demo demo EX 300
```

回滚内容：

```bash
redis-cli DEL order:cache:demo
```

必须检查：

- key 前缀不能太宽。
- TTL 是否正确。
- 不要删除不带前缀的 key。
- 是否影响登录、课程、用户缓存。

## 10. 代码发布测试

入口：`新增动作 -> 新增代码发布`。

字段怎么填：

- `载荷路径`：`release/20260708 commit-range`
- `上线内容`：本次发布改了哪些模块。
- `执行内容`：分支、commit 范围、构建产物。
- `回滚内容`：上一版 commit、镜像或 jar/dist 路径。
- `代码/变更大纲`：接口、页面、任务、消息消费、配置变更。
- `疑似 bug 分析`：空值、枚举、缓存、字段兼容、SQL 性能。

验证命令：

```bash
mvn -q -pl backend test
cd frontend && npm run build
curl -fsS http://127.0.0.1:18080/actuator/health
```

如果代码配套 SQL、Nacos、Kafka、ES 变更，要拆成独立上线项。不要全塞到代码发布里。

## 11. 新组件上线测试

入口：`新增动作 -> 新增组件上线`。

适合 MongoDB、独立 worker、新采集器等。

必须填写：

- 镜像。
- 端口。
- 配置目录。
- 数据目录。
- docker-compose diff。
- healthcheck。
- 备份方式。
- 回滚 compose。
- 日志和监控。

验证命令：

```bash
docker compose config
docker compose ps
curl -fsS http://127.0.0.1:PORT/health
```

回滚测试：

1. 停新组件。
2. 恢复上一版 compose。
3. 保留或清理隔离数据目录。
4. 再跑 healthcheck。

## 12. 蓝绿切换测试

切绿前必须全部通过：

- 组件规范报告。
- 环境差异报告。
- announce 报告。
- 功能测试报告。
- 数据量对比报告。
- 每个上线项都有 dry-run、执行记录、验证记录。
- 每个上线项都有两位评审证据。
- 觉哥确认。

操作：

1. 进 `报告闸门`。
2. 看缺口是否清空。
3. 点 `切绿`。
4. 点 `生产人工验证`。
5. 页面、接口、组件都没问题后，点 `同步蓝`。

如果切绿失败，看 blockers。不要绕过后端闸门。

## 13. 回蓝和回滚测试

回蓝：

1. 切绿后发现问题。
2. 点 `回蓝`。
3. 确认状态变成 `ROLLED_BACK`。
4. 再看操作链是否有 `SWITCH_BACK_BLUE`。

单节点回滚：

1. 找到有问题的上线项。
2. 点节点上的 `回滚`。
3. 看操作链是否有 `ITEM_ROLLBACK`。
4. 跑 `功能测试`。
5. 跑 `数据对比`。

整单回滚：

1. 进 `报告闸门`。
2. 点 `整单回滚`。
3. 看所有节点是否标记回滚。
4. 复查课程和用户模块。

## 14. 验收清单

一轮完整验收至少覆盖：

- MySQL SQL 上线和回滚。
- Nacos 配置上线和回滚。
- ES 索引和 alias 回切。
- Kafka Topic 新增和回滚策略。
- HBase DDL 和回滚策略。
- Redis key 或配置变更。
- XXLJob 任务新增和停用回滚。
- 代码发布和旧包回滚。
- 新组件上线和 compose 回滚。
- 蓝绿切换、生产人工验证、同步蓝、回蓝。
- 数据对比里课程和用户模块单独为 0。

## 15. 本地自动验证

开发和发版前跑：

```bash
scripts/preflight.sh
scripts/local_smoke.sh
scripts/build_release_bundle.sh
scripts/verify_release_bundle.sh release-bundles/osh-prod-release-*.tar.gz
```

远程治理台冒烟必须显式确认：

```bash
OSH_RELEASE_ALLOW_REMOTE_SMOKE=1 \
OSH_RELEASE_API_BASE=https://osh.lol/release-console-api \
OSH_RELEASE_PASSWORD='治理台密码' \
python3 scripts/smoke_release_flow.py
```

远程冒烟会写治理库演练记录，不会直接改主业务生产。
