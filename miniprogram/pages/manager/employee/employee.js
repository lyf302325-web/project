const app = getApp();

Page({
  data: {
    keyword: '',
    fullList: [],
    employeeList: []
  },

  onShow() {
    if (!app.checkLogin()) return;
    if (!app.globalData.isManager) {
      wx.showToast({ title: '无权限', icon: 'none' });
      wx.navigateBack();
      return;
    }
    this.loadEmployeeList();
  },

  onSearchInput(e) {
    this.setData({ keyword: e.detail.value });
  },

  handleSearch() {
    const keyword = (this.data.keyword || '').trim();
    if (!keyword) {
      this.setData({ employeeList: this.data.fullList });
      return;
    }
    const list = (this.data.fullList || []).filter(u => {
      const name = u.name || '';
      const no = u.employeeNo || '';
      return name.includes(keyword) || no.includes(keyword);
    });
    this.setData({ employeeList: list });
  },

  loadEmployeeList() {
    wx.showLoading({ title: '加载中...' });
    app.request({
      url: '/manager/employee/list',
      method: 'GET'
    }).then(res => {
      const list = res.data || [];
      this.setData({ fullList: list, employeeList: list });
      this.handleSearch();
    }).catch(err => {
      wx.showToast({ title: err.message || '加载失败', icon: 'none' });
    }).finally(() => {
      wx.hideLoading();
    });
  },

  goToDetail(e) {
    const item = e.currentTarget.dataset.item;
    const now = new Date();
    const month = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
    wx.navigateTo({
      url: `/pages/manager/statistics/detail/detail?userId=${item.id}&month=${month}&name=${item.name}&employeeNo=${item.employeeNo}`
    });
  }
});
