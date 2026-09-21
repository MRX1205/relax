# 东莞到家 - 完整开发计划

> 版本：v2.0 | 制定日期：2026-09-21 | 状态：待执行

---

## 一、项目概述

**项目名称：** 东莞到家 (Dongguan Home Service)  
**项目类型：** 微信小程序 + Spring Boot 后端  
**核心功能：** 上门按摩/SPA预约平台，用户可预约技师上门服务

### 技术栈
| 层级 | 技术 |
|------|------|
| 小程序前端 | WeChat Miniprogram + TypeScript |
| 后端 | Spring Boot 4.1 + Java 21 |
| ORM | MyBatis Plus |
| 数据库 | MySQL + Flyway 迁移 |
| 缓存 | Redis |
| 文件存储 | 腾讯 COS / 本地存储 |
| 安全 | Spring Security + JWT |
| 支付 | 微信支付 V3 (JSAPI) / Mock 支付 |

---

## 二、参考设计分析（丽都到家 App）

参考 `/pic/` 目录下的 15 张参考图，核心 UI 设计要点如下：

### 首页设计
- Banner 轮播图（绿色正规·禁止小费 / 城市合伙人招募）
- 4个快捷入口：限时秒杀、技师入驻、拉新赚钱、会员卡
- Tab 栏：**首页 / 技师 / 动态 / 我的**（4个tab，非现有的5个）

### 技师列表页
- 城市选择器
- 搜索栏 + 列表/地图切换
- 排序筛选：全城 / 距离优先 / 服务时段 / 筛选
- 技师卡片：名字、年代标签（95后/85后/90后）、身高、星级评分、年订单数、距离、"去下单"按钮
- 卡片底部：4张生活照片宫格、最早可约时间、签名:实名认证

### 技师详情页
- 全宽主图（支持头像/生活照 Tab 切换）
- 4个认证标识：安心服务/手机认证/实名认证/健康档案
- 统计：评分、订单数、距离、年龄/体重/身高
- 服务项目列表：名称+价格+时长+"立即预约"按钮
- 评价区块

### 动态页面
- Tab：附近 / 上门 / 到店 / 关注
- 双列瀑布流照片网格（带区域标签和距离）

### 我的页面
- 用户头像、昵称、立即绑定手机号
- 统计：奖金余额、关注、收藏、优惠券
- VIP会员横幅
- 我的服务菜单（9项）：我的订单、地址管理、联系客服、意见反馈、技师接单、招商/合作、账户信息、关于我们

### 技师/BOSS 登录页（参考图13）
- 双 Tab 切换（技师 / BOSS）
- 手机号+密码登录
- 验证码登录选项

### 身份选择页（参考图14）
- 应聘理疗师（实名认证，安全有保障）
- 城市合伙人（合作共赢，共创未来）
- 门店入驻（合作共赢，共创未来）

### 预约下单页（参考图15）
- 地址选择
- 服务项目（带数量增减 ➕➖）
- 服务理疗师（带头像，可切换）
- 服务时间选择
- 出行方式（公交地铁 / 打车自驾，影响出行费）
- 价格明细：套餐总价 + 往返出行费 + 优惠券 + 备注
- 号码保护 checkbox
- 微信支付方式
- 底部：合计金额 + 立即下单

---

## 三、角色与权限设计

### 3.1 三种角色

| 角色 | 代码 | 说明 | 入口 |
|------|------|------|------|
| 普通用户 | `USER` | 浏览、预约、下单、评价 | 微信一键登录后默认 |
| 技师 | `TECHNICIAN` | 接单、排班、收入管理 | 申请审核通过后获得 |
| 管理员 | `ADMIN` / `SUPER_ADMIN` | 全部管理功能 | 后台分配 |

### 3.2 角色切换设计方案

**采用：单账号多角色 + 服务端记录 lastRole 方案（已有基础实现）**

