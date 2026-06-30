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
        "mysql,redis,nacos,kafka,elasticsearch,hbase,java-backend,vue-frontend,nginx,docker-compose,mongodb",
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


if __name__ == "__main__":
    try:
        main()
    except SmokeError as error:
        print("冒烟失败:", error, file=sys.stderr)
        sys.exit(1)
