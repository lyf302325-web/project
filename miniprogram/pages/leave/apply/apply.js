const app = getApp();

Page({
  data: {
    leaveTypes: [
      { id: 1, name: '事假' },
      { id: 2, name: '病假' },
      { id: 3, name: '年假' },
      { id: 4, name: '婚假' },
      { id: 5, name: '产假' },
      { id: 6, name: '丧假' }
    ],
    selectedType: {},
    startDate: '',
    endDate: '',
    days: 0,
    reason: '',
    images: [],
    approvalFlow: [],
    leaveBalances: {},
    loading: false
  },

  onLoad() {
    if (!app.checkLogin()) return;
    this.loadLeaveBalances();
  },

  loadLeaveBalances() {
    app.request({
      url: '/user/leave-balance',
      method: 'GET'
    }).then(res => {
      const map = {};
      (res.data || []).forEach(item => {
        map[item.leaveType] = item;
      });
      this.setData({ leaveBalances: map });
    }).catch(() => {});
  },

  onTypeChange(e) {
    const index = e.detail.value;
    this.setData({
      selectedType: this.data.leaveTypes[index]
    });
    if (this.data.days > 0) {
      this.loadApprovalFlow(this.data.days);
    }
  },

  onStartDateChange(e) {
    this.setData({ startDate: e.detail.value });
    this.calculateDays();
  },

  onEndDateChange(e) {
    this.setData({ endDate: e.detail.value });
    this.calculateDays();
  },

  calculateDays() {
    const { startDate, endDate } = this.data;
    if (startDate && endDate) {
      const start = new Date(startDate);
      const end = new Date(endDate);
      const days = Math.ceil((end - start) / (1000 * 60 * 60 * 24)) + 1;
      this.setData({ days: days > 0 ? days : 0 });
      this.loadApprovalFlow(days);
    }
  },

  loadApprovalFlow(days) {
    const leaveType = this.data.selectedType && this.data.selectedType.id ? this.data.selectedType.id : '';
    app.request({
      url: '/approval/flow',
      method: 'GET',
      data: { days, leaveType }
    }).then(res => {
      const list = (res.data || []).map(item => ({
        ...item,
        roleName: item.role === 'manager' ? '部门主管' : item.role === 'admin' ? '管理员' : ''
      }));
      this.setData({ approvalFlow: list });
    }).catch(err => {
      console.error('获取审批流程失败', err);
    });
  },

  onReasonInput(e) {
    this.setData({ reason: e.detail.value });
  },

  chooseImage() {
    wx.chooseImage({
      count: 3 - this.data.images.length,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        this.setData({
          images: [...this.data.images, ...res.tempFilePaths]
        });
      }
    });
  },

  deleteImage(e) {
    const index = e.currentTarget.dataset.index;
    const images = this.data.images;
    images.splice(index, 1);
    this.setData({ images });
  },

  submitApply() {
    const { selectedType, startDate, endDate, days, reason, images, leaveBalances } = this.data;

    if (!selectedType.id) {
      wx.showToast({ title: '请选择请假类型', icon: 'none' });
      return;
    }
    if (!startDate) {
      wx.showToast({ title: '请选择开始日期', icon: 'none' });
      return;
    }
    if (!endDate) {
      wx.showToast({ title: '请选择结束日期', icon: 'none' });
      return;
    }
    if (!reason.trim()) {
      wx.showToast({ title: '请输入请假事由', icon: 'none' });
      return;
    }

    const bal = leaveBalances && selectedType.id ? leaveBalances[selectedType.id] : null;
    if (bal && bal.remainingDays != null && Number(days) > Number(bal.remainingDays)) {
      wx.showToast({ title: '请假天数超过剩余余额', icon: 'none' });
      return;
    }

    this.setData({ loading: true });

    app.request({
      url: '/leave/apply',
      method: 'POST',
      data: {
        leaveType: selectedType.id,
        startDate,
        endDate,
        days,
        reason,
        images
      }
    }).then(res => {
      wx.showToast({ title: '申请提交成功', icon: 'success' });
      setTimeout(() => {
        wx.navigateBack();
      }, 1500);
    }).catch(err => {
      wx.showToast({ title: err.message || '提交失败', icon: 'none' });
    }).finally(() => {
      this.setData({ loading: false });
    });
  }
});
