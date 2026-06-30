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
    print("已加入真实上线项: MySQL SQL + Nacos 配置 + ES 索引 + Kafka Topic + HBase DDL + XXLJob 任务 + 代码")

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
    ]


if __name__ == "__main__":
    try:
        main()
    except SmokeError as error:
        print("冒烟失败:", error, file=sys.stderr)
        sys.exit(1)
