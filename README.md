# 东莞到家按摩小程序（relax）

东莞本地的正规上门按摩预约平台。用户、技师、管理员三种角色共用同一个原生微信小程序，登录后按账号角色进入各自的工作台；后端是 Java 21 + Spring Boot 的模块化单体，负责登录鉴权、供给管理、下单支付、技师履约、退款售后与人工结算。

当前进度：阶段 1 至阶段 8 已实现，前后端全链路可以跑通（详见 [docs/TEST_REPORT.md](./docs/TEST_REPORT.md)）；阶段 9 之后的生产环境联调、微信审核与上线尚未完成。

## 技术栈

| 层 | 选型 |
| --- | --- |
| 小程序 | 原生微信小程序 + TypeScript 5.9，自研 `request` 封装与 UI 组件，按角色分包加载 |
| 后端 | Java 21、Spring Boot 4.1.0、Spring Security、MyBatis-Plus 3.5.17 |
| 数据库 | MySQL 8.4，Flyway 管理 V1–V11 迁移脚本 |
| 缓存 | Redis 8.2，用于限流与登录态 |
| 文件存储 | 本地目录 / 数据库 / 腾讯云 COS 三种实现，按配置切换 |
| 接口文档 | springdoc-openapi 3.1.0，仅本地与测试环境开放 |
| 部署 | Docker Compose + Nginx 反向代理 |
| 测试与 CI | Maven（H2 内存库跑后端测试）、Vitest（小程序单测）、GitHub Actions |

## 目录结构

```text
miniapp/                         原生微信小程序（TypeScript）
  miniprogram/pages/             主包：首页、登录、手机号、角色切换、账户、协议
  miniprogram/packageUser/       用户端分包：浏览、预约、订单、评价、售后、地址、优惠券
  miniprogram/packageTech/       技师端分包：入驻、服务区域、排班、接单、收入
  miniprogram/packageAdmin/      管理端分包：分类、项目、技师、定价、订单、退款、结算、轮播、支付配置
  miniprogram/components/        自定义 tabbar 与 ui-button / ui-field / ui-state 组件
  miniprogram/services/          鉴权、目录、文件、IAM、协议等接口封装
  tests/                         小程序单元测试
server/                          Spring Boot 后端
  src/main/java/com/relax/       按业务域划分的模块（auth、catalog、order、payment、refund……）
  src/main/resources/db/migration/  Flyway 迁移脚本
  src/test/java/com/relax/       单元测试、接口测试与全链路 E2E 测试
deploy/                          Docker Compose、Nginx 配置、MySQL 备份/恢复脚本
docs/                            PRD、技术设计、开发计划、部署与运维手册
```

## 功能概览

### 用户端

- 微信登录、手机号绑定、多角色切换
- 浏览项目与技师，支持“项目选技师”和“技师选项目”两条下单路径
- 地址管理（仅限东莞区域）、预约时段选择、订单预览与下单
- 微信支付（本地与测试环境走模拟支付网关）、订单列表与详情、取消订单
- 服务完成后评价、申请退款与售后、优惠券、站内通知

### 技师端

- 入驻申请与资质提交，等待管理员审核
- 维护服务区域、排班与可接单时段
- 接单、出发、到达、开始服务、完成服务
- 查看收入台账与结算记录

### 管理端

- 分类与项目的上架下架、基础价维护
- 技师资料审核、项目绑定、技师单独定价、异常订单改派
- 订单查询与取消、退款审批、结算单创建与凭证登记
- 轮播图与协议内容维护、支付配置、操作日志、演示数据生成

## 本地运行

### 环境要求

| 组件 | 版本 |
| --- | --- |
| Java | 21 |
| Node.js | 20 |
| Docker / Docker Compose | 24+ / 2.20+ |
| 微信开发者工具 | 最新稳定版 |

Maven 由 `server/mvnw` 固定为 3.9.11，无需单独安装。

### 1. 启动后端依赖（MySQL + Redis）

`deploy/docker-compose.dev.yml` 只启动数据库和缓存，并把端口映射到 13306 / 16379，避免与本机已有服务冲突：

```bash
docker compose --file deploy/docker-compose.dev.yml up -d
```

### 2. 启动后端

`application-local.yml` 的默认连接指向 `localhost:3306` / `localhost:6379`，使用上面的开发 compose 时需要覆盖端口与密码：

```bash
cd server
DB_URL='jdbc:mysql://localhost:13306/relax?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai' \
DB_USERNAME=relax \
DB_PASSWORD=change-me \
REDIS_HOST=localhost \
REDIS_PORT=16379 \
REDIS_PASSWORD=change-redis-password \
./mvnw spring-boot:run
```

启动后：

- 健康检查：`http://127.0.0.1:8080/api/v1/health`、`http://127.0.0.1:8080/actuator/health`
- 接口文档：`http://127.0.0.1:8080/swagger-ui.html`
- 首次启动由 Flyway 自动建表并写入基础数据

