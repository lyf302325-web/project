const app = getApp();

Page({
  data: {
    userInfo: {},
    todayDate: '',
    todayAttendance: {},
    monthStats: {}
  },

  onLoad() {
    this.setData({
      todayDate: this.formatDate(new Date())
    });
  },

  onShow() {
    if (!app.checkLogin()) return;
    
    this.setData({
      userInfo: app.globalData.userInfo || {}
    });
    
    this.loadTodayAttendance();
    this.loadMonthStats();
  },

  formatDate(date) {
    const year = date.getFullYear();
    const month = date.getMonth() + 1;
    const day = date.getDate();
    const weekDays = ['日', '一', '二', '三', '四', '五', '六'];
    const weekDay = weekDays[date.getDay()];
    return `${month}月${day}日 周${weekDay}`;
  },

  loadTodayAttendance() {
    app.request({
      url: '/attendance/today',
      method: 'GET'
    }).then(res => {
      this.setData({ todayAttendance: res.data || {} });
    }).catch(err => {
      console.error('获取今日考勤失败', err);
    });
  },

  loadMonthStats() {
    app.request({
      url: '/attendance/month-stats',
      method: 'GET'
    }).then(res => {
      this.setData({ monthStats: res.data || {} });
    }).catch(err => {
      console.error('获取月度统计失败', err);
    });
  },

  goToCheckin() {
    wx.switchTab({ url: '/pages/checkin/checkin' });
  },

  goToLeave() {
    wx.navigateTo({ url: '/pages/leave/leave' });
  },

  goToFieldwork() {
    wx.navigateTo({ url: '/pages/fieldwork/fieldwork' });
  },

  goToAppeal() {
    wx.navigateTo({ url: '/pages/appeal/appeal' });
  },

  goToOvertime() {
    wx.navigateTo({ url: '/pages/overtime/overtime' });
  }
});
