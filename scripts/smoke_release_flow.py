#!/usr/bin/env python3
import json
import os
import sys
from datetime import datetime, timezone
from urllib.error import HTTPError, URLError
from urllib.parse import urlparse
from urllib.request import Request, urlopen


API_BASE = os.getenv("OSH_RELEASE_API_BASE", "http://127.0.0.1:18080/api").rstrip("/")
USERNAME = os.getenv("OSH_RELEASE_USERNAME", "juege")
PASSWORD = os.getenv("OSH_RELEASE_PASSWORD") or os.getenv("OSH_SEED_JUEGE_PASSWORD") or ""
COMPONENTS = [
    item.strip()
    for item in os.getenv(
        "OSH_RELEASE_COMPONENTS",
        "mysql,redis,nacos,zookeeper,kafka,elasticsearch,kibana,hbase,xxl-job,flink,java-backend,vue-frontend,filebeat,otel-collector,secret-manager,nginx,docker-compose,qdrant,mongodb",
    ).split(",")
    if item.strip()
]
TARGET_ENV = os.getenv("OSH_RELEASE_TARGET_ENV", "prod")
TARGET_COLOR = os.getenv("OSH_RELEASE_TARGET_COLOR", "green")
PROJECT_BRANCH = os.getenv("OSH_RELEASE_PROJECT_BRANCH", "release/20260708")


class SmokeError(Exception):
    pass


def ensure_safe_target():
    host = urlparse(API_BASE).hostname or ""
    local_hosts = {"127.0.0.1", "localhost", "::1"}
    if host not in local_hosts and os.getenv("OSH_RELEASE_ALLOW_REMOTE_SMOKE") != "1":
        raise SmokeError(
            "API 地址不是 localhost。若确认要写入远程治理台演练数据，请设置 OSH_RELEASE_ALLOW_REMOTE_SMOKE=1。"
        )


def require_password():
    if not PASSWORD:
        raise SmokeError("请先设置 OSH_RELEASE_PASSWORD，脚本不会读取或打印真实密码。")


def api(method, path, token=None, body=None, allow_failure=False):
    payload = None
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    if body is not None:
        payload = json.dumps(body, ensure_ascii=False).encode("utf-8")
    request = Request(API_BASE + path, data=payload, headers=headers, method=method)
    try:
        with urlopen(request, timeout=30) as response:
            return parse_response(response.read(), response.status, allow_failure)
    except HTTPError as error:
        if allow_failure:
            return parse_response(error.read(), error.code, True)
        raise SmokeError(read_error(error)) from error
    except URLError as error:
        raise SmokeError("无法连接治理台 API：" + str(error.reason)) from error


def parse_response(raw, status, allow_failure):
    text = raw.decode("utf-8", errors="replace")
    try:
        payload = json.loads(text)
    except json.JSONDecodeError as error:
        raise SmokeError("API 返回不是 JSON：" + text[:200]) from error
    success = bool(payload.get("success"))
    if status >= 400 or not success:
        if allow_failure:
            return {"ok": False, "status": status, "message": payload.get("message", ""), "payload": payload}
        raise SmokeError(payload.get("message") or "API 请求失败")
    return payload.get("data")


def read_error(error):
    text = error.read().decode("utf-8", errors="replace")
    try:
        payload = json.loads(text)
        return payload.get("message") or text
    except json.JSONDecodeError:
        return text or str(error)


def assert_status(detail, expected):
    actual = detail.get("status")
    if actual != expected:
        raise SmokeError("状态不符合预期：期望 %s，实际 %s" % (expected, actual))


def reviewer(name):
    display_names = {
        "reviewer_a": "评审 A",
        "reviewer_b": "评审 B",
        "juege": "觉哥",
        "ops": "运维同学",
    }
    return {"reviewerUsername": name, "reviewerDisplayName": display_names.get(name, name)}