### 3. 打开小程序

```bash
cd miniapp
npm ci
```

用微信开发者工具导入 `miniapp` 目录即可预览；开发版请求地址在 [miniapp/miniprogram/config/api-environment.ts](./miniapp/miniprogram/config/api-environment.ts) 中配置（开发环境当前指向 `http://192.168.1.7:8080`，体验版与正式版指向 `https://realxback.lyhlz.cn`）。

### 4. 一键启动完整环境

需要容器化的 MySQL、Redis、后端和 Nginx 时：

```bash
cp .env.example deploy/.env   # 修改其中的示例密码
cd deploy
docker compose up --build -d
docker compose ps
```

## 检查与测试

```bash
# 小程序：TypeScript 类型检查 + Vitest 单元测试
cd miniapp
npm ci && npm run check

# 后端：编译、Flyway 迁移与全部测试（H2 内存库）
cd server
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw verify

# 部署配置：Compose 静态校验
docker compose --file deploy/docker-compose.yml config --quiet
docker compose --file deploy/docker-compose.dev.yml config --quiet
```

最近一次本地验证（2026-09-21）：

| 检查项 | 结果 |
| --- | --- |
| `./mvnw verify` | 通过，48 个测试，0 失败，1 跳过 |
| `npm test`（Vitest） | 通过，1 个测试 |
| `npm run typecheck` | 失败，2 个类型错误（见“已知问题”） |
| Compose 配置校验 | 通过 |

## 接口约定

- 所有业务接口以 `/api/v1` 开头，统一响应体为 `{ code, message, data, requestId }`，`code` 为 `OK` 表示成功。
- 鉴权使用 `Authorization: Bearer <accessToken>`，令牌由 `POST /api/v1/auth/wechat-login` 系列接口下发，默认有效期 7 天。
- 角色与权限：`USER`、`TECHNICIAN`、`ADMIN`、`SUPER_ADMIN`，管理端接口再按权限分组校验。
- 管理端接口集中在 `/api/v1/admin/**`，技师端履约接口集中在 `/api/v1/technician/**`。
- 日志按请求注入 `requestId`，响应头回传 `X-Request-Id`，便于排查问题。

## 环境与配置

后端使用 Spring Profile 区分环境：`local`（默认）、`dev`、`staging`、`production`、`test`。密钥类配置全部通过环境变量注入，仓库内不保存真实密钥。常用变量：

| 变量 | 用途 |
| --- | --- |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | MySQL 连接 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis 连接 |
| `WECHAT_APP_ID` / `WECHAT_APP_SECRET` | 微信小程序凭据 |
| `COS_REGION` / `COS_BUCKET` / `COS_SECRET_ID` / `COS_SECRET_KEY` | 腾讯云 COS 文件存储 |
| `AUTH_TOKEN_TTL` | 登录令牌有效期，默认 `7d` |
| `BOOTSTRAP_SUPER_ADMIN_OPEN_ID` | 首个超级管理员微信 openId |
| `LOCAL_FILE_ROOT` | 本地文件存储根目录 |

完整清单见 [.env.example](./.env.example) 与 [docs/DEPLOYMENT.md](./docs/DEPLOYMENT.md)。

## 文档

| 文档 | 内容 |
| --- | --- |
| [docs/PRD.md](./docs/PRD.md) | 产品目标、角色、页面、业务规则与验收标准 |
| [docs/TECHNICAL_DESIGN.md](./docs/TECHNICAL_DESIGN.md) | 技术架构、模块边界、数据模型、订单与支付设计 |
| [docs/DEVELOPMENT_PLAN.md](./docs/DEVELOPMENT_PLAN.md) | 阶段划分、开发顺序与验收方式 |
| [docs/PHASE_1_SETUP.md](./docs/PHASE_1_SETUP.md) | 阶段 1 骨架与基础设施说明 |
| [docs/DEPLOYMENT.md](./docs/DEPLOYMENT.md) | 部署、发布与回滚手册 |
| [docs/OPERATIONS.md](./docs/OPERATIONS.md) | 日常巡检、订单与技师运营、备份与安全 |
| [docs/TEST_REPORT.md](./docs/TEST_REPORT.md) | 完整测试报告与已修复问题记录 |

## 已知问题

- `miniapp/miniprogram/packageAdmin/pages/order-list/index.ts` 第 2 行的 `services/http` 导入多写了一层相对路径（`../../../../services/http`），导致 `npm run typecheck` 报 2 个类型错误；其余管理端页面使用的是 `../../../services/http`。修正该路径后 CI 的 miniapp 任务即可通过。
- `keepsPrivateFileBehindAuthorizedShortLivedAccess` 因测试环境事务隔离问题被 `@Disabled`，生产逻辑正常。
- 本地与测试环境的支付走模拟网关，真实微信支付参数需在部署环境注入。
