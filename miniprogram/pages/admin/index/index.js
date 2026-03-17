const app = getApp();

Page({
  data: {
    userInfo: {},
    todayStats: {},
    pendingLeave: 0,
    pendingFieldwork: 0,
    pendingAppeal: 0
  },

  onShow() {
    if (!app.checkLogin()) return;
    this.setData({
      userInfo: app.globalData.userInfo || {}
    });
    this.loadTodayStats();
    this.loadPendingCount();
  },

  loadTodayStats() {
    app.request({
      url: '/admin/today-stats',
      method: 'GET'
    }).then(res => {
      this.setData({ todayStats: res.data || {} });
    }).catch(err => {
      console.error('获取今日统计失败', err);
    });
  },

  loadPendingCount() {
    app.request({
      url: '/admin/pending-count',
      method: 'GET'
    }).then(res => {
      this.setData({
        pendingLeave: res.data.leave || 0,
        pendingFieldwork: res.data.fieldwork || 0,
        pendingAppeal: res.data.appeal || 0
      });
    }).catch(err => {
      console.error('获取待处理数量失败', err);
    });
  },

  goToApproval() {
    wx.navigateTo({ url: '/pages/admin/approval/approval?type=leave' });
  },

  goToFieldworkApproval() {
    wx.navigateTo({ url: '/pages/admin/approval/approval?type=fieldwork' });
  },

  goToAppealApproval() {
    wx.navigateTo({ url: '/pages/admin/approval/approval?type=appeal' });
  },

  goToEmployee() {
    wx.navigateTo({ url: '/pages/admin/employee/employee' });
  },

  goToDepartment() {
    wx.navigateTo({ url: '/pages/admin/department/department' });
  },

  goToRule() {
    wx.navigateTo({ url: '/pages/admin/rule/rule' });
  },

  goToApprovalConfig() {
    wx.navigateTo({ url: '/pages/admin/approval/config/config' });
  },

  goToStatistics() {
    wx.navigateTo({ url: '/pages/admin/statistics/statistics' });
  },

  goToLog() {
    wx.navigateTo({ url: '/pages/admin/log/log' });
  },

  goToNotice() {
    wx.navigateTo({ url: '/pages/admin/notice/notice' });
  },

  goToLeaveQuota() {
    wx.navigateTo({ url: '/pages/admin/leave_quota/leave_quota' });
  },

  goToOvertimeManage() {
    wx.navigateTo({ url: '/pages/admin/overtime/overtime' });
  },

  goToRegisterAudit() {
    wx.navigateTo({ url: '/pages/admin/register_audit/register_audit' });
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
