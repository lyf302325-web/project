const app = getApp();

Page({
  data: {
    empInfo: {},
    records: [],
    month: '',
    baseUrl: '',
    userId: ''
  },

  onLoad(options) {
    if (!app.checkLogin()) return;
    if (!app.globalData.isManager) {
      wx.showToast({ title: '无权限', icon: 'none' });
      wx.navigateBack();
      return;
    }
    const { userId, month, name, employeeNo } = options;
    this.setData({
      empInfo: { name, employeeNo },
      month: month,
      userId: userId,
      baseUrl: app.globalData.baseUrl.replace('/api', '')
    });
    this.loadEmployeeInfo(userId);
    this.loadDetail(userId, month);
  },

  loadEmployeeInfo(userId) {
    app.request({
      url: `/manager/employee/detail/${userId}`,
      method: 'GET'
    }).then(res => {
      const u = res.data || {};
      this.setData({
        empInfo: {
          ...this.data.empInfo,
          name: u.name,
          employeeNo: u.employeeNo,
          departmentName: u.departmentName,
          position: u.position,
          phone: u.phone,
          hireDate: u.hireDate,
          resignDate: u.resignDate
        }
      });
    }).catch(() => {});
  },

  loadDetail(userId, month) {
    wx.showLoading({ title: '加载中...' });
    app.request({
      url: '/manager/attendance/detail',
      method: 'GET',
      data: { userId, month }
    }).then(res => {
      this.setData({
        empInfo: {
          ...this.data.empInfo,
          name: res.data.name,
          employeeNo: res.data.employeeNo
        },
        records: res.data.records || []
      });
    }).catch(err => {
      wx.showToast({ title: err.message || '加载失败', icon: 'none' });
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
  }
});
