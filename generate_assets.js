const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const ASSETS_DIR = path.resolve(__dirname, 'miniapp/miniprogram/assets');
const TABBAR_DIR = path.join(ASSETS_DIR, 'tabbar');
const ICONS_DIR = path.join(ASSETS_DIR, 'icons');
const DEFAULTS_DIR = path.join(ASSETS_DIR, 'defaults');

[TABBAR_DIR, ICONS_DIR, DEFAULTS_DIR].forEach(dir => {
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
});

function svgToPng(svgContent, outPath, width = 64, height = 64) {
  const tmpSvg = outPath.replace(/\.png$/, '.svg');
  fs.writeFileSync(tmpSvg, svgContent.trim());
  try {
    execSync(`sips -s format png "${tmpSvg}" --resampleHeightWidth ${height} ${width} --out "${outPath}"`, { stdio: 'pipe' });
  } catch (err) {
    console.error('sips failed for', outPath, err.message);
  }
}

// ══════════════════════════════════════════════════
// 1. TabBar Icons (64x64 PNG)
// ══════════════════════════════════════════════════

// Home
svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="64" height="64">
  <path d="M7 19L24 6l17 13v21a3 3 0 0 1-3 3H10a3 3 0 0 1-3-3V19z" fill="none" stroke="#8E8E93" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round"/>
  <path d="M19 43V25h10v18" fill="none" stroke="#8E8E93" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round"/>
</svg>
`, path.join(TABBAR_DIR, 'home.png'));

svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="64" height="64">
  <path d="M7 19L24 6l17 13v21a3 3 0 0 1-3 3H10a3 3 0 0 1-3-3V19z" fill="#FF382E" stroke="#FF382E" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
  <path d="M19 43V25h10v18" fill="#FFFFFF" stroke="#FFFFFF" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
</svg>
`, path.join(TABBAR_DIR, 'home-active.png'));

// Projects (Browse)
svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="64" height="64">
  <rect x="7" y="7" width="14" height="14" rx="4" fill="none" stroke="#8E8E93" stroke-width="3.5"/>
  <rect x="27" y="7" width="14" height="14" rx="4" fill="none" stroke="#8E8E93" stroke-width="3.5"/>
  <rect x="7" y="27" width="14" height="14" rx="4" fill="none" stroke="#8E8E93" stroke-width="3.5"/>
  <rect x="27" y="27" width="14" height="14" rx="4" fill="none" stroke="#8E8E93" stroke-width="3.5"/>
</svg>
`, path.join(TABBAR_DIR, 'projects.png'));

svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="64" height="64">
  <rect x="7" y="7" width="14" height="14" rx="4" fill="#FF382E"/>
  <rect x="27" y="7" width="14" height="14" rx="4" fill="#FF382E"/>
  <rect x="7" y="27" width="14" height="14" rx="4" fill="#FF382E"/>
  <rect x="27" y="27" width="14" height="14" rx="4" fill="#FF382E"/>
</svg>
`, path.join(TABBAR_DIR, 'projects-active.png'));

// Technicians
svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="64" height="64">
  <circle cx="24" cy="15" r="8" fill="none" stroke="#8E8E93" stroke-width="3.5"/>
  <path d="M10 41v-4a10 10 0 0 1 10-10h8a10 10 0 0 1 10 10v4" fill="none" stroke="#8E8E93" stroke-width="3.5" stroke-linecap="round"/>
</svg>
`, path.join(TABBAR_DIR, 'technicians.png'));

svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="64" height="64">
  <circle cx="24" cy="15" r="8" fill="#FF382E"/>
  <path d="M10 41v-4a10 10 0 0 1 10-10h8a10 10 0 0 1 10 10v4" fill="#FF382E"/>
</svg>
`, path.join(TABBAR_DIR, 'technicians-active.png'));

// Orders
svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="64" height="64">
  <path d="M11 6h18l10 10v24a3 3 0 0 1-3 3H11a3 3 0 0 1-3-3V9a3 3 0 0 1 3-3z" fill="none" stroke="#8E8E93" stroke-width="3.5" stroke-linejoin="round"/>
  <path d="M29 6v10h10" fill="none" stroke="#8E8E93" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round"/>
  <line x1="16" y1="24" x2="32" y2="24" stroke="#8E8E93" stroke-width="3.5" stroke-linecap="round"/>
  <line x1="16" y1="32" x2="28" y2="32" stroke="#8E8E93" stroke-width="3.5" stroke-linecap="round"/>
