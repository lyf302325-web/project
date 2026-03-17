const app = getApp();

Page({
  data: {
    currentTab: 'all',
    list: []
  },

  onShow() {
    if (!app.checkLogin()) return;
    this.loadList();
  },

  switchTab(e) {
    const tab = e.currentTarget.dataset.tab;
    this.setData({ currentTab: tab });
    this.loadList();
  },

  loadList() {
    let status = null;
    if (this.data.currentTab === 'pending') status = 0;
    else if (this.data.currentTab === 'approved') status = 3;
    else if (this.data.currentTab === 'rejected') status = 2;
    else if (this.data.currentTab === 'canceled') status = 4;

    wx.showLoading({ title: '加载中...' });
    app.request({
      url: '/overtime/list',
      method: 'GET',
      data: status == null ? {} : { status }
    }).then(res => {
      const list = (res.data || []).map(item => ({
        ...item,
        statusText: this.getStatusText(item.status)
      }));
      this.setData({ list });
    }).catch(err => {
      wx.showToast({ title: err.message || '加载失败', icon: 'none' });
    }).finally(() => {
      wx.hideLoading();
    });
  },

  goToApply() {
    wx.navigateTo({ url: '/pages/overtime/apply/apply' });
  },

  cancelApply(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '提示',
      content: '确定要撤销该加班申请吗？',
      success: (res) => {
        if (!res.confirm) return;
        wx.showLoading({ title: '处理中...' });
        app.request({
          url: `/overtime/cancel/${id}`,
          method: 'POST'
        }).then(() => {
          wx.showToast({ title: '已撤销', icon: 'success' });
          this.loadList();
        }).catch(err => {
          wx.showToast({ title: err.message || '操作失败', icon: 'none' });
        }).finally(() => {
          wx.hideLoading();
        });
      }
    });
  },

  getStatusText(status) {
    if (status === 0) return '审批中';
    if (status === 1) return '待终审';
    if (status === 2) return '已拒绝';
    if (status === 3) return '已通过';
    if (status === 4) return '已撤销';
    return '';
  }
});