工作流程：
1. 用户首次登录 → 默认获得 `USER` 角色
2. 用户申请成为技师 → 管理员审核通过 → 账号增加 `TECHNICIAN` 角色
3. 管理员由 `SUPER_ADMIN` 在 IAM 页面分配 `ADMIN` 角色
4. 用户在"我的"页面点击"切换身份" → 选择已拥有的角色 → 后端更新 `lastRole`
5. 切换后 `reLaunch` 到首页，首页根据 `account.lastRole` 展示不同内容
6. Tab 栏根据 `lastRole` 动态渲染（`custom-tabbar` 组件已实现此逻辑）

**三角色 Tab 布局：**

| Tab位置 | USER | TECHNICIAN | ADMIN |
|--------|------|-----------|-------|
| Tab 1 | 首页 | 工作台 | 工作台 |
| Tab 2 | 技师 | 订单 | 订单 |
| Tab 3 | 动态 | 排班 | 运营 |
| Tab 4 | 我的 | 收入 | 财务 |
| Tab 5 | — | 我的 | 我的 |

> 注：参考图为4个Tab，我们保持5个Tab（增加"我的"）以满足功能完整性

### 3.3 权限控制

**小程序端：**
- 每个角色的功能页面分布在对应 subpackage（`packageUser`, `packageTech`, `packageAdmin`）
- Tab 切换由 `custom-tabbar` 根据 `account.lastRole` 控制，不同角色看到不同 Tab
- 页面级 `onShow` 检查 token，未登录跳转到 login

**后端：**
- Spring Security + `@PreAuthorize` 控制接口权限
- `/api/v1/technician/**` 需要 `TECHNICIAN` 或更高角色
- `/api/v1/admin/**` 需要 `ADMIN` 或 `SUPER_ADMIN` 角色
- `/api/v1/orders/**` 需要已登录用户

---

## 四、支付系统设计

### 4.1 支付开关（Mock / 真实）

在管理员后台（`packageAdmin/pages/payment-config`）提供开关：

| 配置项 | 值 | 效果 |
|--------|-----|------|
| `wxpay.enabled` | `false` | **Mock 支付**（测试模式，立即模拟成功，无需微信商户号）|
| `wxpay.enabled` | `true` | **真实微信支付** V3 JSAPI（需要配置商户信息）|

### 4.2 完整支付流程

**Mock 支付（默认/测试）：**
```
用户下单 → POST /api/v1/orders → 返回 orderNo
→ POST /api/v1/orders/{orderNo}/payments → 返回 {paymentNo, channel:"MOCK"}
→ POST /api/v1/payments/{paymentNo}/simulate?scenario=SUCCESS
→ 订单状态变为 PAID，流程完成
```

**真实微信支付（生产）：**
```
用户下单 → POST /api/v1/orders → 返回 orderNo
→ POST /api/v1/orders/{orderNo}/payments
  → 后端根据 wxpay.enabled=true 调用微信统一下单接口
  → 返回 {paymentNo, channel:"WXPAY", payParams:{timeStamp, nonceStr, package, signType, paySign}}
→ 小程序调用 wx.requestPayment(payParams)
→ 用户在微信支付弹窗完成支付
→ 微信回调 POST /api/v1/payments/wechat/notify
→ 后端验证签名 → 更新订单状态为 PAID
```

### 4.3 小程序端支付改造代码

```typescript
// packageUser/pages/booking/index.ts - handleSubmit()
const orderDetail = await createOrder({...});
const payment = await createPayment(orderDetail.order.orderNo);

if (payment.channel === 'MOCK') {
  // Mock 支付 - 立即模拟成功
  await simulatePayment(payment.paymentNo, 'SUCCESS');
} else {
  // 真实微信支付
  await new Promise<void>((resolve, reject) => {
    wx.requestPayment({
      timeStamp: payment.payParams!.timeStamp,
      nonceStr: payment.payParams!.nonceStr,
      package: payment.payParams!.package,
      signType: 'RSA',
      paySign: payment.payParams!.paySign,
      success: () => resolve(),
      fail: (err) => reject(new Error(err.errMsg || '支付取消或失败')),
    });
  });
}

wx.showToast({ title: '下单成功', icon: 'success' });
```

