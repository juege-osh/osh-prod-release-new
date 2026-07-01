# 部署和操作说明

## 推荐部署方式

先部署到测试服务器。确认页面、接口、审批流、测试报告都正常后，再考虑放到生产服务器。

```bash
git clone https://github.com/juege-osh/osh-prod-release-new.git
cd osh-prod-release-new
git checkout release/20260708
cp .env.example .env
# 编辑 .env，填好 OSH_DB_PASSWORD、APP_JWT_SECRET 和 4 个治理台账号密码
docker compose up -d --build
```

访问：

- Web：`http://服务器IP:18081`
- Health：`http://服务器IP:18080/actuator/health`

启动后先跑检查：

```bash
scripts/preflight.sh
scripts/local_smoke.sh
OSH_RELEASE_API_BASE=http://127.0.0.1:18080/api \
OSH_RELEASE_PASSWORD='治理台密码' \
python3 scripts/smoke_release_flow.py
```

如果要对远程治理台跑演练，必须额外设置 `OSH_RELEASE_ALLOW_REMOTE_SMOKE=1`，避免误写线上治理记录。

## 发布包方式

治理台自己也要按发布包上线，不建议临时拷贝几个文件。

在本机或 CI 上打包：

```bash
scripts/preflight.sh
scripts/build_release_bundle.sh
scripts/verify_release_bundle.sh release-bundles/osh-prod-release-*.tar.gz
```

发布包里有：

- `source/`：可部署源码、Docker Compose、脚本和文档。
- `artifacts/backend/app.jar`：本次提交构建出的后端 jar。
- `artifacts/frontend/dist/`：本次提交构建出的前端静态文件。
- `RELEASE_MANIFEST.md`：分支、commit、构建时间和部署检查项。
- `SHA256SUMS`：包内文件校验清单。

拷贝到测试服后先校验：

```bash
scp -P 58753 release-bundles/osh-prod-release-*.tar.gz* osh-test:/tmp/
ssh osh-test 'cd /tmp && shasum -a 256 -c osh-prod-release-*.tar.gz.sha256'
```

Linux 服务器如果没有 `shasum`，用：

```bash
ssh osh-test 'cd /tmp && sha256sum -c osh-prod-release-*.tar.gz.sha256'
```

解包部署：

```bash
ssh osh-test
mkdir -p /opt/osh-prod-release-new/releases
cd /opt/osh-prod-release-new/releases
tar -xzf /tmp/osh-prod-release-*.tar.gz
cd osh-prod-release-*/source
cp /opt/osh-prod-release-new/current/.env .env
docker compose config
docker compose up -d --build
curl -fsS http://127.0.0.1:18080/actuator/health
```

第一次部署时没有旧 `.env`，就从 `.env.example` 复制一份，只在服务器本地填真实密码：

```bash
cp .env.example .env
```

生产部署也按同样方式做，但要先在测试服跑通页面、接口、报告、切绿闸门和回滚记录。没有觉哥确认，不做生产写操作。

## 治理台自身回滚

治理台发版失败时，回滚治理台本身，不要碰主业务系统。

推荐方式：

```bash
ssh osh-test
cd /opt/osh-prod-release-new/releases/上一版/source
cp /opt/osh-prod-release-new/current/.env .env
docker compose up -d --build
curl -fsS http://127.0.0.1:18080/actuator/health
```

如果前端页面错乱，优先回滚前端包和反代配置；如果后端接口报错，再回滚后端容器。治理库回滚要谨慎，先备份 PostgreSQL，再确认 Flyway 迁移影响。

## 浏览器连通性检查

2026-06-30 用内置浏览器检查生产 IP `149.88.92.159`：

- `https://osh.lol/` 正常打开，当前是 OSH 主站首页。
- `http://149.88.92.159/` 能打开，但页面为空。
- `https://149.88.92.159/` 因证书域名不匹配被浏览器拦截。
- `http://149.88.92.159:18081/` 连接被拒绝，说明治理台前端还没在生产 IP 监听。
- `http://149.88.92.159:18080/actuator/health` 连接被拒绝，说明治理台后端还没在生产 IP 监听。

