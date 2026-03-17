const app = getApp();

Page({
  data: {
    userInfo: {},
    departmentName: '',
    employeeCount: 0,
    pendingLeave: 0,
    pendingFieldwork: 0,
    pendingAppeal: 0,
    pendingOvertime: 0,
    todayAbnormal: 0
  },

  onShow() {
    if (!app.checkLogin()) return;
    if (!app.globalData.isManager) {
      wx.showToast({ title: '无权限', icon: 'none' });
      wx.reLaunch({ url: '/pages/index/index' });
      return;
    }
    this.setData({
      userInfo: app.globalData.userInfo || {}
    });
    this.loadHome();
  },

  loadHome() {
    app.request({
      url: '/manager/home',
      method: 'GET'
    }).then(res => {
      const data = res.data || {};
      const pending = data.pending || {};
      this.setData({
        departmentName: data.departmentName || '',
        employeeCount: data.employeeCount || 0,
        pendingLeave: pending.leave || 0,
        pendingFieldwork: pending.fieldwork || 0,
        pendingAppeal: pending.appeal || 0,
        pendingOvertime: pending.overtime || 0,
        todayAbnormal: data.todayAbnormal || 0
      });
    }).catch(err => {
      wx.showToast({ title: err.message || '加载失败', icon: 'none' });
    });
  },

  goToLeaveApproval() {
    wx.navigateTo({ url: '/pages/manager/approval/approval?type=leave' });
  },

  goToFieldworkApproval() {
    wx.navigateTo({ url: '/pages/manager/approval/approval?type=fieldwork' });
  },

  goToAppealApproval() {
    wx.navigateTo({ url: '/pages/manager/approval/approval?type=appeal' });
  },

  goToOvertimeApproval() {
    wx.navigateTo({ url: '/pages/manager/approval/approval?type=overtime' });
  },

  goToApprovalRecords() {
    wx.navigateTo({ url: '/pages/manager/records/records' });
  },

  goToStatistics() {
    wx.navigateTo({ url: '/pages/manager/statistics/statistics' });
  },

  goToEmployees() {
    wx.navigateTo({ url: '/pages/manager/employee/employee' });
  },

  goToNotice() {
    wx.navigateTo({ url: '/pages/manager/notice/notice' });
  },

  handleLogout() {
    wx.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          wx.removeStorageSync('token');
          wx.removeStorageSync('userInfo');
          app.globalData.token = null;
          app.globalData.userInfo = null;
          app.globalData.isAdmin = false;
          app.globalData.isManager = false;
          wx.redirectTo({ url: '/pages/login/login' });
        }
      }
    });
  }
});