### 4.4 后端 PaymentView 需新增字段

```java
// PaymentMapper.PaymentView 需新增：
// channel: String (MOCK / WXPAY)
// payParams: Map<String, String> (真实支付时填充，mock时为null)
```

---

## 五、Bug 清单与修复方案

### 🔴 Critical（必须修复，影响编译/核心功能）

| # | 文件 | Bug 描述 | 修复方案 |
|---|------|---------|---------|
| C1 | `packageAdmin/pages/order-list/index.ts:2` | import 路径 `../../../../services/http` 层级多一层 `../`，运行时报错 | 改为 `../../../services/http` |
| C2 | `packageUser/pages/refund/` | 目录存在但完全为空，app.json 引用此页面导致编译失败 | 创建完整页面（index.ts / index.wxml / index.wxss / index.json）|
| C3 | `packageUser/pages/booking/index.ts:238-239` | `simulatePayment()` 无条件调用，生产环境无法真实支付 | 根据 `payment.channel` 判断走 Mock 还是 `wx.requestPayment` |
| C4 | `pages/account/index.ts` | avatarUrl 设置为相对路径 `/api/v1/public/files/${id}`，无法显示 | 拼接 `environment.apiBaseUrl` 为完整 URL |

### 🟠 Significant（影响功能完整性）

| # | 文件 | Bug 描述 | 修复方案 |
|---|------|---------|---------|
| S1 | `pages/tab-browse/index.wxml` | tab key 值 `"done"` 与 JS 中 `"completed"` 不一致，已完成 tab 无法切换 | 统一为 `"completed"` |
| S2 | `pages/home/index.ts` | `adminStats.todayOrders` 字段不在 `StatsView` 类型中，永远显示 0 | 与后端 API 对齐字段，或后端添加 todayOrders |
| S3 | `packageUser/pages/project-list/index.ts` | `parseInt(categoryId)` 对 UUID 字符串返回 NaN，分类筛选完全失效 | 去掉 parseInt，直接传字符串 categoryId |
| S4 | `packageUser/pages/booking/index.ts:180` | 时间计算 `Math.ceil(90/60)=2` 导致 90 分钟服务结束时间早 30 分钟 | 改为 `LocalTime` 风格的分钟精确计算 |
| S5 | `packageAdmin/pages/projects/index.ts:84` | 项目封面图使用错误的文件用途 `"TECHNICIAN_PHOTO"` | 改为 `"PROJECT_COVER"` |
| S6 | `pages/phone/index.ts handleSkip()` | 跳过绑定手机号后固定跳用户工作台，技师/管理员会进入错误界面 | 读取 `account.lastRole` 按角色路由 |
| S7 | `packageUser/pages/booking` | 下单无优惠券选择 UI，`couponId` 永远不传 | 增加优惠券 picker，整合到 createOrder |

### 🟡 Minor / 代码质量问题

