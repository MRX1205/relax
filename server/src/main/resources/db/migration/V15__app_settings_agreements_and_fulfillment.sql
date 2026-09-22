-- V15: App settings, comprehensive legal agreements, and fulfillment enhancements

-- 1. App Settings: Platform Name and VIP Membership Toggle
INSERT INTO app_setting (setting_key, setting_value, description)
VALUES 
    ('app.name', '东莞到家', '平台品牌对外展示名称'),
    ('member.vip.enabled', 'false', 'VIP会员入口开关: true-开启, false-关闭')
ON DUPLICATE KEY UPDATE 
    description = VALUES(description);

-- 2. Comprehensive Legal Agreements: USER_AGREEMENT, PRIVACY_POLICY, TRANSACTION_RULES
-- (1) 用户服务协议
INSERT INTO content_agreement (id, type, version, title, content, effective_at)
VALUES (
    1,
    'USER_AGREEMENT',
    '2.0',
    '东莞到家用户服务协议',
    '【前言】欢迎使用东莞到家到家理疗信息服务平台（以下简称“本平台”）。在您注册、登录、使用本平台服务前，请仔细阅读并充分理解本协议全部内容。\n\n一、服务性质与合规声明\n1. 本平台为正规生活康复理疗、推拿与养生保健服务预约撮合平台。所有入驻技师均经过严格的实名身份核验、从业资质认证与健康体检。\n2. 平台坚决恪守国家法律法规，严禁任何形式的涉黄、涉赌、涉毒等违法违规行为。技师与顾客均须保持职业尊严与合法言行边界。一经发现任何违法违规行为，平台将立即封禁账号、扣除信誉分并移交公安司法机关严肃处理。\n\n二、预约服务与订单规范\n1. 用户可根据需求选择服务项目、技师、期望上门时段及服务地址。请确保提交的服务地址准确真实、联系电话通畅有效。\n2. 如因用户提供的地址错误、闭门不应或联系中断导致技师在约定时间无法提供服务的，因此产生的往返交通损失由用户承担。\n\n三、服务安全与履约守则\n1. 上门技师须统一着装，携带专业消毒工具及消耗品，严格遵守安全卫生规范。\n2. 服务地点须具备安全合法的室内环境。用户须尊重技师的人身权利与人格尊严，严禁进行言语挑衅、肢体骚扰或强迫技师提供非约定服务内容。\n3. 为保障双方人身及财产安全，平台提供全程一键紧急求助与安全录音核验机制。\n\n四、费用结算与禁止私下交易\n1. 本平台所有项目服务价格均实行明码标价，技师严禁向用户额外索取小费或变相加价。\n2. 用户与技师须通过平台完成下单与服务确认，严禁私下脱离平台进行现金或个人转账交易。对于私下交易产生的服务质量争议、资金损失或人身安全风险，平台概不承担责任并有权终止服务资质。\n\n五、争议解决与客服保障\n如在履约过程中发生服务质量不符、时长不足或态度恶劣等情形，用户可在订单中心申请售后维权或联系客服。平台将在24小时内核实事实并依规予以退款或补偿处理。',
    CURRENT_TIMESTAMP
)
ON DUPLICATE KEY UPDATE
    version = VALUES(version),
    title = VALUES(title),
    content = VALUES(content),
    effective_at = VALUES(effective_at);

