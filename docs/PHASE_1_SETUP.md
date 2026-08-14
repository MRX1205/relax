# 阶段 1 运行与验收说明

> 状态：阶段 1 工程实现完成，微信体验版和容器实启需在对应工具可用后验收  
> 更新日期：2026-08-14

## 1. 范围

阶段 1 只提供项目骨架与基础设施，不包含真实登录、角色授权、项目/技师管理、订单、支付或结算业务。

已建立：

- 原生 TypeScript 微信小程序，以及用户、技师、管理员三个分包入口。
- 请求封装、环境映射、基础按钮/表单/状态组件和后端连接检查页。
- Java 21、Spring Boot 模块化单体、统一响应/异常、requestId、健康检查和 OpenAPI。
- MySQL、Redis、Flyway、Docker Compose、Nginx、备份/恢复脚本和 CI 检查。

## 2. 本地依赖

- Node.js 20、npm 11。
- Java 21。Maven 由 `server/mvnw` 固定为 3.9.11，无需单独安装。
- Docker Desktop 与 Docker Compose，用于完整基础设施运行。
- 微信开发者工具，用于导入和预览小程序。

## 3. 自动化检查

```bash
cd miniapp
npm ci
npm run check
npm audit --audit-level=high --registry=https://registry.npmjs.org

cd ../server
./mvnw verify

cd ..
docker compose --file deploy/docker-compose.yml config --quiet
bash -n deploy/scripts/backup-mysql.sh deploy/scripts/restore-mysql.sh
```

`npm run check` 同时执行 TypeScript 类型检查和 Vitest 基础测试。`./mvnw verify` 会使用 H2 测试配置启动 Spring 上下文、运行 Flyway 基线迁移并测试健康接口。

## 4. 启动完整本地环境

先创建仅供本地使用的环境文件，并修改其中的示例密码：

```bash
cp .env.example deploy/.env
cd deploy
docker compose up --build -d
docker compose ps
curl http://127.0.0.1:8080/api/v1/health
```

预期健康接口返回 `code: "OK"`、`data.status: "UP"`，响应头包含 `X-Request-Id`。OpenAPI 页面为 `http://127.0.0.1:8080/swagger-ui.html`。

停止环境：

```bash
cd deploy
docker compose down
```

不要在仍需保留本地数据时增加 `--volumes`。

## 5. 单独启动后端

本机 MySQL 与 Redis 已启动、数据库和账号与 `application-local.yml` 默认值一致时：

```bash
cd server
./mvnw spring-boot:run
```

如配置不同，通过 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD` 覆盖。`staging` 和 `production` 配置不提供密码默认值，必须由部署环境注入。

## 6. 导入微信小程序

1. 在微信开发者工具中导入 `miniapp` 目录。
2. 本地骨架使用 `touristappid`；获得正式 AppID 后写入开发者私有配置，不提交密钥。
3. 执行编译，确认首页和三个角色工作台分包均可打开。
4. 后端启动后，在首页点击“检查服务”，确认显示 `relax-server 已连接`。

本地开发地址是 `http://127.0.0.1:8080`。体验版和正式版发布前，必须将 `miniapp/miniprogram/config/api-environment.ts` 中的占位域名替换为已备案且配置 HTTPS 的合法域名，并在微信公众平台配置 request 合法域名。

## 7. 数据库备份与恢复

容器运行时执行：

```bash
deploy/scripts/backup-mysql.sh
deploy/scripts/restore-mysql.sh deploy/backups/<备份文件>.sql.gz
```

恢复会覆盖目标数据库中的同名对象，只能在已确认的目标环境执行。

## 8. 阶段 1 验收状态

| 验收项 | 当前状态 |
| --- | --- |
| 小程序类型检查与基础测试 | 已通过 |
| 后端上下文、Flyway、健康接口与打包 | 已通过 |
| Docker Compose 静态配置 | 已通过 |
| 备份/恢复脚本语法 | 已通过 |
| 生产密钥扫描 | 已通过，仓库只保留示例占位值 |
| GitHub Actions 检查工作流 | 文件已配置；待项目方确认初始化 Git 仓库并关联远程仓库后实跑 |
| 测试环境自动部署 | 待提供测试服务器、域名和部署密钥后接通 |
| Docker 镜像构建与容器实启 | 待 Docker Desktop 启动后执行 |
| 微信开发者工具编译与体验版联调 | 待安装工具、提供 AppID 和测试域名后执行 |

阶段 2 未获确认前，不开发登录、权限、区域和文件业务。
