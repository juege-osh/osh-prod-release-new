#!/usr/bin/env python3
import re
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]

REQUIRED_TYPES = {
    "MYSQL_SQL",
    "REDIS_SCRIPT",
    "REDIS_CONFIG",
    "NACOS_CONFIG",
    "KAFKA_TOPIC",
    "KAFKA_CONFIG",
    "ZOOKEEPER_CONFIG",
    "ES_INDEX",
    "ES_CONFIG",
    "KIBANA_CONFIG",
    "HBASE_DDL",
    "HBASE_CONFIG",
    "XXLJOB_TASK",
    "XXLJOB_CONFIG",
    "FLINK_JOB",
    "FLINK_CONFIG",
    "CODE",
    "FILEBEAT_CONFIG",
    "OTEL_CONFIG",
    "SECRET_CONFIG",
    "NGINX_CONFIG",
    "COMPOSE_CHANGE",
    "QDRANT_COLLECTION",
    "QDRANT_CONFIG",
    "MONGODB_SCRIPT",
    "SQL",
    "CONFIG",
    "COMPONENT",
}

REQUIRED_RUNBOOK_TERMS = [
    "SQL 上线测试",
    "Nacos 配置上线测试",
    "ES 索引上线测试",
    "Kafka Topic 上线测试",
    "HBase DDL 上线测试",
    "Redis 变更测试",
    "代码发布测试",
    "新组件上线测试",
    "蓝绿切换测试",
    "回蓝和回滚测试",
]


def read(path):
    return (ROOT / path).read_text(encoding="utf-8")


def extract_frontend_types(frontend):
    return set(re.findall(r"value:\s*'([A-Z_]+)'", frontend))


def extract_smoke_types(smoke):
    direct_types = set(re.findall(r'"itemType":\s*"([A-Z_]+)"', smoke))
    helper_types = set(re.findall(r"typed_payload\(\s*\"([A-Z_]+)\"", smoke))
    return direct_types | helper_types


def require(condition, message):
    if not condition:
        raise AssertionError(message)


def main():
    frontend = read("frontend/src/App.vue")
    smoke = read("scripts/smoke_release_flow.py")
    runbook = read("docs/COMMERCIAL_TEST_RUNBOOK.md")
    readme = read("README.md")

    frontend_types = extract_frontend_types(frontend)
    smoke_types = extract_smoke_types(smoke)

    missing_frontend = sorted(REQUIRED_TYPES - frontend_types)
    missing_smoke = sorted(REQUIRED_TYPES - smoke_types)
    require(not missing_frontend, "frontend itemTypeOptions missing: " + ", ".join(missing_frontend))
    require(not missing_smoke, "smoke release payloads missing: " + ", ".join(missing_smoke))

    require("操作手册" in frontend and "runbookGuides" in frontend, "frontend runbook page missing")
    require("releaseOverview" in frontend and "上线总览" in frontend and "上线后结果" in frontend,
            "frontend release overview missing")
    require("docs/COMMERCIAL_TEST_RUNBOOK.md" in readme, "README missing commercial runbook link")

    missing_terms = [term for term in REQUIRED_RUNBOOK_TERMS if term not in runbook]
    require(not missing_terms, "runbook missing sections: " + ", ".join(missing_terms))

    for keyword in ["课程", "用户", "回滚", "dry-run", "切绿"]:
        require(keyword in runbook, "runbook missing keyword: " + keyword)

    print("release runbook validation ok")


if __name__ == "__main__":
    try:
        main()
    except AssertionError as error:
        print("release runbook validation failed:", error, file=sys.stderr)
        sys.exit(1)