-- (2) 隐私保护政策
INSERT INTO content_agreement (id, type, version, title, content, effective_at)
VALUES (
    2,
    'PRIVACY_POLICY',
    '2.0',
    '东莞到家隐私保护政策',
    '【前言】东莞到家深知个人信息对您的重要性，并严格按照《中华人民共和国个人信息保护法》《中华人民共和国网络安全法》等法律法规保护您的隐私安全与个人权益。\n\n一、我们收集的信息及用途\n1. 账号基础信息：当您授权微信一键登录时，我们会获取您的微信唯一标识（OpenID）、微信头像及昵称，用于创建并标识您的平台账户。\n2. 手机号码信息：用于订单接单通知、技师到达联络、验证码安全鉴权及重大服务变更提醒。\n3. 地理位置与地址信息：当您设置服务地址或使用“附近技师”功能时，我们需要获取您的定位坐标及详细收货/服务地址，用于计算与技师的距离、核算出行费用并在接单后指引技师精准上门。\n4. 交易与服务记录：包括预约项目、下单时间、支付金额与渠道、售后评价与服务工单，用于履约调度、财务对账与客服保障。\n\n二、信息的保护与加密存储\n1. 平台采用金融级 TLS/HTTPS 加密协议传输所有数据，敏感数据（如手机号码、密码哈希、支付凭据）均采用加密算法存储与脱敏展示。\n2. 严格限制内部员工访问权限，所有数据访问均有系统审计日志留痕。\n\n三、信息的共享与第三方限制\n1. 仅在履约必需的范围内，平台会将当单用户的脱敏联系方式和服务地址向接单技师呈现。技师严禁将顾客信息挪作他用或留存外传。\n2. 平台绝不会向任何第三方商业实体出售、出租或非法共享您的个人信息。\n\n四、您的个人信息权利\n您可以在个人中心自主修改昵称、头像、管理常用服务地址，或联系平台客服申请注销账号。当账号注销后，我们将依据法律法规删除或匿名化您的个人信息。',
    CURRENT_TIMESTAMP
)
ON DUPLICATE KEY UPDATE
    version = VALUES(version),
    title = VALUES(title),
    content = VALUES(content),
    effective_at = VALUES(effective_at);

-- (3) 平台交易规则
INSERT INTO content_agreement (id, type, version, title, content, effective_at)
VALUES (
    3,
    'TRANSACTION_RULES',
    '2.0',
    '东莞到家平台交易与退款规则',
    '【前言】为保障顾客与服务技师双方的合法权益，建立透明、公正、诚信的交易环境，特制定本交易与退款规则。\n\n一、服务费用构成\n1. 平台总费用 = 基础项目服务费 + 上门出行交通费（依实际距离阶梯计费）。所有费用在预约下单界面由系统实时核算、公开透明。\n2. 平台提供以下支付模式（以平台当前生效设置为准）：\n   - 微信官方支付：通过微信支付担保收银台线上扣款；\n   - 现场结算/仅预约：用户下单直接锁定技师档期，费用在技师上门核验并服务完成后现场直接支付结算；\n   - 模拟支付：测试开发环境下的秒级模拟扣款体验。\n\n二、接单与履约时效\n1. 顾客下单后，系统优先通知对应技师。技师须在15分钟内完成接单响应。\n2. 接单后技师须提前规划行程并准时到达约定服务地址；到达后现场与顾客确认项目内容，并按标准流程计时履约。\n\n三、退款与取消订单规则\n1. 技师接单前：用户可随时无条件免费取消订单，已付款项原路全额极速退款。\n2. 技师已接单但尚未出发前：用户可免费取消订单，全额退款。\n3. 技师已出发前往途中：若因用户个人原因单方面取消，平台将扣除基础交通出行成本（按单笔最高不超过30元计）补偿技师空跑，其余项目费用全额原路退还。\n4. 技师原因爽约或严重迟到超30分钟：用户有权单方取消订单，平台全额退还全部费用，并由平台向用户补偿平台优惠体验券。\n\n四、加时服务与增项\n服务进行期间如需加长时长或追加其他理疗项目，用户须在小程序内直接提交加钟/增项订单，以便系统统筹技师后续排班并享有平台全程安全保障。严禁私下现金交易加钟。',
    CURRENT_TIMESTAMP
)
ON DUPLICATE KEY UPDATE
    version = VALUES(version),
    title = VALUES(title),
    content = VALUES(content),
    effective_at = VALUES(effective_at);