def main():
    ensure_safe_target()
    require_password()
    if not COMPONENTS:
        raise SmokeError("OSH_RELEASE_COMPONENTS 不能为空。")

    print("API:", API_BASE)
    print("组件数:", len(COMPONENTS))

    login = api("POST", "/auth/login", body={"username": USERNAME, "password": PASSWORD})
    token = login["token"]
    print("登录账号:", login["username"])

    change = api(
        "POST",
        "/changes",
        token=token,
        body={
            "title": "API 冒烟演练 " + datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S"),
            "projectBranch": PROJECT_BRANCH,
            "releaseType": "NORMAL",
            "targetEnvCode": TARGET_ENV,
            "targetColor": TARGET_COLOR,
            "developerUsername": "reviewer_a",
            "developerDisplayName": "评审 A",
            "demoRequired": True,
            "riskLevel": "HIGH",
            "summary": "API 冒烟演练，只写治理库，不执行生产 SSH、网关切流或业务库写操作。",
            "componentKeys": COMPONENTS,
            "contentJson": json.dumps({"smoke": True, "protectedModules": ["course", "user"]}, ensure_ascii=False),
        },
    )
    change_id = change["id"]
    print("变更单:", change["changeCode"], "ID:", change_id)

    for payload in release_payloads():
        change = api("POST", f"/changes/{change_id}/items", token=token, body=payload)
    print("已加入真实上线项:", len(release_payloads()), "类专用动作")

    api("POST", f"/changes/{change_id}/submit", token=token, body={})
    api(
        "POST",
        f"/changes/{change_id}/demo",
        token=token,
        body={
            "actorUsername": "reviewer_a",
            "actorDisplayName": "评审 A",
            "reviewerUsername": "reviewer_b",
            "reviewerDisplayName": "评审 B",
            "comment": "API 冒烟：评审 A 已向评审 B 演示。",
        },
    )
    for name in ("reviewer_a", "reviewer_b", "juege"):
        body = reviewer(name)
        body.update({"passed": True, "comment": "API 冒烟审核通过"})
        api("POST", f"/changes/{change_id}/approve", token=token, body=body)

    detail = api("GET", f"/changes/{change_id}", token=token)
    for item in detail["items"]:
        for reviewer_name, env_code in (("reviewer_a", "test"), ("reviewer_b", "prod-green")):
            body = reviewer(reviewer_name)
            body.update(
                {
                    "itemId": item["id"],
                    "componentKey": item["componentKey"],
                    "testType": "FUNCTION",
                    "environmentCode": env_code,
                    "passed": True,
                    "demoObserved": True,
                    "responsibilityAccepted": True,
                    "evidence": "API 冒烟：%s 已完成 %s 验证。" % (body["reviewerDisplayName"], item["componentName"]),
                }
            )
            api("POST", f"/changes/{change_id}/reviewer-test", token=token, body=body)

    api("POST", f"/changes/{change_id}/validate-specs", token=token, body={})
    blocked_function = api("POST", f"/changes/{change_id}/test/function", token=token, body={}, allow_failure=True)
    if blocked_function.get("ok", True):
        raise SmokeError("缺少单项 dry-run/执行/验证时，功能测试不应该通过。")
    print("单项闸门已拦截:", blocked_function.get("message"))

    detail = api("GET", f"/changes/{change_id}", token=token)
    complete_item_operations(change_id, token, detail["items"])
    api("POST", f"/changes/{change_id}/test/function", token=token, body={})
    api("POST", f"/changes/{change_id}/test/data", token=token, body={})

    reports = api("GET", f"/changes/{change_id}/reports", token=token)
    if reports.get("readyForGreen"):
        raise SmokeError("缺少 ENV_DIFF/ANNOUNCE 时不应该允许切绿。")
    blocked = api("POST", f"/changes/{change_id}/switch/green", token=token, body={}, allow_failure=True)
    if blocked.get("ok", True):
        raise SmokeError("切绿闸门未生效：缺报告时竟然成功。")
    print("切绿闸门已拦截:", blocked.get("message"))

    api("POST", f"/changes/{change_id}/test/env-diff", token=token, body={})
    api("POST", f"/changes/{change_id}/test/announce", token=token, body={})
    reports = api("GET", f"/changes/{change_id}/reports", token=token)
    if not reports.get("readyForGreen") or reports.get("blockers"):
        raise SmokeError("报告齐全后仍不能切绿：" + json.dumps(reports.get("blockers"), ensure_ascii=False))

    detail = api("POST", f"/changes/{change_id}/switch/green", token=token, body={})
    assert_status(detail, "SWITCHED")
    detail = api(
        "POST",
        f"/changes/{change_id}/verify/manual",
        token=token,
        body={"actorUsername": "ops", "actorDisplayName": "运维同学", "comment": "API 冒烟：绿系统人工验证通过。"},
    )
    assert_status(detail, "VERIFIED")
    detail = api(
        "POST",
        f"/changes/{change_id}/sync/blue",
        token=token,
        body={"actorUsername": "ops", "actorDisplayName": "运维同学", "comment": "API 冒烟：同步蓝系统记录完成。"},
    )
    assert_status(detail, "RELEASED")
    detail = api("POST", f"/changes/{change_id}/switch/blue", token=token, body={})
    assert_status(detail, "ROLLED_BACK")
    detail = api("POST", f"/changes/{change_id}/rollback", token=token, body={})
    assert_status(detail, "ROLLED_BACK")

    print("冒烟完成，最终状态:", detail["status"])
    print("课程/用户业务数据：脚本未连接业务库，只写治理库演练记录。")


