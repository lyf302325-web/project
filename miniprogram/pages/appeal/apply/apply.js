const app = getApp();

Page({
  data: {
    appealTypes: [
      { id: 1, name: '漏签到' },
      { id: 2, name: '漏签退' },
      { id: 3, name: '定位失败' },
      { id: 4, name: '设备故障' },
      { id: 5, name: '其他' }
    ],
    selectedType: {},
    attendanceDate: '',
    reason: '',
    images: [],
    loading: false
  },

  onLoad() {
    if (!app.checkLogin()) return;
  },

  onDateChange(e) {
    this.setData({ attendanceDate: e.detail.value });
  },

  onTypeChange(e) {
    const index = e.detail.value;
    this.setData({
      selectedType: this.data.appealTypes[index]
    });
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
    const { attendanceDate, selectedType, reason, images } = this.data;

    if (!attendanceDate) {
      wx.showToast({ title: '请选择考勤日期', icon: 'none' });
      return;
    }
    if (!selectedType.id) {
      wx.showToast({ title: '请选择申诉类型', icon: 'none' });
      return;
    }
    if (!reason.trim()) {
      wx.showToast({ title: '请输入申诉原因', icon: 'none' });
      return;
    }

    this.setData({ loading: true });

    app.request({
      url: '/appeal/apply',
      method: 'POST',
      data: {
        attendanceDate,
        appealType: selectedType.id,
        reason,
        images
      }
    }).then(res => {
      wx.showToast({ title: '申诉提交成功', icon: 'success' });
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
