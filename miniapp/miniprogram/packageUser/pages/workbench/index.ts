import { getCachedAccount } from "../../../services/auth";

Page({
  data: {
    account: null as Account | null,
    menus: [
      { name: "服务项目", desc: "浏览所有服务项目", url: "/packageUser/pages/project-list/index" },
      { name: "技师列表", desc: "查看可预约技师", url: "/packageUser/pages/technician-list/index" },
      { name: "我的订单", desc: "查看订单状态和详情", url: "/packageUser/pages/order-list/index" },
      { name: "地址管理", desc: "管理服务地址", url: "/packageUser/pages/address-list/index" },
    ],
  },
  onShow() { this.setData({ account: getCachedAccount() }); },
  handleMenuTap(e: WechatMiniprogram.TouchEvent) { wx.navigateTo({ url: e.currentTarget.dataset.url }); },
});