</svg>
`, path.join(TABBAR_DIR, 'orders.png'));

svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="64" height="64">
  <path d="M11 6h18l10 10v24a3 3 0 0 1-3 3H11a3 3 0 0 1-3-3V9a3 3 0 0 1 3-3z" fill="#FFF0EE" stroke="#FF382E" stroke-width="3.5" stroke-linejoin="round"/>
  <path d="M29 6v10h10" fill="#FFE2DC" stroke="#FF382E" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round"/>
  <line x1="16" y1="24" x2="32" y2="24" stroke="#FF382E" stroke-width="3.5" stroke-linecap="round"/>
  <line x1="16" y1="32" x2="28" y2="32" stroke="#FF382E" stroke-width="3.5" stroke-linecap="round"/>
</svg>
`, path.join(TABBAR_DIR, 'orders-active.png'));

// Account (My)
svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="64" height="64">
  <circle cx="24" cy="24" r="18" fill="none" stroke="#8E8E93" stroke-width="3.5"/>
  <circle cx="24" cy="18" r="6" fill="none" stroke="#8E8E93" stroke-width="3.5"/>
  <path d="M13 36c2.5-5 6.5-7 11-7s8.5 2 11 7" fill="none" stroke="#8E8E93" stroke-width="3.5" stroke-linecap="round"/>
</svg>
`, path.join(TABBAR_DIR, 'account.png'));

svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="64" height="64">
  <circle cx="24" cy="24" r="18" fill="#FF382E" stroke="#FF382E" stroke-width="2"/>
  <circle cx="24" cy="18" r="6" fill="#FFFFFF"/>
  <path d="M13 36c2.5-5 6.5-7 11-7s8.5 2 11 7" fill="#FFFFFF"/>
</svg>
`, path.join(TABBAR_DIR, 'account-active.png'));

// ══════════════════════════════════════════════════
// 2. Home Quick Action Icons (96x96 PNG)
// ══════════════════════════════════════════════════

// 限时特惠 (Flame / Special Offer)
svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="96" height="96">
  <path d="M26.5 4C26.5 4 15 17 15 26.5a11.5 11.5 0 0 0 23 0c0-6-4.5-12-7-15.5-.8 4-3 7-6 8 0-4 1.5-11 1.5-15z" fill="#FF382E"/>
  <path d="M24 23c-1.5 2-3 4.5-3 7a5.5 5.5 0 0 0 11 0c0-2.5-2-5.5-4-7-1 1.5-2.5 2-4 0z" fill="#FFB3A7"/>
</svg>
`, path.join(ICONS_DIR, 'flame.png'), 96, 96);

// 技师入驻 (Therapist Join / Verification)
svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="96" height="96">
  <path d="M24 4L7 11v11c0 10.5 7.2 20.3 17 22 9.8-1.7 17-11.5 17-22V11L24 4z" fill="#007AFF"/>
  <path d="M24 14a5 5 0 1 0 0 10 5 5 0 0 0 0-10zm-7 18c0-3.5 3.5-6 7-6s7 2.5 7 6v2H17v-2z" fill="#FFFFFF"/>
</svg>
`, path.join(ICONS_DIR, 'tech.png'), 96, 96);

// 邀请有礼 (Gift / Reward)
svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="96" height="96">
  <rect x="8" y="19" width="32" height="23" rx="3" fill="#34C759"/>
  <rect x="5" y="13" width="38" height="8" rx="2" fill="#28A745"/>
  <rect x="21" y="13" width="6" height="29" fill="#E8F8EC"/>
  <path d="M24 13c-3-5-8-5-8-2s5 3 8 2zm0 0c3-5 8-5 8-2s-5 3-8 2z" fill="none" stroke="#28A745" stroke-width="3"/>
