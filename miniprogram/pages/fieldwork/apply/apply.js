const app = getApp();

Page({
  data: {
    fieldworkDate: '',
    location: '',
    reason: '',
    images: [],
    loading: false
  },

  onLoad() {
    if (!app.checkLogin()) return;
  },

  onDateChange(e) {
    this.setData({ fieldworkDate: e.detail.value });
  },

  onLocationInput(e) {
    this.setData({ location: e.detail.value });
  },

  onReasonInput(e) {
    this.setData({ reason: e.detail.value });
  },

  chooseImage() {
    wx.chooseImage({
      count: 3 - this.data.images.length,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        this.setData({
          images: [...this.data.images, ...res.tempFilePaths]
        });
      }
    });
  },

  deleteImage(e) {
    const index = e.currentTarget.dataset.index;
    const images = this.data.images;
    images.splice(index, 1);
    this.setData({ images });
  },

  submitApply() {
    const { fieldworkDate, location, reason, images } = this.data;

    if (!fieldworkDate) {
      wx.showToast({ title: '请选择外勤日期', icon: 'none' });
      return;
    }
    if (!location.trim()) {
      wx.showToast({ title: '请输入外勤地点', icon: 'none' });
      return;
    }
    if (!reason.trim()) {
      wx.showToast({ title: '请输入外勤事由', icon: 'none' });
      return;
    }

    this.setData({ loading: true });

    app.request({
      url: '/fieldwork/apply',
      method: 'POST',
      data: {
        fieldworkDate,
        location,
        reason,
        images
      }
    }).then(res => {
      wx.showToast({ title: '申请提交成功', icon: 'success' });
      setTimeout(() => {
        wx.navigateBack();
      }, 1500);
    }).catch(err => {
      wx.showToast({ title: err.message || '提交失败', icon: 'none' });
    }).finally(() => {
      this.setData({ loading: false });
    });
  }
});