结论：生产主站正常，治理台还没有部署到生产可访问端口。

## 生产反代建议

不要把治理台挂到 `https://osh.lol/` 根路径，避免覆盖现有主站。

推荐挂到独立路径：

- `https://osh.lol/release-console/`
- API 路径：`https://osh.lol/release-console-api/`

Nginx 示例：

```nginx
location /release-console/ {
  proxy_pass http://127.0.0.1:18081/;
  proxy_set_header Host $host;
  proxy_set_header X-Real-IP $remote_addr;
  proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
  proxy_set_header X-Forwarded-Proto $scheme;
}

location /release-console-api/ {
  proxy_pass http://127.0.0.1:18080/api/;
  proxy_set_header Host $host;
  proxy_set_header X-Real-IP $remote_addr;
  proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
  proxy_set_header X-Forwarded-Proto $scheme;
}
```

如果前端挂在 `/release-console/`，构建时建议设置：

```bash
export VITE_API_BASE=/release-console-api
export VITE_BASE_PATH=/release-console/
```

再执行：

```bash
docker compose up -d --build
```

## 上线流程

1. 新建变更单，选择组件。
2. 系统按组件顺序生成节点和子 change。
3. 负责人更新每个子 change 的上线内容、增量计划、回滚计划、测试计划和数据采集计划。
4. 执行组件规范校验。
5. 提交评审。
6. 评审 A、评审 B 分别确认。
7. 觉哥最终确认。
8. 两位评审人为每个子 change 提交测试证据。
9. 执行环境差异报告和 announce 检查。
10. 执行功能测试。
11. 执行数据量对比。
12. 绿环境确认没问题后，记录切绿。
13. 负责人在生产绿系统人工验证。
14. 验证通过后记录同步蓝系统。
15. 如果有问题，立即回蓝或按节点回滚。

## 生产保护

这个版本不会自动 SSH 到生产执行命令。即使页面点了“切绿”“同步蓝”或“回滚”，也只是写治理库状态和操作证据。

生产 profile 启动前会检查 `APP_JWT_SECRET`、`OSH_DB_PASSWORD` 和 4 个种子用户密码。少配或使用默认弱值会直接启动失败。本地 `local` profile 只用于演练。

生产上线治理台前，至少确认：

- 发布包校验通过。
- 服务器本地 `.env` 没有被覆盖。
- PostgreSQL 治理库已备份。
- `docker compose config` 通过。
- `/actuator/health` 返回正常。
- `scripts/smoke_release_flow.py` 在允许的测试环境跑通。
- `https://osh.lol/` 主站根路径没有被治理台覆盖。

要接入真实生产执行，需要再单独加执行器，并至少满足：

- 命令白名单。
- dry-run 结果留档。
- 觉哥二次确认。
- 回滚脚本存在且可执行。
- 课程模块、用户模块快照对比通过。
- 操作日志不打印密码、token、数据库连接串。

## 本地冒烟结果

2026-06-30 已跑通本地 API 闭环：

- 新建全组件变更单。
- 提交评审。
- 评审 A、评审 B、觉哥确认。
- 组件规范校验。
- 每个组件生成两条评审测试证据。
- 环境差异报告。
- announce 检查报告。
- 功能测试报告。
- 数据量对比报告。
- 切绿。
- 生产人工验证记录。
- 同步蓝系统记录。
- 按节点回滚。

冒烟数据覆盖 11 个组件、11 个节点、22 条评审测试证据、5 类报告和 7 条操作记录。

## 组件规范

每个组件必须有：

- 配置目录：如 `/data/osh/config/mysql`
- 数据目录：如 `/data/osh/data/mysql`
- 部署目录：如 `/data/osh/compose/mysql`
- 增量脚本。
- 回滚脚本。
- 自动化检查项。
- 责任人和两位评审人。