| # | 文件 | 问题 | 修复方案 |
|---|------|------|---------|
| M1 | `pages/home/index.ts:3-4` | 双重 import from 同一文件（重复导入）| 合并为一行 import |
| M2 | `packageTech/pages/order-detail/index.ts` | `this.onLoad({orderNo})` 手动调用生命周期反模式 | 抽取 `loadDetail()` 方法 |
| M3 | `packageUser/pages/order-detail/index.ts` | 同上 | 同上 |
| M4 | `packageAdmin/pages/banners/index` | 只能填标题/排序，无图片上传、无链接配置 | 补充图片上传 + 跳转链接字段 |
| M5 | `packageTech/pages/service-areas/index` | 只读展示，技师无法修改服务区域 | 增加编辑/删除/新增功能 |
| M6 | `packageAdmin/pages/refunds/index` | 拒绝退款理由硬编码为 `"不符合退款条件"` | Modal 弹窗输入自定义理由 |
| M7 | `pages/tab-mid/index` | `sortBy` 状态有更新但未传给 getTechnicians API | 将 sortBy 参数传给 API |
| M8 | `services/http.ts` | 401 无全局处理，各页面需各自处理 | 在 request() 中添加 401 → 清除 session → reLaunch 到 login |
| M9 | 服务层 | `order.ts` / `http.ts` / `region.ts` 在三个 subpackage 中各自复制（~1000行重复代码）| 使用 npm 包或路径别名整合 |
| M10 | 多个页面 | `handleInput(field)` 闭包反模式，WXML 无法绑定有参函数 | 改为 `data-field` + 统一 dispatch 方法 |

---

## 六、UI 全面重设计计划

### 6.1 首页 (`pages/home/index`) 重设计

**现状：** 纯文字分类入口 + 项目列表 + 技师列表

**目标设计（对标参考图2）：**
- 顶部：城市选择器 + 搜索框（可跳转搜索技师）
- Banner 轮播图（图片来自管理员配置，支持跳转链接）
- 4个快捷功能图标：限时秒杀 / 技师入驻 / 拉新赚钱 / 会员卡
- 热门项目横向滚动卡片（附真实封面图片）
- 推荐技师卡片（含生活照、评分、年订单数、距离）

### 6.2 技师列表页 重设计

**现状：** 简单文字列表，只有名字和状态

**目标设计（对标参考图4）：**
- 城市选择器 + 搜索栏
- 排序筛选工具栏：全城 / 距离优先 / 服务时段 / 筛选
- 升级版技师卡片：
  - 技师名 + 年代标签（90后/95后/85后）
  - ⭐ 评分 + 年订单数 + 距离
  - **4张生活照缩略图（2×2 宫格）**
  - 最早可约时间
  - 实名认证标识
  - "去下单"按钮（橙红色圆角按钮）

### 6.3 技师详情页 重设计

**现状：** 简单头部 + 项目列表 + 时间日历

**目标设计（对标参考图5-6）：**
- **全宽顶部大图**（支持头像/生活照 Tab 切换）
- 点击图片可调用 `wx.previewImage` 查看大图
- 4个认证徽章：安心服务 / 手机认证 / 实名认证 / 健康档案
- +关注按钮（心形图标）
- 统计数据行：评分 | 订单数 | 距离 | 年龄/体重/身高
- 服务项目列表：图片 + 名称 + 价格 + 时长 + "立即预约"按钮
- 下单须知 + 安全保障展开区块
- 用户评价列表

### 6.4 动态页面 (`pages/tab-browse` USER模式) 重设计

**现状：** 项目列表（浏览 Tab 为用户模式时）

**目标设计（对标参考图7）：**
- 顶部筛选 Tab：附近 / 上门 / 到店 / 关注
- 双列瀑布流图片网格（`scroll-view` + 绝对定位实现）
- 每个 item：技师/项目图片 + 地区标签 + 距离

### 6.5 我的页面 (`pages/account`) 重设计

**现状：** 简单白色背景 + 列表

**目标设计（对标参考图10）：**
- 顶部红色渐变背景卡片
- 用户头像（圆形）+ 昵称 + "立即绑定手机号"入口
- 4格统计数据：奖金余额 / 关注数 / 收藏数 / 优惠券数
- VIP 横幅（金色渐变背景）
- "我的服务"菜单列表（9项带图标）

### 6.6 预约下单页 (`packageUser/pages/booking`) 重设计

**现状：** 功能实现但 UI 简陋

