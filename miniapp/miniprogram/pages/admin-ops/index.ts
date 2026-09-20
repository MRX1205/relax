import { getAccessToken } from "../../services/http";

Page({
  data: {
    menus: [
      { name: "分类管理", desc: "服务分类的增删改", icon: "📂", url: "/packageAdmin/pages/categories/index" },
      { name: "项目管理", desc: "项目上架、定价和上下架", icon: "💆", url: "/packageAdmin/pages/projects/index" },
      { name: "技师管理", desc: "入驻审核、定价和状态管理", icon: "👥", url: "/packageAdmin/pages/technicians/index" },
      { name: "轮播图管理", desc: "管理首页轮播图", icon: "🖼️", url: "/packageAdmin/pages/banners/index" },
      { name: "Mock数据", desc: "测试数据开关和重置", icon: "🧪", url: "/packageAdmin/pages/mock-data/index" },
    ],
  },

  onShow() {
    if (!getAccessToken()) {
      wx.reLaunch({ url: "/pages/login/index" });
    }
  },

  handleMenuTap(e: WechatMiniprogram.TouchEvent) {
    wx.navigateTo({ url: e.currentTarget.dataset.url });
  },
});
