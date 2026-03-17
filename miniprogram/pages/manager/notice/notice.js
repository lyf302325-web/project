const app = getApp();

Page({
  data: {
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

  loadList() {
    wx.showLoading({ title: '加载中...' });
    app.request({
      url: '/manager/notice/list',
      method: 'GET'
    }).then(res => {
      const typeMap = {
        pending_approval: { label: '待审批', cls: 'status-warning' },
        attendance_alert: { label: '考勤异常', cls: 'status-error' },
        approval_result: { label: '审批结果', cls: 'status-success' }
      };
      const list = (res.data || []).map(m => {
        const t = typeMap[m.type] || { label: '系统', cls: 'status-normal' };
        return {
          ...m,
          typeLabel: t.label,
          typeClass: t.cls,
          timeText: this.formatTime(m.time)
        };
      });
      this.setData({ list });
    }).catch(err => {
      wx.showToast({ title: err.message || '加载失败', icon: 'none' });
    }).finally(() => {
      wx.hideLoading();
    });
  },

  formatTime(time) {
    if (!time) return '';
    if (typeof time === 'string') {
      return time.replace('T', ' ').replace(/\.\d+$/, '');
    }
    if (Array.isArray(time)) {
      const [y, mo, d, h, mi, s] = time;
      const pad = (v) => String(v || 0).padStart(2, '0');
      return `${y}-${pad(mo)}-${pad(d)} ${pad(h)}:${pad(mi)}:${pad(s)}`;
    }
    return '';
  },

  openDetail(e) {
    const item = e.currentTarget.dataset.item;
    this.setData({ showModal: true, currentItem: item });
  },

  closeModal() {
    this.setData({ showModal: false, currentItem: null });
  }
});
