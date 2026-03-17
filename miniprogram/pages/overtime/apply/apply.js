const app = getApp();

Page({
  data: {
    overtimeTypes: [
      { id: 1, name: '工作日' },
      { id: 2, name: '周末' },
      { id: 3, name: '节假日' }
    ],
    selectedType: {},
    startDate: '',
    startTime: '',
    endDate: '',
    endTime: '',
    reason: '',
    images: [],
    uploading: false,
    loading: false
  },

  onLoad() {
    if (!app.checkLogin()) return;
  },

  onTypeChange(e) {
    const index = e.detail.value;
    this.setData({ selectedType: this.data.overtimeTypes[index] });
  },

  onStartDateChange(e) {
    this.setData({ startDate: e.detail.value });
  },

  onStartTimeChange(e) {
    this.setData({ startTime: e.detail.value });
  },

  onEndDateChange(e) {
    this.setData({ endDate: e.detail.value });
  },

  onEndTimeChange(e) {
    this.setData({ endTime: e.detail.value });
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
        const files = res.tempFilePaths || [];
        if (files.length === 0) return;
        this.uploadImages(files);
      }
    });
  },

  uploadImages(files) {
    this.setData({ uploading: true });
    const uploaded = [];
    const uploadOne = (i) => {
      if (i >= files.length) {
        this.setData({ images: [...this.data.images, ...uploaded], uploading: false });
        return;
      }
      wx.uploadFile({
        url: app.globalData.baseUrl + '/file/upload',
        filePath: files[i],
        name: 'file',
        header: {
          'Authorization': app.globalData.token ? 'Bearer ' + app.globalData.token : ''
        },
        success: (uploadRes) => {
          try {
            const data = JSON.parse(uploadRes.data);
            if (data.code === 200) {
              uploaded.push(data.data);
            }
          } catch (e) {}
        },
        complete: () => uploadOne(i + 1)
      });
    };
    uploadOne(0);
  },

  deleteImage(e) {
    const index = e.currentTarget.dataset.index;
    const images = this.data.images;
    images.splice(index, 1);
    this.setData({ images });
  },

  submitApply() {
    const { selectedType, startDate, startTime, endDate, endTime, reason, images, uploading } = this.data;

    if (uploading) {
      wx.showToast({ title: '图片上传中', icon: 'none' });
      return;
    }
    if (!selectedType.id) {
      wx.showToast({ title: '请选择加班类型', icon: 'none' });
      return;
    }
    if (!startDate || !startTime) {
      wx.showToast({ title: '请选择开始时间', icon: 'none' });
      return;
    }
    if (!endDate || !endTime) {
      wx.showToast({ title: '请选择结束时间', icon: 'none' });
      return;
    }
    if (!reason.trim()) {
      wx.showToast({ title: '请输入加班事由', icon: 'none' });
      return;
    }

    this.setData({ loading: true });

    app.request({
      url: '/overtime/apply',
      method: 'POST',
      data: {
        overtimeType: selectedType.id,
        startTime: `${startDate} ${startTime}`,
        endTime: `${endDate} ${endTime}`,
        reason,
        images
      }
    }).then(() => {
      wx.showToast({ title: '提交成功', icon: 'success' });
      setTimeout(() => wx.navigateBack(), 1200);
    }).catch(err => {
      wx.showToast({ title: err.message || '提交失败', icon: 'none' });
    }).finally(() => {
      this.setData({ loading: false });
    });
  }
});

