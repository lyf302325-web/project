const app = getApp();

Page({
  data: {
    currentTab: 'all',
    appealList: []
  },

  onShow() {
    if (!app.checkLogin()) return;
    this.loadAppealList();
  },

  switchTab(e) {
    const tab = e.currentTarget.dataset.tab;
    this.setData({ currentTab: tab });
    this.loadAppealList();
  },

  loadAppealList() {
    const { currentTab } = this.data;
    let status = null;
    if (currentTab === 'pending') status = 0;
    else if (currentTab === 'approved') status = 1;
    else if (currentTab === 'rejected') status = 2;

    app.request({
      url: '/appeal/list',
      method: 'GET',
      data: status !== null ? { status } : {}
    }).then(res => {
      this.setData({ appealList: res.data || [] });
    }).catch(err => {
      console.error('获取申诉列表失败', err);
    });
  },

  goToApply() {
    wx.navigateTo({ url: '/pages/appeal/apply/apply' });
  }
});
