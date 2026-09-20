Page({
  data: {
    menus: [
      { code: "categories", name: "分类管理", desc: "服务分类的增删改", url: "/packageAdmin/pages/categories/index" },
      { code: "projects", name: "项目管理", desc: "项目上架、定价和上下架", url: "/packageAdmin/pages/projects/index" },
      { code: "technicians", name: "技师管理", desc: "入驻审核、定价和状态管理", url: "/packageAdmin/pages/technicians/index" },
      { code: "orders", name: "订单管理", desc: "查看和处理订单", url: "/packageAdmin/pages/order-list/index" },
      { code: "refunds", name: "退款审核", desc: "审核退款申请", url: "/packageAdmin/pages/refunds/index" },
      { code: "settlements", name: "结算管理", desc: "技师结算和付款", url: "/packageAdmin/pages/settlements/index" },
      { code: "banners", name: "轮播图", desc: "管理首页轮播图", url: "/packageAdmin/pages/banners/index" },
    ],
  },
  handleMenuTap(e: WechatMiniprogram.TouchEvent) { wx.navigateTo({ url: e.currentTarget.dataset.url }); },
});
