const app = getApp();

Page({
  data: {
    year: '',
    rules: [
      { leaveType: 1, leaveTypeName: '事假', totalDays: 0 },
      { leaveType: 2, leaveTypeName: '病假', totalDays: 0 },
      { leaveType: 3, leaveTypeName: '年假', totalDays: 0 },
      { leaveType: 4, leaveTypeName: '婚假', totalDays: 0 },
      { leaveType: 5, leaveTypeName: '产假', totalDays: 0 },
      { leaveType: 6, leaveTypeName: '丧假', totalDays: 0 }
    ],
    loading: false
  },

  onShow() {
    if (!app.checkLogin()) return;
    if (!app.globalData.isAdmin) {
      wx.showToast({ title: '无权限', icon: 'none' });
      wx.navigateBack();
      return;
    }
    const y = new Date().getFullYear();
    this.setData({ year: String(y) });
    this.loadRules();
  },

  onYearInput(e) {
    this.setData({ year: e.detail.value });
  },

  onTotalDaysInput(e) {
    const index = e.currentTarget.dataset.index;
    this.setData({
      [`rules[${index}].totalDays`]: e.detail.value
    });
  },

  loadRules() {
    app.request({
      url: '/admin/leave-quota/list',
      method: 'GET',
      data: { year: this.data.year }
    }).then(res => {
      const list = res.data || [];
      const map = {};
      list.forEach(r => {
        map[r.leaveType] = r;
      });
      const rules = this.data.rules.map(r => {
        const found = map[r.leaveType];
        return {
          ...r,
          totalDays: found ? found.totalDays : r.totalDays
        };
      });
      this.setData({ rules });
    }).catch(err => {
      wx.showToast({ title: err.message || '加载失败', icon: 'none' });
    });
  },

  saveRules() {
    this.setData({ loading: true });
    const payloadRules = this.data.rules.map(r => ({
      leaveType: r.leaveType,
      totalDays: r.totalDays || 0,
      position: ''
    }));
    app.request({
      url: '/admin/leave-quota/save',
      method: 'POST',
      data: { year: this.data.year, rules: payloadRules }
    }).then(() => {
      wx.showToast({ title: '保存并同步成功', icon: 'success' });
    }).catch(err => {
      wx.showToast({ title: err.message || '保存失败', icon: 'none' });
    }).finally(() => {
      this.setData({ loading: false });
    });
  },

  initBalances() {
    wx.showModal({
      title: '提示',
      content: '将按当前规则为所有员工初始化/更新假期余额，是否继续？',
      success: (res) => {
        if (!res.confirm) return;
        wx.showLoading({ title: '处理中...' });
        app.request({
          url: '/admin/leave-quota/init',
          method: 'POST',
          data: { year: this.data.year }
        }).then(() => {
          wx.showToast({ title: '已初始化', icon: 'success' });
        }).catch(err => {
          wx.showToast({ title: err.message || '操作失败', icon: 'none' });
        }).finally(() => {
          wx.hideLoading();
        });
      }
    });
  }
});
