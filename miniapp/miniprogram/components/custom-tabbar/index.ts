import { getCachedAccount } from "../../services/auth";

interface TabItem {
  key: string;
  pagePath: string;
  text: string;
  icon: string;
}

const ROLE_TABS: Record<string, TabItem[]> = {
  USER: [
    { key: "home", pagePath: "/pages/home/index", text: "首页", icon: "🏠" },
    { key: "projects", pagePath: "/pages/tab-browse/index", text: "项目", icon: "💆" },
    { key: "technicians", pagePath: "/pages/tab-mid/index", text: "技师", icon: "👤" },
    { key: "orders", pagePath: "/pages/tab-last/index", text: "订单", icon: "📋" },
    { key: "account", pagePath: "/pages/account/index", text: "我的", icon: "👤" },
  ],
  TECHNICIAN: [
    { key: "workbench", pagePath: "/pages/home/index", text: "工作台", icon: "🔧" },
    { key: "orders", pagePath: "/pages/tab-browse/index", text: "订单", icon: "📋" },
    { key: "schedules", pagePath: "/pages/tab-mid/index", text: "排班", icon: "📅" },
    { key: "income", pagePath: "/pages/tab-last/index", text: "收入", icon: "💰" },
    { key: "account", pagePath: "/pages/account/index", text: "我的", icon: "👤" },
  ],
  ADMIN: [
    { key: "workbench", pagePath: "/pages/home/index", text: "工作台", icon: "⚙️" },
    { key: "orders", pagePath: "/pages/tab-browse/index", text: "订单", icon: "📋" },
    { key: "ops", pagePath: "/pages/tab-mid/index", text: "运营", icon: "📊" },
    { key: "finance", pagePath: "/pages/tab-last/index", text: "财务", icon: "💳" },
    { key: "account", pagePath: "/pages/account/index", text: "我的", icon: "👤" },
  ],
  SUPER_ADMIN: [
    { key: "workbench", pagePath: "/pages/home/index", text: "工作台", icon: "⚙️" },
    { key: "orders", pagePath: "/pages/tab-browse/index", text: "订单", icon: "📋" },
    { key: "ops", pagePath: "/pages/tab-mid/index", text: "运营", icon: "📊" },
    { key: "finance", pagePath: "/pages/tab-last/index", text: "财务", icon: "💳" },
    { key: "account", pagePath: "/pages/account/index", text: "我的", icon: "👤" },
  ],
};

Component({
  data: {
    tabs: [] as TabItem[],
    activeKey: "",
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
      const account = getCachedAccount();
      const role = account?.lastRole || "USER";
      const tabs = ROLE_TABS[role] || ROLE_TABS.USER;

      const pages = getCurrentPages();
      const currentPage = pages[pages.length - 1];
      const currentPath = "/" + (currentPage?.route || "");

      const activeTab = tabs.find(t => t.pagePath === currentPath);
      this.setData({
        tabs,
        activeKey: activeTab?.key || tabs[0].key,
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
