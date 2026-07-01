# 发布包说明

治理台每次上线都要有可追溯发布包。不要临时拷贝前端 dist 或后端 jar。

## 打包

```bash
scripts/preflight.sh
scripts/build_release_bundle.sh
scripts/verify_release_bundle.sh release-bundles/osh-prod-release-*.tar.gz
```

生成文件：

- `release-bundles/osh-prod-release-*.tar.gz`
- `release-bundles/osh-prod-release-*.tar.gz.sha256`

## 包内结构

- `source/`：源码、Docker Compose、脚本、文档和 `.env.example`。
- `artifacts/backend/app.jar`：本次构建出来的后端 jar。
- `artifacts/frontend/dist/`：本次构建出来的前端静态文件。
- `RELEASE_MANIFEST.md`：构建时间、分支、commit 和部署检查项。
- `SHA256SUMS`：包内文件 sha256。

真实 `.env` 不会进包。服务器只保留本地 `.env`。

## 上线前检查

上线治理台前，至少确认：

- `scripts/preflight.sh` 通过。
- 发布包外层 sha256 通过。
- 包内 `SHA256SUMS` 通过。
- 服务器本地 `.env` 没有被覆盖。
- PostgreSQL 治理库已备份。
- `docker compose config` 通过。
- `/actuator/health` 正常。
- 登录、创建变更单、单项分析、dry-run、执行记录、验证记录、报告闸门和回滚记录都测过。

## 回滚

治理台发版异常时，只回滚治理台，不碰主业务系统。

1. 回到上一版发布包解压目录。
2. 复用服务器本地 `.env`。
3. 执行 `docker compose up -d --build`。
4. 检查 `/actuator/health`。
5. 登录后确认变更单和报告还能打开。

涉及数据库迁移时先备份，再判断能不能回退代码。不要直接删除治理库数据。
