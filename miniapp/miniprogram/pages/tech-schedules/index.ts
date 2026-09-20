Page({
  data: {},
  onLoad() {
    // Redirect to the actual schedules page in subpackage
    wx.navigateTo({ url: "/packageTech/pages/schedules/index" });
  },
});
