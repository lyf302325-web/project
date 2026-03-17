const app = getApp();

Page({
  data: {
    id: null,
    detail: {}
  },

  onLoad(options) {
    if (!app.checkLogin()) return;
    this.setData({ id: options.id });
    this.loadDetail();
  },

  loadDetail() {
    app.request({
      url: `/leave/detail/${this.data.id}`,
      method: 'GET'
    }).then(res => {
      this.setData({ detail: res.data || {} });
    }).catch(err => {
      console.error('获取详情失败', err);
    });
  },

  previewImage(e) {
    const url = e.currentTarget.dataset.url;
    wx.previewImage({
      current: url,
      urls: this.data.detail.images
    });
  },

  cancelApply() {
    wx.showModal({
      title: '提示',
      content: '确定要撤销此申请吗？',
      success: (res) => {
        if (res.confirm) {
          app.request({
            url: `/leave/cancel/${this.data.id}`,
            method: 'POST'
          }).then(res => {
            wx.showToast({ title: '撤销成功', icon: 'success' });
            setTimeout(() => {
              wx.navigateBack();
            }, 1500);
          }).catch(err => {
            wx.showToast({ title: err.message || '撤销失败', icon: 'none' });
          });
        }
      }
    });
  }
});
