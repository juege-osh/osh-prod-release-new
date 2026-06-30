# 部署和操作说明

## 推荐部署方式

先部署到测试服务器。确认页面、接口、审批流、测试报告都正常后，再考虑放到生产服务器。

```bash
git clone https://github.com/juege-osh/osh-prod-release-new.git
cd osh-prod-release-new
git checkout release/20260708
export OSH_DB_PASSWORD='请换成强密码'
export APP_JWT_SECRET='请换成 32 位以上随机串'
export OSH_RELEASE_WEB_PORT=18081
docker compose up -d --build
```

访问：

- Web：`http://服务器IP:18081`
- Health：`http://服务器IP:18080/actuator/health`

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
2. 系统按组件顺序生成节点。
3. 提交评审。
4. 评审 A、评审 B 分别确认。
5. 觉哥最终确认。
6. 执行功能测试。
7. 执行数据量对比。
8. 绿环境确认没问题后，记录切绿。
9. 负责人在生产人工验证。
10. 如果有问题，立即回蓝或按节点回滚。

## 生产保护

这个版本不会自动 SSH 到生产执行命令。即使页面点了“切绿”或“回滚”，也只是写治理库状态。

要接入真实生产执行，需要再单独加执行器，并至少满足：

- 命令白名单。
- dry-run 结果留档。
- 觉哥二次确认。
- 回滚脚本存在且可执行。
- 课程模块、用户模块快照对比通过。
- 操作日志不打印密码、token、数据库连接串。

## 组件规范

每个组件必须有：

- 配置目录：如 `/data/osh/config/mysql`
- 数据目录：如 `/data/osh/data/mysql`
- 部署目录：如 `/data/osh/compose/mysql`
- 增量脚本。
- 回滚脚本。
- 自动化检查项。
- 责任人和两位评审人。