def complete_item_operations(change_id, token, items):
    for item in items:
        api("POST", f"/changes/{change_id}/items/{item['id']}/analyze", token=token, body={})
        for action, result in (
            ("dry-run", "API 冒烟 dry-run 通过"),
            ("execute", "API 冒烟绿环境执行记录"),
            ("verify", "API 冒烟验证通过"),
        ):
            api(
                "POST",
                f"/changes/{change_id}/items/{item['id']}/{action}",
                token=token,
                body={
                    "actorUsername": "ops",
                    "actorDisplayName": "运维同学",
                    "environmentCode": TARGET_ENV,
                    "targetColor": TARGET_COLOR,
                    "result": result,
                    "evidence": "%s %s，安全模式只写治理库。" % (item["title"], result),
                    "passed": True,
                    "safeMode": True,
                },
            )
    print("已完成单项分析、dry-run、执行记录和验证")


def release_payloads():
    return [
        {
            "itemType": "MYSQL_SQL",
            "componentKey": "mysql",
            "title": "SQL 上线：订单状态字段",
            "payloadPath": "mysql/release/20260708/add-order-status.sql",
            "changeContent": "新增订单状态字段，不改课程和用户模块。",
            "executionContent": "alter table order_info add column status varchar(32) default 'CREATED';",
            "rollbackContent": "alter table order_info drop column status;",
            "incrementalPlan": "先备份表结构，绿环境 dry-run，再执行 SQL。",
            "rollbackPlan": "确认应用退回旧版本后执行回滚 SQL。",
            "codeChangeSummary": "后端读取 status 字段，老数据默认 CREATED。",
            "riskAnalysis": "风险在订单查询兼容性；课程和用户模块不受影响。",
            "bugAnalysis": "注意默认值、老版本代码兼容、索引影响和回滚时序。",
            "verificationCommands": "select count(*) from order_info where status is null;",
            "testPlan": "验证订单列表、订单详情、回滚 SQL。",
            "dataProbePlan": "对比 order_info 行数；课程和用户表变化必须为 0。",
        },
        {
            "itemType": "NACOS_CONFIG",
            "componentKey": "nacos",
            "title": "配置上线：订单超时配置",
            "payloadPath": "/data/osh/config/nacos/order-timeout.yaml",
            "changeContent": "调整订单超时配置，只先上绿环境。",
            "executionContent": "order.timeoutSeconds: 900",
            "rollbackContent": "order.timeoutSeconds: 600",
            "incrementalPlan": "先 diff，再发布绿环境配置，观察接口返回。",
            "rollbackPlan": "恢复上一版 nacos 配置并刷新服务。",
            "codeChangeSummary": "非代码变更；后端读取原配置 key。",
            "riskAnalysis": "风险在超时时间变化影响订单释放。",
            "bugAnalysis": "注意配置 key 拼写、缓存刷新和灰度范围。",
            "verificationCommands": "curl -fsS http://127.0.0.1:18080/actuator/health",
            "testPlan": "验证配置读取、订单超时逻辑和回滚配置。",
            "dataProbePlan": "对比订单状态数量；课程和用户表变化必须为 0。",
        },
        {
            "itemType": "ES_INDEX",
            "componentKey": "elasticsearch",
            "title": "ES 上线：订单搜索索引",
            "payloadPath": "es/order_search_v20260708",
            "changeContent": "新增订单搜索索引和别名，不改课程和用户模块。",
            "executionContent": "PUT /order_search_v20260708\n{\"settings\":{\"number_of_shards\":1},\"mappings\":{\"properties\":{\"orderId\":{\"type\":\"keyword\"}}}}\nPOST /_aliases 切别名",
            "rollbackContent": "POST /_aliases 切回旧索引；确认后删除 order_search_v20260708。",
            "incrementalPlan": "先创建新索引，导入测试数据，绿环境验证后再切 alias。",
            "rollbackPlan": "alias 切回旧索引，再删除新索引。",
            "codeChangeSummary": "搜索接口继续读 alias，不直接写死新索引。",
            "riskAnalysis": "风险在 mapping 不兼容、alias 指错和 reindex 漏字段；课程和用户模块不受影响。",
            "bugAnalysis": "注意 mapping 类型、分片数、排序字段和查询性能。",
            "verificationCommands": "GET _cat/indices\nGET _alias/order_search",
            "testPlan": "验证索引创建、alias、搜索结果和回切。",
            "dataProbePlan": "对比 ES 文档数；课程和用户表变化必须为 0。",
        },
        {
            "itemType": "KAFKA_TOPIC",
            "componentKey": "kafka",
            "title": "Kafka 上线：订单事件 Topic",
            "payloadPath": "topic: osh.order.event.v1",
            "changeContent": "新增订单事件 topic，不改现有消费者。",
            "executionContent": "kafka-topics --create --topic osh.order.event.v1 --partitions 3 --replication-factor 1",
            "rollbackContent": "确认无生产消费后删除或停用 osh.order.event.v1。",
            "incrementalPlan": "先 describe 确认不存在，再绿环境小流量生产消费。",
            "rollbackPlan": "停生产者消费者，再删除或禁用新 topic。",
            "codeChangeSummary": "生产者后续切到新 topic，消费者保持兼容。",
            "riskAnalysis": "风险在分区数不可减少、消费组 lag 和重复消费。",
            "bugAnalysis": "注意 topic 重名、retention 配置和消费组并发。",
            "verificationCommands": "kafka-topics --describe --topic osh.order.event.v1",
            "testPlan": "验证 topic、分区、副本和消费组 lag。",
            "dataProbePlan": "对比 topicsChanged 和 lagDelta；课程和用户表变化必须为 0。",
        },
        {
            "itemType": "HBASE_DDL",
            "componentKey": "hbase",
            "title": "HBase 上线：订单明细表",
            "payloadPath": "hbase:osh:order_detail_v1",
            "changeContent": "新增 HBase 订单明细表，不改课程和用户模块。",
            "executionContent": "create_namespace 'osh'\ncreate 'osh:order_detail_v1', {NAME => 'cf', VERSIONS => 1}",
            "rollbackContent": "disable 'osh:order_detail_v1'\ndrop 'osh:order_detail_v1'",
            "incrementalPlan": "先 exists/describe，再绿环境建表并抽样读写。",
            "rollbackPlan": "确认无写入依赖后 disable/drop 新表。",
            "codeChangeSummary": "后续任务写新表，老链路保持不变。",
            "riskAnalysis": "风险在列族配置、预分区和 region 影响；课程和用户模块不受影响。",
            "bugAnalysis": "注意列族名、TTL、压缩配置和 disable 表影响。",
            "verificationCommands": "exists 'osh:order_detail_v1'\ndescribe 'osh:order_detail_v1'",
            "testPlan": "验证表结构、列族和抽样 rowkey 查询。",
            "dataProbePlan": "对比 HBase tableDelta/rowDelta；课程和用户表变化必须为 0。",
        },
        {
            "itemType": "XXLJOB_TASK",
            "componentKey": "xxl-job",
            "title": "XXLJob 上线：订单补偿任务",
            "payloadPath": "job: orderCompensateJob",
            "changeContent": "新增订单补偿任务，默认停用，人工验证后再启用。",
            "executionContent": "jobHandler=orderCompensateJob\ncron=0 */5 * * * ?\nroute=FIRST\nblock=SERIAL_EXECUTION",
            "rollbackContent": "停用 orderCompensateJob，恢复旧 cron/handler。",
            "incrementalPlan": "测试环境手动触发一次，绿环境默认停用上线。",
            "rollbackPlan": "停用任务，确认没有补偿中的批次。",
            "codeChangeSummary": "任务 handler 已随后端发布，逻辑需幂等。",
            "riskAnalysis": "风险在误触发、重复补偿、失败重试和幂等。",
            "bugAnalysis": "注意 cron、路由策略、阻塞策略和失败重试。",
            "verificationCommands": "检查 XXLJob 执行日志；手动触发一次测试任务。",
            "testPlan": "验证任务保存、手动触发、日志和停用回滚。",
            "dataProbePlan": "对比订单补偿数量；课程和用户表变化必须为 0。",
        },
        {
            "itemType": "CODE",
            "componentKey": "java-backend",
            "title": "代码上线：订单状态兼容",
            "payloadPath": "release/20260708",
            "changeContent": "发布订单状态兼容逻辑。",
            "executionContent": "branch: release/20260708\ncommitRange: latest release diff\nartifact: backend.jar",
            "rollbackContent": "rollbackCommit: previous release\nartifact: previous backend.jar",
            "incrementalPlan": "构建后只发绿环境，跑接口和数据对比。",
            "rollbackPlan": "绿环境异常先回蓝，再回滚 backend.jar。",
            "codeChangeSummary": "订单查询增加 status 字段兼容；不改课程和用户模块。",
            "riskAnalysis": "风险在空值兼容、老版本前端字段展示和 SQL 性能。",
            "bugAnalysis": "疑似 bug：空 status、枚举不匹配、缓存旧对象。",
            "verificationCommands": "mvn -q -pl backend test",
            "testPlan": "验证订单查询、详情、列表分页和回滚包。",
            "dataProbePlan": "订单行数不变；课程和用户表变化必须为 0。",
        },
    ] + extra_release_payloads()


