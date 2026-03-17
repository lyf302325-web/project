const app = getApp();

Page({
  data: {
    userInfo: {},
    isAdmin: false,
    showPasswordModal: false,
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  },

  onShow() {
    if (!app.checkLogin()) return;
    this.setData({
      userInfo: app.globalData.userInfo || {},
      isAdmin: app.globalData.isAdmin
    });
  },

  goToChangePassword() {
    this.setData({ showPasswordModal: true });
  },

  closePasswordModal() {
    this.setData({
      showPasswordModal: false,
      oldPassword: '',
      newPassword: '',
      confirmPassword: ''
    });
  },

  onOldPasswordInput(e) {
    this.setData({ oldPassword: e.detail.value });
  },

  onNewPasswordInput(e) {
    this.setData({ newPassword: e.detail.value });
  },

  onConfirmPasswordInput(e) {
    this.setData({ confirmPassword: e.detail.value });
  },

  submitChangePassword() {
    const { oldPassword, newPassword, confirmPassword } = this.data;

    if (!oldPassword) {
      wx.showToast({ title: '请输入原密码', icon: 'none' });
      return;
    }
    if (!newPassword) {
      wx.showToast({ title: '请输入新密码', icon: 'none' });
      return;
    }
    if (newPassword.length < 6) {
      wx.showToast({ title: '新密码至少6位', icon: 'none' });
      return;
    }
    if (newPassword !== confirmPassword) {
      wx.showToast({ title: '两次密码不一致', icon: 'none' });
      return;
    }

    app.request({
      url: '/user/change-password',
      method: 'POST',
      data: { oldPassword, newPassword }
    }).then(res => {
      wx.showToast({ title: '修改成功', icon: 'success' });
      this.closePasswordModal();
    }).catch(err => {
      wx.showToast({ title: err.message || '修改失败', icon: 'none' });
    });
  },

  unbindWx() {
    wx.showModal({
      title: '提示',
      content: '确定要解除微信绑定吗？解除后需要重新绑定才能使用微信一键登录',
      success: (res) => {
        if (res.confirm) {
          app.request({
            url: '/user/unbind-wx',
            method: 'POST'
          }).then(() => {
            wx.showToast({ title: '已解除绑定', icon: 'success' });
          }).catch(err => {
            wx.showToast({ title: err.message || '操作失败', icon: 'none' });
          });
        }
      }
    });
  },

  goToLeaveBalance() {
    wx.navigateTo({ url: '/pages/leave/balance/balance' });
  },

  goToAdmin() {
    wx.redirectTo({ url: '/pages/admin/index/index' });
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