</svg>
`, path.join(ICONS_DIR, 'gift.png'), 96, 96);

// VIP会员 (Crown / VIP)
svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="96" height="96">
  <path d="M8 35l-3-20 10 8 9-14 9 14 10-8-3 20H8z" fill="#FF9500"/>
  <rect x="7" y="38" width="34" height="5" rx="2.5" fill="#E08500"/>
  <circle cx="5" cy="15" r="2.5" fill="#FFE5A3"/>
  <circle cx="24" cy="9" r="2.5" fill="#FFE5A3"/>
  <circle cx="43" cy="15" r="2.5" fill="#FFE5A3"/>
</svg>
`, path.join(ICONS_DIR, 'vip.png'), 96, 96);

// 定位图标 (Location Pin)
svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="48" height="48">
  <path d="M24 4C16.3 4 10 10.3 10 18c0 10.5 14 26 14 26s14-15.5 14-26c0-7.7-6.3-14-14-14zm0 19a5 5 0 1 1 0-10 5 5 0 0 1 0 10z" fill="#FF382E"/>
</svg>
`, path.join(ICONS_DIR, 'pin.png'), 48, 48);

// 搜索图标 (Search Icon)
svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="48" height="48">
  <circle cx="21" cy="21" r="13" fill="none" stroke="#8E8E93" stroke-width="4.5"/>
  <line x1="31" y1="31" x2="42" y2="42" stroke="#8E8E93" stroke-width="4.5" stroke-linecap="round"/>
</svg>
`, path.join(ICONS_DIR, 'search.png'), 48, 48);

// 实名认证盾牌 (Verified Shield)
svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="48" height="48">
  <path d="M24 4L7 11v11c0 10.5 7.2 20.3 17 22 9.8-1.7 17-11.5 17-22V11L24 4z" fill="#007AFF"/>
  <path d="M16 23l6 6 11-11" fill="none" stroke="#FFFFFF" stroke-width="4" stroke-linecap="round" stroke-linejoin="round"/>
</svg>
`, path.join(ICONS_DIR, 'shield.png'), 48, 48);

// 消息铃铛 (Bell)
svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="48" height="48">
  <path d="M24 44c2.2 0 4-1.8 4-4H20c0 2.2 1.8 4 4 4zm14-12v-10c0-7.2-4.8-13.3-12-14.7V6c0-1.1-.9-2-2-2s-2 .9-2 2v1.3C14.8 8.7 10 14.8 10 22v10l-4 4v2h36v-2l-4-4z" fill="#AF52DE"/>
</svg>
`, path.join(ICONS_DIR, 'bell.png'), 48, 48);

// 时钟 (Clock)
svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="48" height="48">
  <circle cx="24" cy="24" r="18" fill="none" stroke="#8E8E93" stroke-width="4"/>
  <path d="M24 12v12l8 4" fill="none" stroke="#8E8E93" stroke-width="4" stroke-linecap="round"/>
</svg>
`, path.join(ICONS_DIR, 'clock.png'), 48, 48);

// 星星 (Star)
svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="48" height="48">
  <polygon points="24,4 30.2,16.5 44,18.5 34,28.2 36.4,42 24,35.5 11.6,42 14,28.2 4,18.5 17.8,16.5" fill="#FF9500"/>
</svg>
`, path.join(ICONS_DIR, 'star.png'), 48, 48);

// 对勾 (Check)
svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="48" height="48">
  <circle cx="24" cy="24" r="20" fill="#E8F8EC"/>
  <path d="M14 24l7 7 13-13" fill="none" stroke="#34C759" stroke-width="4" stroke-linecap="round" stroke-linejoin="round"/>
</svg>
`, path.join(ICONS_DIR, 'check.png'), 48, 48);

// 相机 (Camera)
svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="48" height="48">
  <path d="M18 8l-4 4H8a4 4 0 0 0-4 4v20a4 4 0 0 0 4 4h32a4 4 0 0 0 4-4V16a4 4 0 0 0-4-4h-6l-4-4H18zm6 26a10 10 0 1 1 0-20 10 10 0 0 1 0 20z" fill="#FFFFFF"/>
</svg>
`, path.join(ICONS_DIR, 'camera.png'), 48, 48);

