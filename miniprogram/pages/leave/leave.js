const app = getApp();

Page({
  data: {
    currentTab: 'all',
    leaveList: []
  },

  onShow() {
    if (!app.checkLogin()) return;
    this.loadLeaveList();
  },

  switchTab(e) {
    const tab = e.currentTarget.dataset.tab;
    this.setData({ currentTab: tab });
    this.loadLeaveList();
  },

  loadLeaveList() {
    const { currentTab } = this.data;
    let status = null;
    if (currentTab === 'pending') status = 0;
    else if (currentTab === 'approved') status = 1;
    else if (currentTab === 'rejected') status = 2;

    app.request({
      url: '/leave/list',
      method: 'GET',
      data: status !== null ? { status } : {}
    }).then(res => {
      this.setData({ leaveList: res.data || [] });
    }).catch(err => {
      console.error('获取请假列表失败', err);
    });
  },

  goToApply() {
    wx.navigateTo({ url: '/pages/leave/apply/apply' });
  },

  goToDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/leave/detail/detail?id=${id}` });
  }
});
