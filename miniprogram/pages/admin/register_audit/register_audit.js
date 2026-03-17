const app = getApp();

Page({
  data: {
    list: [],
    loading: false
  },

  onShow() {
    if (!app.checkLogin()) return;
    if (!app.globalData.isAdmin) {
      wx.showToast({ title: '无权限', icon: 'none' });
      wx.navigateBack();
      return;
    }
    this.loadList();
  },

  loadList() {
    this.setData({ loading: true });
    app.request({
      url: '/admin/employee/pending',
      method: 'GET'
    }).then(res => {
      this.setData({ list: res.data || [] });
    }).catch(err => {
      wx.showToast({ title: err.message || '加载失败', icon: 'none' });
    }).finally(() => {
      this.setData({ loading: false });
    });
  },

  handleApprove(e) {
    const id = e.currentTarget.dataset.id;
    this.updateStatus(id, 1);
  },

  handleReject(e) {
    const id = e.currentTarget.dataset.id;
    this.updateStatus(id, 2);
  },

  updateStatus(id, registerStatus) {
    wx.showLoading({ title: '处理中...' });
    app.request({
      url: `/admin/employee/approve/${id}`,
      method: 'POST',
      data: { registerStatus }
    }).then(() => {
      wx.showToast({ title: registerStatus === 1 ? '已通过' : '已驳回', icon: 'success' });
      this.loadList();
    }).catch(err => {
      wx.showToast({ title: err.message || '操作失败', icon: 'none' });
    }).finally(() => {
      wx.hideLoading();
    });
  }
});