// 铅笔编辑 (Edit)
svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="48" height="48">
  <path d="M6 34v8h8l24-24-8-8L6 34zm34.7-21.3a2.1 2.1 0 0 0 0-3l-3.7-3.7a2.1 2.1 0 0 0-3 0l-3 3 6.7 6.7 3-3z" fill="#8E8E93"/>
</svg>
`, path.join(ICONS_DIR, 'edit.png'), 48, 48);

// 身份切换 (Switch Role)
svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="48" height="48">
  <circle cx="24" cy="24" r="20" fill="#2C2C2E"/>
  <path d="M16 18h16m0 0l-6-6m6 6l-6 6M32 30H16m0 0l6-6m-6 6l6 6" fill="none" stroke="#FFFFFF" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
</svg>
`, path.join(ICONS_DIR, 'switch-role.png'), 48, 48);

// 日历排班 (Calendar)
svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="48" height="48">
  <rect x="6" y="8" width="36" height="36" rx="6" fill="none" stroke="#34C759" stroke-width="4"/>
  <line x1="16" y1="4" x2="16" y2="12" stroke="#34C759" stroke-width="4" stroke-linecap="round"/>
  <line x1="32" y1="4" x2="32" y2="12" stroke="#34C759" stroke-width="4" stroke-linecap="round"/>
  <line x1="6" y1="20" x2="42" y2="20" stroke="#34C759" stroke-width="4"/>
</svg>
`, path.join(ICONS_DIR, 'calendar.png'), 48, 48);

// 订单 (Order Document)
svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="64" height="64">
  <path d="M12 6h18l10 10v24a3 3 0 0 1-3 3H12a3 3 0 0 1-3-3V9a3 3 0 0 1 3-3z" fill="#007AFF"/>
  <path d="M30 6v10h10" fill="#5AC8FA"/>
  <line x1="17" y1="24" x2="31" y2="24" stroke="#FFFFFF" stroke-width="3" stroke-linecap="round"/>
  <line x1="17" y1="31" x2="31" y2="31" stroke="#FFFFFF" stroke-width="3" stroke-linecap="round"/>
  <line x1="17" y1="38" x2="25" y2="38" stroke="#FFFFFF" stroke-width="3" stroke-linecap="round"/>
</svg>
`, path.join(ICONS_DIR, 'order.png'), 64, 64);

// 地址 (Address Location)
svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="64" height="64">
  <path d="M24 4C15.2 4 8 11.2 8 20c0 12 16 24 16 24s16-12 16-24c0-8.8-7.2-16-16-16zm0 21.5a5.5 5.5 0 1 1 0-11 5.5 5.5 0 0 1 0 11z" fill="#34C759"/>
  <circle cx="24" cy="19.5" r="5" fill="#FFFFFF"/>
</svg>
`, path.join(ICONS_DIR, 'address.png'), 64, 64);

// 优惠券 (Coupon Ticket)
svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="64" height="64">
  <path d="M6 14h36a2 2 0 0 1 2 2v4a4 4 0 0 0 0 8v4a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-4a4 4 0 0 0 0-8v-4a2 2 0 0 1 2-2z" fill="#FF9500"/>
  <line x1="18" y1="16" x2="18" y2="32" stroke="#FFFFFF" stroke-width="2.5" stroke-dasharray="3 3"/>
  <circle cx="31" cy="24" r="5" fill="#FFFFFF"/>
</svg>
`, path.join(ICONS_DIR, 'coupon.png'), 64, 64);

// 邀请 (Invite Cash)
svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="64" height="64">
  <circle cx="24" cy="24" r="20" fill="#FF382E"/>
  <text x="24" y="32" font-family="-apple-system, sans-serif" font-size="24" font-weight="900" fill="#FFFFFF" text-anchor="middle">¥</text>
</svg>
`, path.join(ICONS_DIR, 'invite.png'), 64, 64);

// 消息通知 (Notice Bell)
svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="64" height="64">
  <path d="M24 44c2.5 0 4.5-2 4.5-4.5h-9c0 2.5 2 4.5 4.5 4.5zm14-13.5v-11c0-7.2-4.8-13.2-11.5-14.7V4c0-1.4-1.1-2.5-2.5-2.5S21.5 2.6 21.5 4v.8C14.8 6.3 10 12.3 10 19.5v11L6 34.5v2.5h36v-2.5l-4-4z" fill="#AF52DE"/>
</svg>
`, path.join(ICONS_DIR, 'notice.png'), 64, 64);

