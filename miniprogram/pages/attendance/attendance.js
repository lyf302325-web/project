const app = getApp();

Page({
  data: {
    currentYear: new Date().getFullYear(),
    currentMonth: new Date().getMonth() + 1,
    monthStats: {},
    attendanceList: []
  },

  onShow() {
    if (!app.checkLogin()) return;
    this.loadMonthData();
  },

  prevMonth() {
    let { currentYear, currentMonth } = this.data;
    if (currentMonth === 1) {
      currentYear--;
      currentMonth = 12;
    } else {
      currentMonth--;
    }
    this.setData({ currentYear, currentMonth });
    this.loadMonthData();
  },

  nextMonth() {
    let { currentYear, currentMonth } = this.data;
    const now = new Date();
    if (currentYear === now.getFullYear() && currentMonth === now.getMonth() + 1) {
      return;
    }
    if (currentMonth === 12) {
      currentYear++;
      currentMonth = 1;
    } else {
      currentMonth++;
    }
    this.setData({ currentYear, currentMonth });
    this.loadMonthData();
  },

  loadMonthData() {
    const { currentYear, currentMonth } = this.data;
    
    app.request({
      url: '/attendance/month',
      method: 'GET',
      data: { year: currentYear, month: currentMonth }
    }).then(res => {
      const weekDays = ['日', '一', '二', '三', '四', '五', '六'];
      const list = (res.data.list || []).map(item => {
        const date = new Date(item.attendanceDate);
        return {
          ...item,
          day: date.getDate(),
          weekDay: '周' + weekDays[date.getDay()]
        };
      });
      this.setData({
        monthStats: res.data.stats || {},
        attendanceList: list
      });
    }).catch(err => {
      console.error('获取月度考勤失败', err);
    });
  },

  exportReport() {
    const { currentYear, currentMonth } = this.data;
    wx.showLoading({ title: '导出中...' });

    const url = app.globalData.baseUrl + '/attendance/export?year=' + currentYear + '&month=' + currentMonth;

    wx.downloadFile({
      url: url,
      header: {
        'Authorization': app.globalData.token ? 'Bearer ' + app.globalData.token : ''
      },
      success: (res) => {
        if (res.statusCode === 200) {
          wx.openDocument({
            filePath: res.tempFilePath,
            fileType: 'xlsx',
            showMenu: true,
            success: () => {
              wx.showToast({ title: '导出成功', icon: 'success' });
            },
            fail: () => {
              wx.showToast({ title: '打开文件失败', icon: 'none' });
            }
          });
        } else {
          wx.showToast({ title: '导出失败', icon: 'none' });
        }
      },
      fail: () => {
        wx.showToast({ title: '下载失败', icon: 'none' });
      },
      complete: () => {
        wx.hideLoading();
      }
    });
  }
});
