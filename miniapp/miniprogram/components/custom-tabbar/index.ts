interface TabItem {
  key: string;
  pagePath: string;
  text: string;
  iconNormal: string;
  iconActive: string;
}

const USER_TABS: TabItem[] = [
  {
    key: "home",
    pagePath: "/pages/home/index",
    text: "首页",
    iconNormal: "/assets/tabbar/home.png",
    iconActive: "/assets/tabbar/home-active.png",
  },
  {
    key: "projects",
    pagePath: "/pages/tab-browse/index",
    text: "发现项目",
    iconNormal: "/assets/tabbar/projects.png",
    iconActive: "/assets/tabbar/projects-active.png",
  },
  {
    key: "technicians",
    pagePath: "/pages/tab-mid/index",
    text: "找技师",
    iconNormal: "/assets/tabbar/technicians.png",
    iconActive: "/assets/tabbar/technicians-active.png",
  },
  {
    key: "orders",
    pagePath: "/pages/tab-last/index",
    text: "订单",
    iconNormal: "/assets/tabbar/orders.png",
    iconActive: "/assets/tabbar/orders-active.png",
  },
  {
    key: "account",
    pagePath: "/pages/account/index",
    text: "我的",
    iconNormal: "/assets/tabbar/account.png",
    iconActive: "/assets/tabbar/account-active.png",
  },
];

Component({
  data: {
    tabs: USER_TABS,
    activeKey: "home",
  },

  lifetimes: {
    attached() {
      this.refreshTabs();
    },
  },

  pageLifetimes: {
    show() {
      this.refreshTabs();
    },
  },

  methods: {
    refreshTabs() {
      const pages = getCurrentPages();
      const currentPage = pages[pages.length - 1];
      const currentPath = "/" + (currentPage?.route || "");

      const activeTab = this.data.tabs.find(t => t.pagePath === currentPath);
      this.setData({
        activeKey: activeTab?.key || "home",
      });
    },

    handleTap(e: WechatMiniprogram.TouchEvent) {
      const url = e.currentTarget.dataset.path;
      const key = e.currentTarget.dataset.key;
      if (key === this.data.activeKey) return;

      wx.switchTab({ url });
    },
  },
});
