# 部署与发布手册

> 版本：V1.0  
> 更新日期：2026-08-25

## 1. 环境要求

| 组件 | 版本 |
|---|---|
| Java | 21 |
| MySQL | 8.4 |
| Redis | 8.2+ |
| Nginx | 1.28+ |
| Docker | 24+ |
| Docker Compose | 2.20+ |

## 2. 配置模板

### 2.1 环境变量

```bash
# .env
DB_HOST=mysql
DB_PORT=3306
DB_NAME=relax
DB_USERNAME=relax
DB_PASSWORD=<strong-password>

REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=<redis-password>

WX_APP_ID=<appid>
WX_APP_SECRET=<secret>
WX_MCH_ID=<商户号>
WX_MCH_KEY=<商户密钥>
WX_MCH_CERT_PATH=/certs/apiclient_cert.pem
WX_MCH_KEY_PATH=/certs/apiclient_key.pem

COS_BUCKET=<bucket>
COS_REGION=<region>
COS_SECRET_ID=<secret-id>
COS_SECRET_KEY=<secret-key>

SUPER_ADMIN_OPENID=<openid>
```

### 2.2 小程序域名配置

在微信公众平台配置以下合法域名：

- request 合法域名：`https://api.yourdomain.com`
- uploadFile 合法域名：`https://api.yourdomain.com`
- downloadFile 合法域名：`https://api.yourdomain.com`

## 3. 部署步骤

### 3.1 使用 Docker Compose

```bash
# 1. 克隆代码
git clone <repo-url> && cd relax

# 2. 配置环境变量
cp .env.example deploy/.env
vim deploy/.env  # 填入真实配置

# 3. 启动服务
cd deploy
docker compose up --build -d

# 4. 检查健康状态
curl http://localhost:8080/api/v1/health
# 预期返回: {"code":"OK","data":{"status":"UP"}}

# 5. 检查数据库迁移
docker compose exec server java -jar app.jar --spring.flyway.validate-on-migrate=true
```

### 3.2 单独部署后端

```bash
cd server
./mvnw clean package -DskipTests
java -jar target/relax-server-*.jar \
  --spring.profiles.active=production \
  --DB_URL=jdbc:mysql://host:3306/relax \
  --DB_USERNAME=relax \
  --DB_PASSWORD=<password>
```

### 3.3 发布小程序

1. 在微信开发者工具中导入 `miniapp` 目录
2. 修改 `miniprogram/config/api-environment.ts` 中的生产域名
3. 上传代码
4. 提交审核

## 4. 数据库迁移

```bash
# 自动迁移（启动时）
java -jar app.jar --spring.flyway.migrate-on-startup=true

# 手动迁移
cd server && ./mvnw flyway:migrate
```

## 5. 备份与恢复

```bash
# 备份
deploy/scripts/backup-mysql.sh

# 恢复
deploy/scripts/restore-mysql.sh deploy/backups/<file>.sql.gz
```

## 6. 回滚方案

### 6.1 小程序回滚
- 在微信公众平台 → 版本管理 → 回退到上一个版本

### 6.2 后端回滚
```bash
# 停止当前版本
docker compose down

# 启动上一个版本
docker compose up -d --build
```

### 6.3 数据库回滚
- Flyway 不支持自动回滚
- 破坏性变更前必须备份
- 回滚时使用备份恢复

## 7. 监控

### 7.1 健康检查
```
GET /api/v1/health
GET /actuator/health
```

### 7.2 关键指标
- 订单创建成功率
- 支付成功率
- 平均响应时间
- 错误率（5xx）

### 7.3 告警
- 服务不可用
- 数据库连接失败
- 支付通知处理失败
- 磁盘空间不足

## 8. 常见问题

### 8.1 支付通知未收到
1. 检查回调域名配置
2. 检查 HTTPS 证书有效性
3. 检查防火墙规则
4. 查看微信支付商户平台通知日志

### 8.2 订单超时未释放
1. 检查定时任务是否正常运行
2. 手动执行：`POST /api/v1/admin/orders/{no}/cancel`

### 8.3 数据库连接池耗尽
1. 检查慢查询日志
2. 增加连接池大小
3. 优化查询语句
