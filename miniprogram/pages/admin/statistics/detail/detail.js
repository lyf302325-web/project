const app = getApp();

Page({
  data: {
    empInfo: {},
    records: [],
    month: '',
    baseUrl: '',
    userId: '',
    showModal: false,
    correctIndex: -1,
    correctData: {},
    checkInStatusList: [
      { name: '正常', value: 0 },
      { name: '迟到', value: 1 }
    ],
    checkOutStatusList: [
      { name: '正常', value: 0 },
      { name: '早退', value: 2 }
    ]
  },

  onLoad(options) {
    const { userId, month, name, employeeNo } = options;
    this.setData({
      empInfo: { name, employeeNo },
      month: month,
      userId: userId,
      baseUrl: app.globalData.baseUrl.replace('/api', '')
    });
    this.loadDetail(userId, month);
  },

  loadDetail(userId, month) {
    wx.showLoading({ title: '加载中...' });
    app.request({
      url: '/admin/attendance/detail',
      method: 'GET',
      data: { userId, month }
    }).then(res => {
      this.setData({
        empInfo: {
          name: res.data.name,
          employeeNo: res.data.employeeNo
        },
        records: res.data.records || []
      });
    }).catch(err => {
      wx.showToast({ title: '加载失败', icon: 'none' });
    }).finally(() => {
      wx.hideLoading();
    });
  },

  previewPhoto(e) {
    const url = this.data.baseUrl + e.currentTarget.dataset.url;
    wx.previewImage({
      urls: [url],
      current: url
    });
  },

  showCorrectModal(e) {
    const index = e.currentTarget.dataset.index;
    const item = e.currentTarget.dataset.item;
    const checkInStatusIndex = item.checkInStatus === 1 ? 1 : 0;
    const checkOutStatusIndex = item.checkOutStatus === 2 ? 1 : 0;
    this.setData({
      showModal: true,
      correctIndex: index,
      correctData: {
        id: item.id,
        attendanceDate: item.attendanceDate,
        checkInTime: item.checkInTime || '',
        checkOutTime: item.checkOutTime || '',
        checkInStatus: item.checkInStatus,
        checkOutStatus: item.checkOutStatus,
        checkInStatusIndex: checkInStatusIndex,
        checkOutStatusIndex: checkOutStatusIndex
      }
    });
  },

  closeModal() {
    this.setData({ showModal: false });
  },

  onCheckInTimeChange(e) {
    this.setData({ 'correctData.checkInTime': e.detail.value });
  },

  onCheckOutTimeChange(e) {
    this.setData({ 'correctData.checkOutTime': e.detail.value });
  },

  onCheckInStatusChange(e) {
    const index = e.detail.value;
    this.setData({
      'correctData.checkInStatusIndex': index,
      'correctData.checkInStatus': this.data.checkInStatusList[index].value
    });
  },

  onCheckOutStatusChange(e) {
    const index = e.detail.value;
    this.setData({
      'correctData.checkOutStatusIndex': index,
      'correctData.checkOutStatus': this.data.checkOutStatusList[index].value
    });
  },

  submitCorrect() {
    const { correctData } = this.data;
    wx.showLoading({ title: '提交中...' });
    app.request({
      url: '/admin/attendance/correct',
      method: 'POST',
      data: {
        id: correctData.id,
        checkInTime: correctData.checkInTime || null,
        checkOutTime: correctData.checkOutTime || null,
        checkInStatus: correctData.checkInStatus,
        checkOutStatus: correctData.checkOutStatus
      }
    }).then(res => {
      wx.showToast({ title: '修正成功', icon: 'success' });
      this.closeModal();
      this.loadDetail(this.data.userId, this.data.month);
    }).catch(err => {
      wx.showToast({ title: err.message || '修正失败', icon: 'none' });
    }).finally(() => {
      wx.hideLoading();
    });
  }
});