**目标设计（对标参考图15）：**
- 顶部：请选择服务地址（可编辑/选择）
- 服务项目行：图片 + 名称 + 价格 + 数量增减（➕➖）
- 服务理疗师行：头像 + 名称 + 可切换箭头
- 服务时间行：日期 + 时间 + 可修改箭头
- 出行方式选择：公交地铁 / 打车自驾（按钮切换，影响出行费）
- 出行距离和费用计算展示
- 价格明细：套餐总价 / 往返出行费 / 通用优惠券（可选）
- 备注（选填）
- 号码保护（checkbox）
- 支付方式（微信支付 ✓）
- 底部固定：合计金额（红色大字）+ 立即下单按钮

---

## 七、功能完整性补充

### 待创建/完善的功能

| 功能 | 路径 | 优先级 | 说明 |
|------|------|--------|------|
| 退款申请页 | `packageUser/pages/refund/index` | 🔴 P0 | 目录为空，编译必须修复 |
| 照片查看器 | `packageUser/pages/technician-detail` | 🟠 P1 | 使用 `wx.previewImage` |
| 优惠券下单整合 | `packageUser/pages/booking` | 🟠 P1 | 选择优惠券并传 couponId |
| Banner 图片上传 | `packageAdmin/pages/banners` | 🟠 P1 | 目前只能填标题 |
| 售后凭证图片 | `packageUser/pages/after-sale` | 🟡 P2 | 上传图片佐证 |
| 技师服务区编辑 | `packageTech/pages/service-areas` | 🟡 P2 | 目前只读 |
| 动态/瀑布流 | `pages/tab-browse` USER模式 | 🟡 P2 | 参考图7 |
| 关注技师 | 技师详情 + 我的 | 🟡 P2 | +关注按钮 |

### 后端需要补充的字段/API

| API/字段 | 当前状态 | 说明 |
|---------|---------|------|
| `TechnicianItem.photos[]` | 缺失 | 技师生活照 URL 列表 |
| `TechnicianItem.rating` | 缺失 | 技师评分（保留1位小数）|
| `TechnicianItem.annualOrders` | 缺失 | 年订单量（整数）|
| `TechnicianItem.ageTag` | 缺失 | 年代标签（"90后"/"95后"）|
| `TechnicianItem.height` | 缺失 | 身高（cm，整数）|
| `TechnicianItem.earliestAvailableTime` | 缺失 | 最早可约时间字符串 |
| `TechnicianItem.certifications[]` | 缺失 | 认证标识列表 |
| `StatsView.todayOrders` | 缺失 | 今日订单数 |
| `PaymentView.channel` | 缺失 | MOCK / WXPAY |
| `PaymentView.payParams` | 缺失 | 真实支付时的 wx.requestPayment 参数 |
| 关注技师 API | 缺失 | `POST /api/v1/technicians/{id}/follow` |

---

## 八、开发阶段规划

### Phase 1：修复 Critical Bug（1-2天）
**目标：** 让小程序能正常编译运行，核心支付流程正确

- [ ] **[C1]** 修复 `packageAdmin/pages/order-list/index.ts` import 路径
- [ ] **[C2]** 创建 `packageUser/pages/refund/` 所有文件（退款申请页）
- [ ] **[C3]** 改造 booking 页支付逻辑（按 channel 判断 Mock/真实）
- [ ] **[C4]** 修复 account 页 avatarUrl 拼接 apiBaseUrl
- [ ] **[S1]** 修复 tab-browse "done"/"completed" 不一致
- [ ] **[S4]** 修复 booking 时间结束时间计算错误
- [ ] **[M1]** 合并 home/index.ts 双重 import

### Phase 2：支付系统整合（2-3天）
**目标：** Mock/真实支付双模式可用

