const app = getApp();

Page({
  data: {
    type: 'leave',
    currentTab: 'pending',
    approvalList: []
  },

  onLoad(options) {
    if (!app.checkLogin()) return;
    if (!app.globalData.isManager) {
      wx.showToast({ title: '无权限', icon: 'none' });
      wx.navigateBack();
      return;
    }
    this.setData({ type: options.type || 'leave' });

    let title = '请假审批';
    if (options.type === 'fieldwork') title = '外勤审批';
    else if (options.type === 'appeal') title = '申诉处理';
    else if (options.type === 'overtime') title = '加班初审';
    wx.setNavigationBarTitle({ title });
  },

  onShow() {
    this.loadApprovalList();
  },

  switchTab(e) {
    const tab = e.currentTarget.dataset.tab;
    this.setData({ currentTab: tab });
    this.loadApprovalList();
  },

  loadApprovalList() {
    const { type, currentTab } = this.data;
    let status = 0;
    if (currentTab === 'approved') status = 1;
    else if (currentTab === 'rejected') status = 2;

    app.request({
      url: `/manager/${type}/list`,
      method: 'GET',
      data: { status }
    }).then(res => {
      this.setData({ approvalList: res.data || [] });
    }).catch(err => {
      wx.showToast({ title: err.message || '获取审批列表失败', icon: 'none' });
    });
  },

  onOpinionInput(e) {
    const index = e.currentTarget.dataset.index;
    this.setData({
      [`approvalList[${index}].opinion`]: e.detail.value
    });
  },

  handleApprove(e) {
    const id = e.currentTarget.dataset.id;
    const index = e.currentTarget.dataset.index;
    const opinion = this.data.approvalList[index].opinion || '';
    this.doApproval(id, 1, opinion);
  },

  handleReject(e) {
    const id = e.currentTarget.dataset.id;
    const index = e.currentTarget.dataset.index;
    const opinion = this.data.approvalList[index].opinion || '';
    this.doApproval(id, 2, opinion);
  },

  doApproval(id, status, opinion) {
    const { type } = this.data;

    wx.showLoading({ title: '处理中...' });

    app.request({
      url: `/manager/${type}/approve/${id}`,
      method: 'POST',
      data: { status, opinion }
    }).then(() => {
      wx.showToast({ title: status === 1 ? '已通过' : '已拒绝', icon: 'success' });
      this.loadApprovalList();
    }).catch(err => {
      wx.showToast({ title: err.message || '操作失败', icon: 'none' });
    }).finally(() => {
      wx.hideLoading();
    });
  }
});
