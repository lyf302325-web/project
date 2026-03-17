const app = getApp();

Page({
  data: {
    year: '',
    list: [],
    loading: false
  },

  onShow() {
    if (!app.checkLogin()) return;
    const y = new Date().getFullYear();
    this.setData({ year: String(y) });
    this.loadList();
  },

  loadList() {
    this.setData({ loading: true });
    app.request({
      url: '/user/leave-balance',
      method: 'GET',
      data: { year: this.data.year }
    }).then(res => {
      this.setData({ list: res.data || [] });
    }).catch(err => {
      wx.showToast({ title: err.message || '加载失败', icon: 'none' });
    }).finally(() => {
      this.setData({ loading: false });
    });
  }
});