// 身份通道 (Role Switch)
svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="64" height="64">
  <circle cx="24" cy="24" r="20" fill="#1D1D1F"/>
  <path d="M15 19h18m0 0l-5-5m5 5l-5 5M33 29H15m0 0l5-5m-5 5l5 5" fill="none" stroke="#FFFFFF" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
</svg>
`, path.join(ICONS_DIR, 'role.png'), 64, 64);

// 成为技师 (Join Therapist)
svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="64" height="64">
  <circle cx="24" cy="24" r="20" fill="#30B0C7"/>
  <circle cx="24" cy="18" r="6" fill="#FFFFFF"/>
  <path d="M14 36c1.5-5 5.5-7 10-7s8.5 2 10 7" fill="#FFFFFF"/>
</svg>
`, path.join(ICONS_DIR, 'join.png'), 64, 64);

// 微信图标 (WeChat Logo)
svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="64" height="64">
  <circle cx="24" cy="24" r="20" fill="#07C160"/>
  <ellipse cx="20" cy="21" rx="10" ry="8" fill="#FFFFFF"/>
  <circle cx="17" cy="20" r="1.5" fill="#07C160"/>
  <circle cx="23" cy="20" r="1.5" fill="#07C160"/>
  <ellipse cx="29" cy="28" rx="8" ry="6.5" fill="#FFFFFF"/>
  <circle cx="26.5" cy="27" r="1.2" fill="#07C160"/>
  <circle cx="31.5" cy="27" r="1.2" fill="#07C160"/>
</svg>
`, path.join(ICONS_DIR, 'wechat.png'), 64, 64);

// 手机图标 (Phone)
svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="48" height="48">
  <rect x="13" y="6" width="22" height="36" rx="4" fill="none" stroke="#8E8E93" stroke-width="3"/>
  <line x1="21" y1="10" x2="27" y2="10" stroke="#8E8E93" stroke-width="2" stroke-linecap="round"/>
  <circle cx="24" cy="36" r="2" fill="#8E8E93"/>
</svg>
`, path.join(ICONS_DIR, 'phone.png'), 48, 48);

// 密码锁 (Lock)
svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="48" height="48">
  <rect x="11" y="20" width="26" height="22" rx="4" fill="#8E8E93"/>
  <path d="M17 20v-6a7 7 0 0 1 14 0v6" fill="none" stroke="#8E8E93" stroke-width="3" stroke-linecap="round"/>
  <circle cx="24" cy="30" r="2.5" fill="#FFFFFF"/>
  <path d="M24 32v4" stroke="#FFFFFF" stroke-width="2" stroke-linecap="round"/>
</svg>
`, path.join(ICONS_DIR, 'lock.png'), 48, 48);

// 眼睛 (Eye / Password show)
svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="48" height="48">
  <path d="M4 24s8-12 20-12 20 12 20 12-8 12-20 12S4 24 4 24z" fill="none" stroke="#8E8E93" stroke-width="3"/>
  <circle cx="24" cy="24" r="6" fill="#8E8E93"/>
</svg>
`, path.join(ICONS_DIR, 'eye.png'), 48, 48);

// 眼睛关闭 (Eye-off / Password hide)
svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="48" height="48">
  <path d="M4 24s8-12 20-12 20 12 20 12-8 12-20 12S4 24 4 24z" fill="none" stroke="#8E8E93" stroke-width="3"/>
  <circle cx="24" cy="24" r="6" fill="#8E8E93"/>
  <line x1="6" y1="42" x2="42" y2="6" stroke="#8E8E93" stroke-width="3.5" stroke-linecap="round"/>
