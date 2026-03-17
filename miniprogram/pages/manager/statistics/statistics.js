const app = getApp();

Page({
  data: {
    startDate: '',
    endDate: '',
    stats: {},
    employeeStats: [],
    filteredEmployeeStats: [],
    statusIndex: 0,
    statusOptions: [
      { label: '全部', key: 'all' },
      { label: '异常', key: 'abnormal' },
      { label: '迟到', key: 'late' },
      { label: '早退', key: 'early' },
      { label: '旷工', key: 'absent' },
      { label: '外勤', key: 'fieldwork' },
      { label: '正常', key: 'normal' }
    ]
  },

  onLoad() {
    if (!app.checkLogin()) return;
    if (!app.globalData.isManager) {
      wx.showToast({ title: '无权限', icon: 'none' });
      wx.navigateBack();
      return;
    }
    const now = new Date();
    const start = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`;
    const end = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
    this.setData({ startDate: start, endDate: end });
    this.handleSearch();
  },

  onStartDateChange(e) {
    this.setData({ startDate: e.detail.value });
  },

  onEndDateChange(e) {
    this.setData({ endDate: e.detail.value });
  },

  onStatusChange(e) {
    this.setData({ statusIndex: Number(e.detail.value) || 0 });
    this.applyEmployeeFilter();
  },

  applyEmployeeFilter() {
    const { employeeStats, statusOptions, statusIndex } = this.data;
    const option = statusOptions[statusIndex] || statusOptions[0];
    const key = option.key;
    const list = employeeStats || [];
    const filtered = key === 'all' ? list : list.filter(item => Number(item[key] || 0) > 0);
    this.setData({ filteredEmployeeStats: filtered });
  },

  handleSearch() {
    const { startDate, endDate } = this.data;
    if (!startDate || !endDate) {
      wx.showToast({ title: '请选择时间范围', icon: 'none' });
      return;
    }

    wx.showLoading({ title: '加载中...' });

    app.request({
      url: '/manager/statistics',
      method: 'GET',
      data: { startDate, endDate }
    }).then(res => {
      this.setData({
        stats: (res.data && res.data.summary) ? res.data.summary : {},
        employeeStats: (res.data && res.data.employees) ? res.data.employees : []
      });
      this.applyEmployeeFilter();
    }).catch(err => {
      wx.showToast({ title: err.message || '获取统计失败', icon: 'none' });
    }).finally(() => {
      wx.hideLoading();
    });
  },

  exportReport() {
    const { startDate, endDate } = this.data;
    wx.showLoading({ title: '导出中...' });

    const url = app.globalData.baseUrl + `/manager/statistics/export?startDate=${startDate}&endDate=${endDate}`;

    wx.downloadFile({
      url,
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
  },

  goToEmployeeDetail(e) {
    const item = e.currentTarget.dataset.item;
    const month = this.data.startDate ? this.data.startDate.slice(0, 7) : '';
    wx.navigateTo({
      url: `/pages/manager/statistics/detail/detail?userId=${item.id}&month=${month}&name=${item.name}&employeeNo=${item.employeeNo}`
    });
  }
});
