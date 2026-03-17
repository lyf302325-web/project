const app = getApp();

Page({
  data: {
    departments: [{ id: null, name: '全部' }],
    selectedDepartment: { id: null, name: '全部' },
    selectedMonth: '',
    stats: {},
    employeeStats: [],
    showExportModal: false,
    csvContent: ''
  },

  onLoad() {
    if (!app.checkLogin()) return;
    const now = new Date();
    const month = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
    this.setData({ selectedMonth: month });
    this.loadDepartments();
    this.handleSearch();
  },

  loadDepartments() {
    app.request({
      url: '/admin/department/list',
      method: 'GET'
    }).then(res => {
      const departments = [{ id: null, name: '全部' }, ...(res.data || [])];
      this.setData({ departments });
    }).catch(err => {
      console.error('获取部门列表失败', err);
    });
  },

  onDepartmentChange(e) {
    const index = e.detail.value;
    this.setData({
      selectedDepartment: this.data.departments[index]
    });
  },

  onMonthChange(e) {
    this.setData({ selectedMonth: e.detail.value });
  },

  handleSearch() {
    const { selectedDepartment, selectedMonth } = this.data;
    
    wx.showLoading({ title: '加载中...' });

    const params = { month: selectedMonth };
    if (selectedDepartment.id) {
      params.departmentId = selectedDepartment.id;
    }

    app.request({
      url: '/admin/statistics',
      method: 'GET',
      data: params
    }).then(res => {
      this.setData({
        stats: res.data.summary || {},
        employeeStats: res.data.employees || []
      });
    }).catch(err => {
      console.error('获取统计数据失败', err);
    }).finally(() => {
      wx.hideLoading();
    });
  },

  goToDetail(e) {
    const item = e.currentTarget.dataset.item;
    const month = this.data.selectedMonth;
    wx.navigateTo({
      url: `/pages/admin/statistics/detail/detail?userId=${item.id}&month=${month}&name=${item.name}&employeeNo=${item.employeeNo}`
    });
  },

  exportReport() {
    const { selectedDepartment, selectedMonth } = this.data;
    
    wx.showLoading({ title: '导出中...' });

    let url = app.globalData.baseUrl + '/admin/statistics/export?month=' + selectedMonth;
    if (selectedDepartment.id) {
      url += '&departmentId=' + selectedDepartment.id;
    }

    wx.downloadFile({
      url: url,
      header: {
        'Authorization': app.globalData.token ? 'Bearer ' + app.globalData.token : ''
      },
      success: (res) => {
        if (res.statusCode === 200) {
          const filePath = res.tempFilePath;
          wx.openDocument({
            filePath: filePath,
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