</svg>
`, path.join(ICONS_DIR, 'eye-off.png'), 48, 48);


// ══════════════════════════════════════════════════
// 3. Modern Default Project Covers (400x300 PNG)
// ══════════════════════════════════════════════════

// 中医推拿封面 (Warm Cedar & Zen Stones)
svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 300" width="400" height="300">
  <defs>
    <linearGradient id="bgMassage" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="#2D2B2A"/>
      <stop offset="100%" stop-color="#1A1817"/>
    </linearGradient>
    <radialGradient id="glowMassage" cx="0.8" cy="0.2" r="0.8">
      <stop offset="0%" stop-color="rgba(255, 149, 0, 0.25)"/>
      <stop offset="100%" stop-color="transparent"/>
    </radialGradient>
  </defs>
  <rect width="400" height="300" fill="url(#bgMassage)"/>
  <rect width="400" height="300" fill="url(#glowMassage)"/>
  
  <!-- Subtle Zen circles -->
  <circle cx="200" cy="125" r="65" fill="none" stroke="rgba(255,255,255,0.06)" stroke-width="1.5"/>
  <circle cx="200" cy="125" r="85" fill="none" stroke="rgba(255,255,255,0.03)" stroke-width="1.5"/>

  <!-- Modern Acupressure Zen Stone Icon -->
  <g transform="translate(155, 75)">
    <ellipse cx="45" cy="70" rx="38" ry="14" fill="#4A4745"/>
    <ellipse cx="45" cy="54" rx="30" ry="11" fill="#6E6A67"/>
    <ellipse cx="45" cy="40" rx="22" ry="8" fill="#FF9500"/>
    <!-- Lotus petal touch -->
    <path d="M45 10 C40 25 32 30 45 35 C58 30 50 25 45 10 Z" fill="#FFE2B3"/>
  </g>

  <!-- Typography badge -->
  <rect x="135" y="195" width="130" height="28" rx="14" fill="rgba(255,255,255,0.1)"/>
  <text x="200" y="214" font-family="-apple-system, SF Pro Display, PingFang SC, sans-serif" font-size="13" font-weight="700" fill="#FFFFFF" text-anchor="middle" letter-spacing="2">中医经络推拿</text>
  <text x="200" y="248" font-family="-apple-system, SF Pro Display, PingFang SC, sans-serif" font-size="11" fill="rgba(255,255,255,0.5)" text-anchor="middle" letter-spacing="1">气血通畅 · 深度释压</text>
</svg>
`, path.join(DEFAULTS_DIR, 'default-massage.png'), 400, 300);

// 精油SPA封面 (Aromatherapy & Orchid Essence)
svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 300" width="400" height="300">
  <defs>
    <linearGradient id="bgSpa" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="#231F2A"/>
      <stop offset="100%" stop-color="#141119"/>
    </linearGradient>
    <radialGradient id="glowSpa" cx="0.8" cy="0.2" r="0.8">
      <stop offset="0%" stop-color="rgba(175, 82, 222, 0.28)"/>
      <stop offset="100%" stop-color="transparent"/>
    </radialGradient>
  </defs>
  <rect width="400" height="300" fill="url(#bgSpa)"/>
  <rect width="400" height="300" fill="url(#glowSpa)"/>

  <!-- Subtle circles -->
  <circle cx="200" cy="125" r="65" fill="none" stroke="rgba(255,255,255,0.06)" stroke-width="1.5"/>

  <!-- Modern Essential Oil Droplet & Botanical Leaf Icon -->
  <g transform="translate(160, 75)">
    <path d="M40 15 C40 15 20 45 20 58 A20 20 0 0 0 60 58 C60 45 40 15 40 15 Z" fill="#AF52DE"/>
    <ellipse cx="33" cy="52" rx="4" ry="8" transform="rotate(-30 33 52)" fill="rgba(255,255,255,0.3)"/>
    <!-- Gentle Leaf -->
    <path d="M48 65 C60 65 68 55 68 45 C58 45 48 55 48 65 Z" fill="#34C759"/>
  </g>

  <!-- Typography badge -->
  <rect x="135" y="195" width="130" height="28" rx="14" fill="rgba(255,255,255,0.1)"/>
  <text x="200" y="214" font-family="-apple-system, SF Pro Display, PingFang SC, sans-serif" font-size="13" font-weight="700" fill="#FFFFFF" text-anchor="middle" letter-spacing="2">芳香精油 SPA</text>
  <text x="200" y="248" font-family="-apple-system, SF Pro Display, PingFang SC, sans-serif" font-size="11" fill="rgba(255,255,255,0.5)" text-anchor="middle" letter-spacing="1">植萃滋润 · 舒缓宁神</text>