- [ ] **后端：** `PaymentController.createPayment()` 根据 `wxpay.enabled` 决定返回 Mock 还是真实支付参数
- [ ] **后端：** `PaymentView` 添加 `channel` 和 `payParams` 字段（含 Mapper 改造）
- [ ] **后端：** 完善 `WxPayService.verifyNotification()` 微信签名验证（当前为 TODO）
- [ ] **小程序：** booking 页集成 `wx.requestPayment` 真实支付调用
- [ ] **管理后台：** 完善 payment-config 页面说明文字

### Phase 3：UI 全面重设计（4-6天）
**目标：** 整体 UI 对标参考图，提升用户体验

**优先：技师相关（用户核心功能）**
- [ ] 后端添加技师扩展字段（photos/rating/annualOrders/ageTag/height/earliestAvailableTime）
- [ ] 数据库迁移脚本（新增字段到 technician 表）
- [ ] 技师列表卡片升级（照片宫格、评分、距离、年代标签、"去下单"）
- [ ] 技师详情全面重设计（大图 + 认证徽章 + 照片切换 + wx.previewImage）
- [ ] 技师详情：评价列表展示

**其他页面：**
- [ ] 首页 Banner 轮播图（swiper 组件 + 真实图片）
- [ ] 首页快捷图标区
- [ ] 我的页面红色渐变头部 + 4格统计 + VIP 横幅
- [ ] 预约下单页对标参考图15全面重设计
- [ ] 登录页增加技师/BOSS 登录 Tab

### Phase 4：功能补全（3-4天）
**目标：** 完整功能链路

- [ ] 退款申请完整功能（完善 Phase 1 骨架）
- [ ] Banner 图片上传 + 跳转链接配置
- [ ] Booking 页优惠券选择 UI 整合
- [ ] 技师服务区域编辑功能
- [ ] 售后凭证图片上传
- [ ] 退款拒绝自定义理由输入

### Phase 5：清理与优化（1-2天）
**目标：** 代码质量提升

- [ ] 删除 9 个不可达死页面（`pages/projects/`, `pages/technicians/`, `pages/orders/`, `pages/workbench-tech/`, `pages/workbench-admin/`, `pages/tech-income/`, `pages/tech-schedules/`, `pages/admin-finance/`, `pages/admin-ops/`）
- [ ] 解决 `order.ts` / `http.ts` / `region.ts` 三份重复问题
- [ ] `services/http.ts` 添加全局 401 自动跳转登录
- [ ] 修复 `handleInput(field)` 闭包反模式（改为 data-field + dispatch）
- [ ] 抽取 `loadDetail()` 方法替换 `this.onLoad()` 手动调用
- [ ] 修复 `pages/phone` 跳过手机绑定后按角色路由 **[S6]**
- [ ] 修复 project-list parseInt UUID bug **[S3]**
- [ ] 修复 projects 封面图文件用途 **[S5]**
- [ ] 修复 home adminStats.todayOrders 字段 **[S2]**

### Phase 6：测试与上线（2-3天）
**目标：** 全流程验收

- [ ] 端到端测试：用户登录→浏览技师→预约→Mock 支付→技师接单→完成→评价
- [ ] 真实微信支付沙箱测试（配置测试商户号）
- [ ] 三角色切换全流程测试
- [ ] 管理员后台所有功能测试
- [ ] 小程序编译检查（无警告无错误）
- [ ] 更新开发环境 IP 配置（`192.168.1.7:8080` 需要可配置化）

---

## 九、开发环境说明

### 小程序本地开发
```
config/api-environment.ts:
  develop → http://192.168.1.7:8080  (需修改为你的本机 IP)
  trial/release → https://realxback.lyhlz.cn
```

**快速切换：** 开发时修改 `api-environment.ts` 中的 develop 地址即可

