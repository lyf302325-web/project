const app = getApp();

Page({
  data: {
    username: '',
    password: '',
    showPassword: false,
    loading: false,
    wxLoading: false,
    showBindForm: false,
    bindUsername: '',
    bindPassword: '',
    bindLoading: false,
    wxOpenid: ''
  },

  onUsernameInput(e) {
    this.setData({ username: e.detail.value });
  },

  onPasswordInput(e) {
    this.setData({ password: e.detail.value });
  },

  togglePassword() {
    this.setData({ showPassword: !this.data.showPassword });
  },

  goToRegister() {
    wx.navigateTo({ url: '/pages/register/register' });
  },

  onBindUsernameInput(e) {
    this.setData({ bindUsername: e.detail.value });
  },

  onBindPasswordInput(e) {
    this.setData({ bindPassword: e.detail.value });
  },

  cancelBind() {
    this.setData({ showBindForm: false, wxOpenid: '' });
  },

  handleWxLogin() {
    this.setData({ wxLoading: true });
    wx.login({
      success: (res) => {
        if (!res.code) {
          wx.showToast({ title: '微信登录失败', icon: 'none' });
          this.setData({ wxLoading: false });
          return;
        }
        app.request({
          url: '/auth/wx-login',
          method: 'POST',
          data: { code: res.code }
        }).then(result => {
          const data = result.data;
          if (data.needBind) {
            this.setData({ showBindForm: true, wxOpenid: data.openid, wxLoading: false });
            wx.showToast({ title: '请绑定账号', icon: 'none' });
          } else {
            this.onLoginSuccess(data);
          }
        }).catch(err => {
          wx.showToast({ title: err.message || '微信登录失败', icon: 'none' });
        }).finally(() => {
          this.setData({ wxLoading: false });
        });
      },
      fail: () => {
        wx.showToast({ title: '微信登录失败', icon: 'none' });
        this.setData({ wxLoading: false });
      }
    });
  },

  handleWxBind() {
    const { bindUsername, bindPassword, wxOpenid } = this.data;
    if (!bindUsername.trim()) {
      wx.showToast({ title: '请输入账号', icon: 'none' });
      return;
    }
    if (!bindPassword.trim()) {
      wx.showToast({ title: '请输入密码', icon: 'none' });
      return;
    }
    this.setData({ bindLoading: true });
    app.request({
      url: '/auth/wx-bind',
      method: 'POST',
      data: { openid: wxOpenid, username: bindUsername, password: bindPassword }
    }).then(result => {
      this.onLoginSuccess(result.data);
    }).catch(err => {
      wx.showToast({ title: err.message || '绑定失败', icon: 'none' });
    }).finally(() => {
      this.setData({ bindLoading: false });
    });
  },

  onLoginSuccess(data) {
    wx.setStorageSync('token', data.token);
    wx.setStorageSync('userInfo', data.userInfo);
    app.globalData.token = data.token;
    app.globalData.userInfo = data.userInfo;
    const isAdmin = data.userInfo.roleType == 1;
    app.globalData.isAdmin = isAdmin;
    wx.showToast({ title: '登录成功', icon: 'success' });
    setTimeout(() => {
      if (isAdmin) {
        wx.reLaunch({ url: '/pages/admin/index/index' });
      } else {
        wx.reLaunch({ url: '/pages/index/index' });
      }
    }, 1500);
  },

  handleLogin() {
    const { username, password } = this.data;
    
    if (!username.trim()) {
      wx.showToast({ title: '请输入账号', icon: 'none' });
      return;
    }
    if (!password.trim()) {
      wx.showToast({ title: '请输入密码', icon: 'none' });
      return;
    }

    this.setData({ loading: true });

    app.request({
      url: '/auth/login',
      method: 'POST',
      data: { username, password }
    }).then(res => {
      console.log('登录成功响应:', res);
      console.log('userInfo:', res.data.userInfo);
      console.log('roleType:', res.data.userInfo.roleType);
      
      if (res.data && res.data.token) {
        wx.setStorageSync('token', res.data.token);
        wx.setStorageSync('userInfo', res.data.userInfo);
        app.globalData.token = res.data.token;
        app.globalData.userInfo = res.data.userInfo;
        
        // roleType可能是数字或字符串，都要处理
        const isAdmin = res.data.userInfo.roleType == 1;
        const isManager = res.data.userInfo.roleType == 2;
        app.globalData.isAdmin = isAdmin;
        app.globalData.isManager = isManager;
        console.log('是否管理员:', isAdmin);
        console.log('是否部门主管:', isManager);

        wx.showToast({ title: '登录成功', icon: 'success' });
        
        setTimeout(() => {
          if (isAdmin) {
            console.log('跳转到管理员页面');
            wx.reLaunch({ url: '/pages/admin/index/index' });
          } else if (isManager) {
            console.log('跳转到部门主管页面');
            wx.reLaunch({ url: '/pages/manager/index/index' });
          } else {
            console.log('跳转到员工首页');
            wx.reLaunch({ url: '/pages/index/index' });
          }
        }, 1500);
      } else {
        console.log('响应数据异常:', res);
        wx.showToast({ title: '登录响应异常', icon: 'none' });
      }
    }).catch(err => {
      console.log('登录失败:', err);
      wx.showToast({ title: err.message || '登录失败', icon: 'none' });
    }).finally(() => {
      this.setData({ loading: false });
    });
  }
});