def extra_release_payloads():
    return [
        typed_payload(
            "REDIS_SCRIPT",
            "redis",
            "Redis 上线：订单缓存脚本",
            "redis:order:cache:*",
            "# redis-cli --scan --pattern 'order:cache:*'\nEVAL \"return redis.call('set', KEYS[1], ARGV[1], 'EX', 300)\" 1 order:cache:demo demo",
            "DEL order:cache:demo",
            "Redis key/script 变更，重点看 key 前缀、TTL、Lua 幂等和误删风险。",
            "redis-cli --scan --pattern 'order:cache:*'\nredis-cli TTL order:cache:demo",
        ),
        typed_payload(
            "REDIS_CONFIG",
            "redis",
            "Redis 配置：内存策略",
            "/data/osh/config/redis/redis.conf",
            "maxmemory-policy allkeys-lru",
            "恢复上一版 maxmemory-policy",
            "Redis 配置变更，重点看重启范围、持久化、淘汰策略和 key 数变化。",
            "redis-cli CONFIG GET maxmemory-policy\nredis-cli INFO memory",
        ),
        typed_payload(
            "KAFKA_CONFIG",
            "kafka",
            "Kafka 配置：Topic retention",
            "topic: osh.order.event.v1 retention.ms",
            "kafka-configs --alter --topic osh.order.event.v1 --add-config retention.ms=604800000",
            "kafka-configs --alter --topic osh.order.event.v1 --delete-config retention.ms",
            "Kafka 配置变更，重点看 broker/topic 配置、消费者 lag 和保留时间。",
            "kafka-configs --describe --topic osh.order.event.v1\nkafka-consumer-groups --all-groups --describe",
        ),
        typed_payload(
            "ZOOKEEPER_CONFIG",
            "zookeeper",
            "Zookeeper 配置：会话超时",
            "/data/osh/config/zookeeper/zoo.cfg",
            "tickTime=2000\nmaxClientCnxns=120",
            "恢复上一版 zoo.cfg",
            "Zookeeper 配置变更，重点看 quorum、session 抖动和 Kafka 依赖。",
            "echo ruok | nc 127.0.0.1 2181",
        ),
        typed_payload(
            "ES_CONFIG",
            "elasticsearch",
            "ES 配置：慢查询日志",
            "/data/osh/config/es/elasticsearch.yml",
            "index.search.slowlog.threshold.query.warn: 2s",
            "恢复上一版 elasticsearch.yml",
            "ES 配置变更，重点看滚动重启、集群健康、查询和写入影响。",
            "GET _cluster/health\nGET _nodes/settings",
        ),
        typed_payload(
            "KIBANA_CONFIG",
            "kibana",
            "Kibana 配置：basePath",
            "/data/osh/config/kibana/kibana.yml",
            "server.publicBaseUrl: https://osh.lol/kibana",
            "恢复上一版 kibana.yml",
            "Kibana 配置变更，重点看 ES 地址、basePath 和入口可用性。",
            "curl -I http://127.0.0.1:5601",
        ),
        typed_payload(
            "HBASE_CONFIG",
            "hbase",
            "HBase 配置：regionserver 参数",
            "/data/osh/config/hbase/hbase-site.xml",
            "<property><name>hbase.client.retries.number</name><value>5</value></property>",
            "恢复上一版 hbase-site.xml",
            "HBase 配置变更，重点看 regionserver 滚动、meta 可用性和客户端超时。",
            "hbase shell status\ndescribe 'osh:order_detail_v1'",
        ),
        typed_payload(
            "XXLJOB_CONFIG",
            "xxl-job",
            "XXLJob 配置：执行器注册",
            "/data/osh/config/xxl-job/application.properties",
            "xxl.job.executor.appname=osh-order",
            "恢复上一版 application.properties",
            "XXLJob 配置变更，重点看执行器注册、任务误触发和日志路径。",
            "检查 XXLJob admin 执行器在线\n手动触发测试任务",
        ),
        typed_payload(
            "FLINK_JOB",
            "flink",
            "Flink 上线：订单流任务",
            "flink/jobs/order-stream.jar",
            "flink run -d -p 2 -s savepoint-path order-stream.jar",
            "停止新 job，从上一版 savepoint 恢复旧 job",
            "Flink 任务变更，重点看 savepoint、checkpoint、状态兼容和重复消费。",
            "Flink UI 检查 job running\n检查 checkpoint 和 Kafka lag",
        ),
        typed_payload(
            "FLINK_CONFIG",
            "flink",
            "Flink 配置：checkpoint",
            "/data/osh/config/flink/flink-conf.yaml",
            "execution.checkpointing.interval: 60s",
            "恢复上一版 flink-conf.yaml",
            "Flink 配置变更，重点看 JM/TM 重启、slot、checkpoint 路径和任务恢复。",
            "Flink UI 检查集群\n检查 JM/TM 日志",
        ),
        typed_payload(
            "FILEBEAT_CONFIG",
            "filebeat",
            "Filebeat 配置：订单日志采集",
            "/data/osh/config/filebeat/filebeat.yml",
            "paths:\n  - /data/osh/logs/order/*.log",
            "恢复上一版 filebeat.yml",
            "Filebeat 配置变更，重点看采集路径、重复采集、索引和磁盘压力。",
            "filebeat test config\n检查 ES 日志索引",
        ),
        typed_payload(
            "OTEL_CONFIG",
            "otel-collector",
            "OTel 配置：采样率",
            "/data/osh/config/otel-collector/config.yaml",
            "processors:\n  probabilistic_sampler:\n    sampling_percentage: 20",
            "恢复上一版 OTel collector 配置",
            "OTel 配置变更，重点看 exporter 地址、采样率和丢数据风险。",
            "otelcol --config config.yaml validate\n检查 collector 日志",
        ),
        typed_payload(
            "SECRET_CONFIG",
            "secret-manager",
            "密钥配置：订单服务 key 版本",
            "/data/osh/config/secret-manager/application.yml",
            "只记录 key 名和版本：order.service.secret.version=v2",
            "恢复到 order.service.secret.version=v1",
            "密钥配置变更不能保存明文，重点看 key 名、版本和调用方刷新。",
            "只读检查 key 是否存在\n确认日志无密钥明文",
        ),
        typed_payload(
            "NGINX_CONFIG",
            "nginx",
            "Nginx 配置：订单接口路由",
            "/data/osh/config/nginx/conf.d/osh.conf",
            "location /api/order/ { proxy_pass http://osh-g-backend:8080; }",
            "恢复上一版 upstream/location",
            "Nginx 配置变更，重点看 nginx -t、location 顺序、证书和蓝绿 upstream。",
            "nginx -t\ncurl -I https://osh.lol/",
        ),
        typed_payload(
            "COMPOSE_CHANGE",
            "docker-compose",
            "Compose 上线：新增隔离组件",
            "/data/osh/compose/docker-compose.yml",
            "services:\n  osh-demo-worker:\n    image: busybox\n    command: ['sh','-c','sleep 3600']",
            "移除 osh-demo-worker 服务并清理隔离目录",
            "Compose 变更，重点看端口冲突、数据目录、配置目录和 healthcheck。",
            "docker compose config\ndocker compose ps",
        ),
        typed_payload(
            "QDRANT_COLLECTION",
            "qdrant",
            "Qdrant 上线：订单向量集合",
            "qdrant/order_vector_v1",
            "PUT /collections/order_vector_v1\n{\"vectors\":{\"size\":1024,\"distance\":\"Cosine\"}}",
            "切回旧 collection alias；确认后删除 order_vector_v1",
            "Qdrant collection 变更，重点看向量维度、alias、索引参数和查询性能。",
            "GET /collections\n查询测试向量\n确认 alias 指向",
        ),
        typed_payload(
            "QDRANT_CONFIG",
            "qdrant",
            "Qdrant 配置：存储参数",
            "/data/osh/config/qdrant/config.yaml",
            "storage:\n  optimizers:\n    deleted_threshold: 0.2",
            "恢复上一版 config.yaml",
            "Qdrant 配置变更，重点看数据目录、端口、内存和 collection 加载。",
            "GET /healthz\nGET /collections",
        ),
        typed_payload(
            "MONGODB_SCRIPT",
            "mongodb",
            "MongoDB 上线：订单扩展索引",
            "mongodb/release/20260708/order-index.js",
            "db.order_ext.createIndex({ orderId: 1 }, { name: 'idx_order_id' })",
            "db.order_ext.dropIndex('idx_order_id')",
            "MongoDB 脚本变更，重点看 collection、索引名、影响文档数和幂等。",
            "db.order_ext.countDocuments()\ndb.order_ext.getIndexes()",
        ),
        typed_payload(
            "SQL",
            "mysql",
            "通用 SQL：临时巡检脚本",
            "mysql/release/20260708/manual-check.sql",
            "select count(*) from order_info;",
            "-- 通用 SQL 只做只读巡检，无需数据回滚；如改数据必须补真实回滚 SQL",
            "通用 SQL 入口用于临时脚本，仍要检查 WHERE、影响行数和课程/用户模块。",
            "select count(*) from order_info;",
        ),
        typed_payload(
            "CONFIG",
            "nacos",
            "通用配置：灰度开关",
            "/data/osh/config/manual/feature-toggle.yaml",
            "order.feature.demoEnabled=false",
            "恢复上一版 feature-toggle.yaml",
            "通用配置入口用于非标准配置，重点看 key 拼写、刷新方式和回滚配置。",
            "读取配置并调用健康检查接口",
        ),
        typed_payload(
            "COMPONENT",
            "docker-compose",
            "通用组件：隔离 worker",
            "/data/osh/compose/manual-component.yml",
            "image=busybox; command='sleep 3600'; healthcheck='true'",
            "停止并移除隔离 worker，清理配置和数据目录",
            "通用组件入口用于非标准组件，重点看配置目录、数据目录、端口、healthcheck 和回滚。",
            "docker compose config\n检查 healthcheck",
        ),
    ]


