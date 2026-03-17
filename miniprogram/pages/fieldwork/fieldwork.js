const app = getApp();

Page({
  data: {
    currentTab: 'all',
    fieldworkList: []
  },

  onShow() {
    if (!app.checkLogin()) return;
    this.loadFieldworkList();
  },

  switchTab(e) {
    const tab = e.currentTarget.dataset.tab;
    this.setData({ currentTab: tab });
    this.loadFieldworkList();
  },

  loadFieldworkList() {
    const { currentTab } = this.data;
    let status = null;
    if (currentTab === 'pending') status = 0;
    else if (currentTab === 'approved') status = 1;
    else if (currentTab === 'rejected') status = 2;

    app.request({
      url: '/fieldwork/list',
      method: 'GET',
      data: status !== null ? { status } : {}
    }).then(res => {
      this.setData({ fieldworkList: res.data || [] });
    }).catch(err => {
      console.error('获取外勤列表失败', err);
    });
  },

  goToApply() {
    wx.navigateTo({ url: '/pages/fieldwork/apply/apply' });
  }
});
