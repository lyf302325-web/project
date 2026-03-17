const app = getApp();

Page({
  data: {
    currentTab: 'pending',
    list: [],
    startDate: '',
    endDate: ''
  },

  onLoad() {
    if (!app.checkLogin()) return;
    if (!app.globalData.isAdmin) {
      wx.showToast({ title: '无权限', icon: 'none' });
      wx.navigateBack();
      return;
    }
    const now = new Date();
    const start = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`;
    const end = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
    this.setData({ startDate: start, endDate: end });
  },

  onShow() {
    if (!app.checkLogin()) return;
    this.loadList();
  },

  switchTab(e) {
    const tab = e.currentTarget.dataset.tab;
    this.setData({ currentTab: tab });
    this.loadList();
  },

  loadList() {
    let status = 1;
    if (this.data.currentTab === 'approved') status = 3;
    else if (this.data.currentTab === 'rejected') status = 2;

    wx.showLoading({ title: '加载中...' });
    app.request({
      url: '/admin/overtime/list',
      method: 'GET',
      data: { status }
    }).then(res => {
      this.setData({ list: res.data || [] });
    }).catch(err => {
      wx.showToast({ title: err.message || '加载失败', icon: 'none' });
    }).finally(() => wx.hideLoading());
  },

  onOpinionInput(e) {
    const index = e.currentTarget.dataset.index;
    this.setData({
      [`list[${index}].opinion`]: e.detail.value
    });
  },

  approve(e) {
    const id = e.currentTarget.dataset.id;
    const index = e.currentTarget.dataset.index;
    const opinion = this.data.list[index].opinion || '';
    this.doApprove(id, 3, opinion);
  },

  reject(e) {
    const id = e.currentTarget.dataset.id;
    const index = e.currentTarget.dataset.index;
    const opinion = this.data.list[index].opinion || '';
    this.doApprove(id, 2, opinion);
  },

  doApprove(id, status, opinion) {
    wx.showLoading({ title: '处理中...' });
    app.request({
      url: `/admin/overtime/approve/${id}`,
      method: 'POST',
      data: { status, opinion }
    }).then(() => {
      wx.showToast({ title: status === 3 ? '已通过' : '已拒绝', icon: 'success' });
      this.loadList();
    }).catch(err => {
      wx.showToast({ title: err.message || '操作失败', icon: 'none' });
    }).finally(() => wx.hideLoading());
  },

  onStartDateChange(e) {
    this.setData({ startDate: e.detail.value });
  },

  onEndDateChange(e) {
    this.setData({ endDate: e.detail.value });
  },

  queryStats() {
    const { startDate, endDate } = this.data;
    if (!startDate || !endDate) {
      wx.showToast({ title: '请选择时间范围', icon: 'none' });
      return;
    }
    wx.showLoading({ title: '统计中...' });
    app.request({
      url: '/admin/overtime/statistics',
      method: 'GET',
      data: { startDate, endDate }
    }).then(res => {
      const total = res.data && res.data.summary ? res.data.summary.totalHours : 0;
      wx.showModal({ title: '加班统计', content: `总加班工时：${total} 小时`, showCancel: false });
    }).catch(err => {
      wx.showToast({ title: err.message || '统计失败', icon: 'none' });
    }).finally(() => wx.hideLoading());
  },

  exportStats() {
    const { startDate, endDate } = this.data;
    if (!startDate || !endDate) {
      wx.showToast({ title: '请选择时间范围', icon: 'none' });
      return;
    }
    wx.showLoading({ title: '导出中...' });
    const url = app.globalData.baseUrl + `/admin/overtime/statistics/export?startDate=${startDate}&endDate=${endDate}`;
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
            showMenu: true
          });
        } else {
          wx.showToast({ title: '导出失败', icon: 'none' });
        }
      },
      fail: () => wx.showToast({ title: '下载失败', icon: 'none' }),
      complete: () => wx.hideLoading()
    });
  }
});

