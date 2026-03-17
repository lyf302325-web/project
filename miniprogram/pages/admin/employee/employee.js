const app = getApp();

Page({
  data: {
    keyword: '',
    employeeList: []
  },

  onShow() {
    if (!app.checkLogin()) return;
    this.loadEmployeeList();
  },

  onSearchInput(e) {
    this.setData({ keyword: e.detail.value });
  },

  handleSearch() {
    this.loadEmployeeList();
  },

  loadEmployeeList() {
    app.request({
      url: '/admin/employee/list',
      method: 'GET',
      data: { keyword: this.data.keyword }
    }).then(res => {
      this.setData({ employeeList: res.data || [] });
    }).catch(err => {
      console.error('获取员工列表失败', err);
    });
  },

  goToAdd() {
    wx.navigateTo({ url: '/pages/admin/employee/edit/edit' });
  },

  goToEdit(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/admin/employee/edit/edit?id=${id}` });
  },

  handleDelete(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '提示',
      content: '确定要删除该员工吗？',
      success: (res) => {
        if (res.confirm) {
          app.request({
            url: `/admin/employee/delete/${id}`,
            method: 'DELETE'
          }).then(res => {
            wx.showToast({ title: '删除成功', icon: 'success' });
            this.loadEmployeeList();
          }).catch(err => {
            wx.showToast({ title: err.message || '删除失败', icon: 'none' });
          });
        }
      }
    });
  }
});