</svg>
`, path.join(DEFAULTS_DIR, 'default-spa.png'), 400, 300);

// 足疗保健封面 (Herbal Foot Bath & Reflexology)
svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 300" width="400" height="300">
  <defs>
    <linearGradient id="bgFoot" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="#1C2723"/>
      <stop offset="100%" stop-color="#101815"/>
    </linearGradient>
    <radialGradient id="glowFoot" cx="0.8" cy="0.2" r="0.8">
      <stop offset="0%" stop-color="rgba(52, 199, 89, 0.25)"/>
      <stop offset="100%" stop-color="transparent"/>
    </radialGradient>
  </defs>
  <rect width="400" height="300" fill="url(#bgFoot)"/>
  <rect width="400" height="300" fill="url(#glowFoot)"/>

  <!-- Subtle circles -->
  <circle cx="200" cy="125" r="65" fill="none" stroke="rgba(255,255,255,0.06)" stroke-width="1.5"/>

  <!-- Modern Herbal Basin Icon -->
  <g transform="translate(160, 80)">
    <path d="M15 50 L20 72 A10 10 0 0 0 30 80 L50 80 A10 10 0 0 0 60 72 L65 50 Z" fill="#34C759"/>
    <ellipse cx="40" cy="50" rx="25" ry="8" fill="#5CE67E"/>
    <!-- Water ripples & Herbal steam -->
    <path d="M30 40 Q35 30 30 20" stroke="#A3F0B8" stroke-width="2.5" fill="none" stroke-linecap="round"/>
    <path d="M42 38 Q47 25 42 15" stroke="#A3F0B8" stroke-width="2.5" fill="none" stroke-linecap="round"/>
    <path d="M54 40 Q59 30 54 20" stroke="#A3F0B8" stroke-width="2.5" fill="none" stroke-linecap="round"/>
  </g>

  <!-- Typography badge -->
  <rect x="135" y="195" width="130" height="28" rx="14" fill="rgba(255,255,255,0.1)"/>
  <text x="200" y="214" font-family="-apple-system, SF Pro Display, PingFang SC, sans-serif" font-size="13" font-weight="700" fill="#FFFFFF" text-anchor="middle" letter-spacing="2">经典养生足疗</text>
  <text x="200" y="248" font-family="-apple-system, SF Pro Display, PingFang SC, sans-serif" font-size="11" fill="rgba(255,255,255,0.5)" text-anchor="middle" letter-spacing="1">草本浸泡 · 足底穴位反射</text>
</svg>
`, path.join(DEFAULTS_DIR, 'default-foot.png'), 400, 300);

// 运动康复封面 (Athletic Recovery & Stretching)
svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 300" width="400" height="300">
  <defs>
    <linearGradient id="bgSports" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="#1B2430"/>
      <stop offset="100%" stop-color="#0F161E"/>
    </linearGradient>
    <radialGradient id="glowSports" cx="0.8" cy="0.2" r="0.8">
      <stop offset="0%" stop-color="rgba(0, 122, 255, 0.28)"/>
      <stop offset="100%" stop-color="transparent"/>
    </radialGradient>
  </defs>
  <rect width="400" height="300" fill="url(#bgSports)"/>
  <rect width="400" height="300" fill="url(#glowSports)"/>

  <!-- Subtle circles -->
  <circle cx="200" cy="125" r="65" fill="none" stroke="rgba(255,255,255,0.06)" stroke-width="1.5"/>

  <!-- Modern Athletic Wave Icon -->
  <g transform="translate(160, 85)">
    <circle cx="40" cy="20" r="7" fill="#007AFF"/>
    <path d="M20 60 Q30 35 40 45 T60 30" fill="none" stroke="#007AFF" stroke-width="4.5" stroke-linecap="round"/>
    <path d="M22 68 Q35 55 48 62 T68 50" fill="none" stroke="rgba(0,122,255,0.4)" stroke-width="3" stroke-linecap="round"/>
  </g>

  <!-- Typography badge -->
  <rect x="135" y="195" width="130" height="28" rx="14" fill="rgba(255,255,255,0.1)"/>
  <text x="200" y="214" font-family="-apple-system, SF Pro Display, PingFang SC, sans-serif" font-size="13" font-weight="700" fill="#FFFFFF" text-anchor="middle" letter-spacing="2">运动机能康复</text>
  <text x="200" y="248" font-family="-apple-system, SF Pro Display, PingFang SC, sans-serif" font-size="11" fill="rgba(255,255,255,0.5)" text-anchor="middle" letter-spacing="1">肌肉拉伸 · 筋膜深层激活</text>
