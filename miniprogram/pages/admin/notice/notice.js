const app = getApp();

Page({
  data: {
    config: {
      attendanceAlert: true,
      approvalResult: true,
      pendingApproval: true,
      checkInReminder: false,
      morningReminderTime: '08:50',
      eveningReminderTime: '18:00',
      missCheckInAfterMinutes: 30,
      missCheckOutAfterMinutes: 30,
      lateRemind: true,
      earlyRemind: true,
      missCheckInRemind: true,
      missCheckOutRemind: true
    },
    loading: false
  },

  onLoad() {
    if (!app.checkLogin()) return;
    this.loadConfig();
  },

  loadConfig() {
    app.request({
      url: '/admin/notice/config',
      method: 'GET'
    }).then(res => {
      if (res.data) {
        this.setData({ config: res.data });
      }
    }).catch(err => {
      console.error('获取配置失败', err);
    });
  },

  onAttendanceAlertChange(e) {
    this.setData({ 'config.attendanceAlert': e.detail.value });
  },

  onApprovalResultChange(e) {
    this.setData({ 'config.approvalResult': e.detail.value });
  },

  onPendingApprovalChange(e) {
    this.setData({ 'config.pendingApproval': e.detail.value });
  },

  onCheckInReminderChange(e) {
    this.setData({ 'config.checkInReminder': e.detail.value });
  },

  onMorningTimeChange(e) {
    this.setData({ 'config.morningReminderTime': e.detail.value });
  },

  onEveningTimeChange(e) {
    this.setData({ 'config.eveningReminderTime': e.detail.value });
  },

  onMissCheckInAfterMinutesInput(e) {
    this.setData({ 'config.missCheckInAfterMinutes': e.detail.value });
  },

  onMissCheckOutAfterMinutesInput(e) {
    this.setData({ 'config.missCheckOutAfterMinutes': e.detail.value });
  },

  onLateRemindChange(e) {
    this.setData({ 'config.lateRemind': e.detail.value });
  },

  onEarlyRemindChange(e) {
    this.setData({ 'config.earlyRemind': e.detail.value });
  },

  onMissCheckInRemindChange(e) {
    this.setData({ 'config.missCheckInRemind': e.detail.value });
  },

  onMissCheckOutRemindChange(e) {
    this.setData({ 'config.missCheckOutRemind': e.detail.value });
  },

  handleSubmit() {
    this.setData({ loading: true });

    app.request({
      url: '/admin/notice/config',
      method: 'POST',
      data: this.data.config
    }).then(res => {
      wx.showToast({ title: '保存成功', icon: 'success' });
    }).catch(err => {
      wx.showToast({ title: err.message || '保存失败', icon: 'none' });
    }).finally(() => {
      this.setData({ loading: false });
    });
  }
});