### 后端本地启动
```bash
cd server
# local profile 使用 H2 内存数据库（无需 MySQL）
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### 超级管理员初始化
第一次运行时，通过环境变量设置超管 OpenID：
```bash
export BOOTSTRAP_SUPER_ADMIN_OPEN_ID=你的微信OpenID
```

### Mock 数据
通过 `packageAdmin/pages/mock-data` 页面一键生成测试数据（应仅限 SUPER_ADMIN 访问）

---

## 十、文件变更总览

### 需要修改的文件（小程序端）

| 文件 | 原因 |
|------|------|
| `pages/home/index.ts` | 合并 import，修复 adminStats.todayOrders |
| `pages/home/index.wxml` | 首页 UI 重设计 |
| `pages/home/index.wxss` | 配套样式 |
| `pages/login/index.wxml` | 增加技师/BOSS 登录 Tab |
| `pages/login/index.ts` | 支持手机号+密码登录 |
| `pages/tab-browse/index.wxml` | 修复 "done" tab，USER 模式改为瀑布流 |
| `pages/tab-browse/index.ts` | USER 模式动态页逻辑 |
| `pages/tab-mid/index.ts` | 修复排序参数传递 |
| `pages/account/index.ts` | 修复 avatarUrl，页面 UI 重设计 |
| `pages/account/index.wxml` | UI 重设计 |
| `pages/account/index.wxss` | 配套样式 |
| `pages/phone/index.ts` | 修复 handleSkip 按角色路由 |
| `packageUser/pages/booking/index.ts` | 修复支付逻辑、时间计算、加入优惠券 |
| `packageUser/pages/booking/index.wxml` | UI 重设计 |
| `packageUser/pages/booking/index.wxss` | 配套样式 |
| `packageUser/pages/technician-list/index.wxml` | 升级技师卡片 |
| `packageUser/pages/technician-list/index.wxss` | 配套样式 |
| `packageUser/pages/technician-detail/index.ts` | 照片查看、认证标识 |
| `packageUser/pages/technician-detail/index.wxml` | 全面重设计 |
| `packageUser/pages/technician-detail/index.wxss` | 配套样式 |
| `packageUser/pages/project-list/index.ts` | 修复 parseInt UUID bug |
| `packageAdmin/pages/order-list/index.ts` | 修复 import 路径 |
| `packageAdmin/pages/banners/index.ts` | 增加图片上传/链接 |
| `packageAdmin/pages/banners/index.wxml` | 配套 UI |
| `packageAdmin/pages/refunds/index.ts` | 自定义拒绝理由 |
| `packageAdmin/pages/projects/index.ts` | 修复文件用途 |
| `services/http.ts` | 添加全局 401 处理 |
| `types/api.d.ts` | 添加 PaymentView.channel/payParams，TechnicianItem 扩展字段 |

### 需要新建的文件（小程序端）

| 文件 | 说明 |
|------|------|
| `packageUser/pages/refund/index.ts` | 退款申请页逻辑 |
| `packageUser/pages/refund/index.wxml` | 退款申请页模板 |
| `packageUser/pages/refund/index.wxss` | 退款申请页样式 |
| `packageUser/pages/refund/index.json` | 退款申请页配置 |

### 需要修改的文件（后端）

| 文件 | 原因 |
|------|------|
| `PaymentController.java` | 根据配置返回 Mock 或真实支付参数 |
| `PaymentMapper.java` | PaymentView 添加 channel/payParams 字段 |
| `PaymentService.java` | 整合 WxPayService，根据开关选择支付方式 |
| `WxPayService.java` | 完善 verifyNotification 签名验证（当前 TODO）|
| `TechnicianMapper.java` | TechnicianItem 添加扩展字段 |
| `StatsController/StatsMapper.java` | 添加 todayOrders 字段 |
| `db/migration/` | 新增数据库迁移脚本（technician 表扩展字段）|

---

## 十一、设计规范参考

### 色彩系统
| 用途 | 色值 |
|------|------|
| 主色/按钮 | `#E54D42` |
| 渐变色 | `#FF7043` |
| 成功色 | `#4CAF50` |
| 警告色 | `#FF9800` |
| 背景灰 | `#F5F5F5` |
| 卡片白 | `#FFFFFF` |
| 文字主色 | `#333333` |
| 文字次色 | `#999999` |
| 价格红 | `#E54D42` |
| 分割线 | `#EEEEEE` |

