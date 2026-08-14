# 东莞到家按摩小程序

单个原生微信小程序承载用户、技师和管理员三种角色，后端采用 Java 21 与 Spring Boot 模块化单体。

当前完成阶段：阶段 1，项目骨架与基础设施。业务功能尚未开始开发。

## 目录

```text
miniapp/   原生微信小程序 TypeScript 工程
server/    Spring Boot 后端
deploy/    Docker Compose、Nginx 和运维脚本
docs/      PRD、技术设计和开发计划
pic/       参考截图
```

## 本地检查

```bash
cd miniapp
npm ci
npm run check

cd ../server
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw verify

cd ../deploy
docker compose config
```

本地运行方式和环境变量见 [阶段 1 运行说明](./docs/PHASE_1_SETUP.md)。