def typed_payload(item_type, component_key, title, payload_path, execution, rollback, risk, verification):
    return {
        "itemType": item_type,
        "componentKey": component_key,
        "title": title,
        "payloadPath": payload_path,
        "changeContent": title + "。只先上绿环境，不改课程和用户模块。",
        "executionContent": execution,
        "rollbackContent": rollback,
        "incrementalPlan": "先做只读检查和 dry-run，再记录绿环境执行证据。",
        "rollbackPlan": "按节点逆序回滚，恢复上一版配置/脚本/组件，并复查课程和用户模块。",
        "codeChangeSummary": "非代码上线项；如调用方依赖该变更，需要在这里补兼容说明。",
        "riskAnalysis": risk + "；课程和用户模块变化必须为 0。",
        "bugAnalysis": "疑似 bug：配置拼写、脚本幂等、资源路径、回滚内容和执行内容不匹配。",
        "verificationCommands": verification,
        "testPlan": "两位评审分别在测试环境和绿环境验证功能、回滚口径和异常处理。",
        "dataProbePlan": "对比上线前后数量摘要；课程和用户表 added/removed/changed 必须为 0。",
    }


if __name__ == "__main__":
    try:
        main()
    except SmokeError as error:
        print("冒烟失败:", error, file=sys.stderr)
        sys.exit(1)