### 组件规范
- 按钮圆角：`48rpx`（胶囊形）
- 卡片圆角：`16rpx`
- 卡片阴影：`0 2rpx 12rpx rgba(0,0,0,0.08)`
- 页面边距：`30rpx`
- 卡片间距：`20rpx`
- 组件内间距：`24rpx`

---

## 十二、验收标准

### 功能验收（必须全部通过）

**用户端：**
- [ ] 微信一键登录成功
- [ ] 浏览技师列表，每个技师卡片显示生活照（2×2 宫格）
- [ ] 进入技师详情，可切换头像/生活照，点击查看大图
- [ ] 选择项目 → 选择时间 → 选择地址 → 下单 → Mock 支付成功
- [ ] 订单状态变为 PAID 并可在订单列表查看
- [ ] 完成后可评价技师

**技师端：**
- [ ] 切换到技师角色，Tab 栏变为工作台/订单/排班/收入/我的
- [ ] 技师可看到新订单，执行接单→出发→到达→服务→完成全流程

**管理员端：**
- [ ] 切换到管理员角色，Tab 栏变为工作台/订单/运营/财务/我的
- [ ] 可查看所有订单
- [ ] 可审核技师申请（通过/拒绝）
- [ ] 可配置微信支付开关（payment-config 页面）

**支付：**
- [ ] `wxpay.enabled=false` 时，Mock 支付立即成功
- [ ] `wxpay.enabled=true` 时，调用真实 `wx.requestPayment`

### UI 验收
- [ ] 技师卡片显示生活照片宫格（非文字占位）
- [ ] 技师详情支持头像/生活照 Tab 切换
- [ ] 首页有可滑动的 Banner 轮播图
- [ ] 下单页面对标参考图15的设计
- [ ] 整体主色调为红橙色系，与参考图一致

### 代码质量验收
- [ ] `npx tsc` 无 TypeScript 编译错误
- [ ] `packageUser/pages/refund/` 目录下有完整的4个页面文件
- [ ] 9个死页面已删除
- [ ] 无 Critical Bug

---

## 十三、附录：现有 API 端点参考

### 已有端点（可直接使用）
```
认证:     POST /api/v1/auth/wechat-login
          GET  /api/v1/me
          PUT  /api/v1/me/last-role
          PUT  /api/v1/me/profile

目录:     GET  /api/v1/home
          GET  /api/v1/technicians
          GET  /api/v1/technicians/{id}
          GET  /api/v1/technicians/{id}/schedules
          GET  /api/v1/projects
          GET  /api/v1/projects/{id}

订单:     POST /api/v1/orders
          GET  /api/v1/orders/{orderNo}
          POST /api/v1/orders/{orderNo}/payments
          POST /api/v1/payments/{paymentNo}/simulate

技师:     GET  /api/v1/technician/profile
          PUT  /api/v1/technician/online-status
          POST /api/v1/technician/application
          GET  /api/v1/technician/orders/{orderNo}/accept
          POST /api/v1/technician/orders/{orderNo}/complete

管理:     GET  /api/v1/admin/orders
          GET  /api/v1/admin/stats
          GET  /api/v1/admin/payment-config
          PUT  /api/v1/admin/payment-config
```

### 需要新增/改造的端点
```
支付:     POST /api/v1/orders/{orderNo}/payments
          → 改造：根据 wxpay.enabled 返回 Mock 或真实支付参数

统计:     GET  /api/v1/admin/stats
          → 添加 todayOrders 字段

关注:     POST /api/v1/technicians/{id}/follow (新增)
          DELETE /api/v1/technicians/{id}/follow (新增)
```

---

*文档制定：2026-09-21 | Antigravity AI 分析生成*
