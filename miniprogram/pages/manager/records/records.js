const app = getApp();

Page({
  data: {
    type: 'all',
    list: [],
    showModal: false,
    currentItem: null
  },

  onShow() {
    if (!app.checkLogin()) return;
    if (!app.globalData.isManager) {
      wx.showToast({ title: '无权限', icon: 'none' });
      wx.navigateBack();
      return;
    }
    this.loadList();
  },

  switchType(e) {
    const type = e.currentTarget.dataset.type;
    this.setData({ type });
    this.loadList();
  },

  loadList() {
    const params = {};
    if (this.data.type !== 'all') params.type = this.data.type;

    wx.showLoading({ title: '加载中...' });
    app.request({
      url: '/manager/approval/records',
      method: 'GET',
      data: params
    }).then(res => {
      const list = (res.data || []).map(item => {
        const recordTypeName = this.getTypeName(item.recordType);
        const statusName = this.getStatusName(item.status);
        return { ...item, recordTypeName, statusName };
      });
      this.setData({ list });
    }).catch(err => {
      wx.showToast({ title: err.message || '加载失败', icon: 'none' });
    }).finally(() => {
      wx.hideLoading();
    });
  },

  getTypeName(type) {
    if (type === 'leave') return '请假';
    if (type === 'fieldwork') return '外勤';
    if (type === 'appeal') return '申诉';
    if (type === 'overtime') return '加班';
    return '';
  },

  getStatusName(status) {
    if (status === 1) return '已通过';
    if (status === 2) return '已拒绝';
    return '待审批';
  },

  openDetail(e) {
    const index = e.currentTarget.dataset.index;
    const item = this.data.list[index];
    if (!item) return;
    const currentItem = {
      ...item,
      recordTypeName: item.recordTypeName || this.getTypeName(item.recordType),
      statusName: item.statusName || this.getStatusName(item.status)
    };
    this.setData({ showModal: true, currentItem });
  },

  closeModal() {
    this.setData({ showModal: false, currentItem: null });
  }
});