</svg>
`, path.join(DEFAULTS_DIR, 'default-sports.png'), 400, 300);

// 通用默认封面
svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 300" width="400" height="300">
  <defs>
    <linearGradient id="bgDefault" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="#2C2C2E"/>
      <stop offset="100%" stop-color="#1C1C1E"/>
    </linearGradient>
    <radialGradient id="glowDefault" cx="0.8" cy="0.2" r="0.8">
      <stop offset="0%" stop-color="rgba(255, 56, 46, 0.2)"/>
      <stop offset="100%" stop-color="transparent"/>
    </radialGradient>
  </defs>
  <rect width="400" height="300" fill="url(#bgDefault)"/>
  <rect width="400" height="300" fill="url(#glowDefault)"/>

  <circle cx="200" cy="125" r="65" fill="none" stroke="rgba(255,255,255,0.06)" stroke-width="1.5"/>

  <!-- Modern Zen Lotus Icon -->
  <g transform="translate(160, 85)">
    <path d="M40 18 C30 35 20 48 40 60 C60 48 50 35 40 18 Z" fill="#FF5A36"/>
    <path d="M40 60 C25 55 12 45 15 32 C28 35 35 48 40 60 Z" fill="rgba(255, 90, 54, 0.7)"/>
    <path d="M40 60 C55 55 68 45 65 32 C52 35 45 48 40 60 Z" fill="rgba(255, 90, 54, 0.7)"/>
  </g>

  <!-- Typography badge -->
  <text x="200" y="214" font-family="-apple-system, SF Pro Display, PingFang SC, sans-serif" font-size="13" font-weight="700" fill="#FFFFFF" text-anchor="middle" letter-spacing="2">品质上门理疗</text>
  <text x="200" y="248" font-family="-apple-system, SF Pro Display, PingFang SC, sans-serif" font-size="11" fill="rgba(255,255,255,0.5)" text-anchor="middle" letter-spacing="1">官方认证 · 执业医师标准</text>
</svg>
`, path.join(DEFAULTS_DIR, 'default-cover.png'), 400, 300);

// Transit (Bus)
svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="64" height="64">
  <rect x="8" y="6" width="32" height="32" rx="6" fill="none" stroke="#FF382E" stroke-width="3.5"/>
  <line x1="8" y1="22" x2="40" y2="22" stroke="#FF382E" stroke-width="3.5"/>
  <circle cx="16" cy="30" r="3" fill="#FF382E"/>
  <circle cx="32" cy="30" r="3" fill="#FF382E"/>
  <line x1="14" y1="38" x2="10" y2="42" stroke="#FF382E" stroke-width="3.5" stroke-linecap="round"/>
  <line x1="34" y1="38" x2="38" y2="42" stroke="#FF382E" stroke-width="3.5" stroke-linecap="round"/>
</svg>
`, path.join(ICONS_DIR, 'transit.png'), 64, 64);

// Car
svgToPng(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="64" height="64">
  <path d="M10 32h28M10 32a4 4 0 0 1-4-4v-8a4 4 0 0 1 4-4l4-8h20l4 8a4 4 0 0 1 4 4v8a4 4 0 0 1-4 4M10 32l-2 6m30-6l2 6" stroke="#FF382E" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
  <circle cx="15" cy="26" r="3" fill="#FF382E"/>
  <circle cx="33" cy="26" r="3" fill="#FF382E"/>
</svg>
`, path.join(ICONS_DIR, 'car.png'), 64, 64);

console.log('ALL ASSETS GENERATED SUCCESSFULLY!');
