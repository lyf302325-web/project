App({
  globalData: {
    userInfo: null,
    token: null,
    baseUrl: 'http://192.168.137.1:8080/api',
    isAdmin: false,
    isManager: false,
    templateIdAbnormal: ''
  },

  onLaunch() {
    const savedBaseUrl = wx.getStorageSync('baseUrl');
    if (savedBaseUrl) {
      this.globalData.baseUrl = savedBaseUrl;
    } else {
      try {
        const sys = wx.getSystemInfoSync();
        if (sys && sys.platform === 'devtools') {
          this.globalData.baseUrl = 'http://localhost:8080/api';
        }
      } catch (e) {}
    }

    // 检查登录状态
    const token = wx.getStorageSync('token');
    const userInfo = wx.getStorageSync('userInfo');
    if (token && userInfo) {
      this.globalData.token = token;
      this.globalData.userInfo = userInfo;
      this.globalData.isAdmin = userInfo.roleType === 1;
      this.globalData.isManager = userInfo.roleType === 2;
    }
  },

  // 封装请求方法
  request(options) {
    const that = this;
    return new Promise((resolve, reject) => {
      wx.request({
        url: that.globalData.baseUrl + options.url,
        method: options.method || 'GET',
        data: options.data || {},
        header: {
          'Content-Type': 'application/json',
          'Authorization': that.globalData.token ? 'Bearer ' + that.globalData.token : ''
        },
        success(res) {
          if (res.statusCode === 200) {
            if (res.data.code === 200) {
              resolve(res.data);
            } else if (res.data.code === 401) {
              // token过期，跳转登录
              wx.removeStorageSync('token');
              wx.removeStorageSync('userInfo');
              wx.redirectTo({ url: '/pages/login/login' });
              reject(res.data);
            } else {
              reject(res.data);
            }
          } else {
            reject({ code: res.statusCode, message: '请求失败' });
          }
        },
        fail(err) {
          reject({ code: -1, message: err && err.errMsg ? err.errMsg : '网络错误' });
        }
      });
    });
  },

  // 检查登录状态
  checkLogin() {
    if (!this.globalData.token) {
      wx.redirectTo({ url: '/pages/login/login' });
      return false;
    }
    return true;
  }
});
