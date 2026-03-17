const app = getApp();

Page({
  data: {
    groups: [],
    selectedGroupId: '',
    selectedGroupName: '',
    departments: [],
    selectedDepartmentIds: [],
    ruleList: [],
    editingRuleId: null,
    showGroupModal: false,
    newGroupName: '',
    ruleData: {
      shiftName: '默认班次',
      checkInTime: '09:00',
      checkOutTime: '18:00',
      earlyCheckInMinutes: 120,
      lateThreshold: 30,
      earlyThreshold: 30,
      lateCheckOutMinutes: 240,
      address: '',
      longitude: '',
      latitude: '',
      checkInRange: 500,
      photoVerify: false,
      fieldworkRequired: false,
      enabled: true
    },
    loading: false
  },

  onLoad() {
    if (!app.checkLogin()) return;
    if (!app.globalData.isAdmin) {
      wx.showToast({ title: '无权限', icon: 'none' });
      wx.navigateBack();
      return;
    }
    this.loadGroups();
    this.loadDepartments();
  },

  loadGroups() {
    app.request({
      url: '/admin/attendance-group/list',
      method: 'GET'
    }).then(res => {
      const groups = res.data || [];
      if (groups.length === 0) {
        app.request({
          url: '/admin/attendance-group/add',
          method: 'POST',
          data: { name: '默认考勤组' }
        }).then(() => {
          this.loadGroups();
        }).catch(() => {
          this.setData({ groups: [], selectedGroupId: '', selectedGroupName: '' });
        });
        return;
      }
      const selectedGroupId = this.data.selectedGroupId || String(groups[0].id);
      const selected = groups.find(g => String(g.id) === String(selectedGroupId)) || groups[0];
      this.setData({
        groups,
        selectedGroupId: String(selected.id),
        selectedGroupName: selected.name
      });
      this.loadRuleList();
      this.loadRuleData();
      this.syncDepartmentSelection();
    }).catch(err => {
      console.error('获取规则失败', err);
    });
  },

  loadDepartments() {
    app.request({
      url: '/admin/department/list',
      method: 'GET'
    }).then(res => {
      this.setData({ departments: res.data || [] });
      this.syncDepartmentSelection();
    }).catch(() => {});
  },

  syncDepartmentSelection() {
    const { departments, selectedGroupId } = this.data;
    if (!departments.length || !selectedGroupId) return;
    const selectedDepartmentIds = departments
      .filter(d => String(d.attendanceGroupId || '') === String(selectedGroupId))
      .map(d => String(d.id));
    this.setData({ selectedDepartmentIds });
  },

  onGroupChange(e) {
    const index = Number(e.detail.value) || 0;
    const group = this.data.groups[index];
    if (!group) return;
    this.setData({
      selectedGroupId: String(group.id),
      selectedGroupName: group.name,
      editingRuleId: null
    });
    this.loadRuleList();
    this.loadRuleData();
    this.syncDepartmentSelection();
  },

  loadRuleList() {
    if (!this.data.selectedGroupId) return;
    app.request({
      url: '/admin/rule/list',
      method: 'GET',
      data: { attendanceGroupId: this.data.selectedGroupId }
    }).then(res => {
      const list = (res.data || []).map(r => ({
        ...r,
        enabledText: r.enabled == 1 ? '已启用' : '未启用'
      }));
      this.setData({ ruleList: list });
    }).catch(() => {});
  },

  loadRuleData() {
    if (!this.data.selectedGroupId) return;
    app.request({
      url: '/admin/rule/get',
      method: 'GET',
      data: { attendanceGroupId: this.data.selectedGroupId }
    }).then(res => {
      if (res.data) {
        const data = res.data;
        data.photoVerify = data.photoVerify == 1;
        data.fieldworkRequired = data.fieldworkRequired == 1;
        data.enabled = data.enabled == null ? true : data.enabled == 1;
        this.setData({ ruleData: data, editingRuleId: data.id || null });
      } else {
        this.resetRuleForm();
      }
    }).catch(() => {
      this.resetRuleForm();
    });
  },

  resetRuleForm() {
    this.setData({
      editingRuleId: null,
      ruleData: {
        shiftName: '默认班次',
        checkInTime: '09:00',
        checkOutTime: '18:00',
        earlyCheckInMinutes: 120,
        lateThreshold: 30,
        earlyThreshold: 30,
        lateCheckOutMinutes: 240,
        address: '',
        longitude: '',
        latitude: '',
        checkInRange: 500,
        photoVerify: false,
        fieldworkRequired: false,
        enabled: true
      }
    });
  },

  startAddShift() {
    this.resetRuleForm();
  },

  editShift(e) {
    const index = e.currentTarget.dataset.index;
    const item = this.data.ruleList[index];
    if (!item) return;
    const data = { ...item };
    data.photoVerify = data.photoVerify == 1;
    data.fieldworkRequired = data.fieldworkRequired == 1;
    data.enabled = data.enabled == null ? true : data.enabled == 1;
    this.setData({ ruleData: data, editingRuleId: data.id || null });
  },

  enableShift(e) {
    const id = e.currentTarget.dataset.id;
    if (!id) return;
    wx.showLoading({ title: '处理中...' });
    app.request({
      url: `/admin/rule/enable/${id}`,
      method: 'POST'
    }).then(() => {
      wx.showToast({ title: '已启用', icon: 'success' });
      this.loadRuleList();
      this.loadRuleData();
    }).catch(err => {
      wx.showToast({ title: err.message || '操作失败', icon: 'none' });
    }).finally(() => wx.hideLoading());
  },

  onCheckInTimeChange(e) {
    this.setData({ 'ruleData.checkInTime': e.detail.value });
  },

  onCheckOutTimeChange(e) {
    this.setData({ 'ruleData.checkOutTime': e.detail.value });
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [`ruleData.${field}`]: e.detail.value });
  },

  onPhotoVerifyChange(e) {
    this.setData({ 'ruleData.photoVerify': e.detail.value });
  },

  onFieldworkRequiredChange(e) {
    this.setData({ 'ruleData.fieldworkRequired': e.detail.value });
  },

  onEnabledChange(e) {
    this.setData({ 'ruleData.enabled': e.detail.value });
  },

  openAddGroup() {
    this.setData({ showGroupModal: true, newGroupName: '' });
  },

  closeAddGroup() {
    this.setData({ showGroupModal: false, newGroupName: '' });
  },

  onNewGroupNameInput(e) {
    this.setData({ newGroupName: e.detail.value });
  },

  confirmAddGroup() {
    const name = (this.data.newGroupName || '').trim();
    if (!name) {
      wx.showToast({ title: '请输入考勤组名称', icon: 'none' });
      return;
    }
    wx.showLoading({ title: '创建中...' });
    app.request({
      url: '/admin/attendance-group/add',
      method: 'POST',
      data: { name }
    }).then(() => {
      wx.showToast({ title: '已创建', icon: 'success' });
      this.closeAddGroup();
      this.loadGroups();
    }).catch(err => {
      wx.showToast({ title: err.message || '创建失败', icon: 'none' });
    }).finally(() => wx.hideLoading());
  },

  onDepartmentCheckboxChange(e) {
    this.setData({ selectedDepartmentIds: e.detail.value || [] });
  },

  saveGroupAssignment() {
    const groupId = this.data.selectedGroupId;
    const departmentIds = (this.data.selectedDepartmentIds || []).map(x => Number(x));
    if (!groupId) {
      wx.showToast({ title: '请先选择考勤组', icon: 'none' });
      return;
    }
    if (!departmentIds.length) {
      wx.showToast({ title: '请选择要绑定的部门', icon: 'none' });
      return;
    }
    wx.showLoading({ title: '保存中...' });
    app.request({
      url: '/admin/attendance-group/assign',
      method: 'POST',
      data: { groupId: Number(groupId), departmentIds }
    }).then(() => {
      wx.showToast({ title: '已保存', icon: 'success' });
      this.loadDepartments();
    }).catch(err => {
      wx.showToast({ title: err.message || '保存失败', icon: 'none' });
    }).finally(() => wx.hideLoading());
  },

  getCurrentLocation() {
    wx.showLoading({ title: '获取位置中...' });
    wx.getLocation({
      type: 'gcj02',
      success: (res) => {
        this.setData({
          'ruleData.longitude': res.longitude.toFixed(6),
          'ruleData.latitude': res.latitude.toFixed(6)
        });
        wx.showToast({ title: '获取成功', icon: 'success' });
      },
      fail: (err) => {
        wx.showToast({ title: '获取位置失败', icon: 'none' });
      },
      complete: () => {
        wx.hideLoading();
      }
    });
  },

  handleSubmit() {
    const { ruleData } = this.data;

    if (!this.data.selectedGroupId) {
      wx.showToast({ title: '请选择考勤组', icon: 'none' });
      return;
    }
    if (!ruleData.shiftName || !String(ruleData.shiftName).trim()) {
      wx.showToast({ title: '请输入班次名称', icon: 'none' });
      return;
    }
    if (!ruleData.checkInTime) {
      wx.showToast({ title: '请设置上班时间', icon: 'none' });
      return;
    }
    if (!ruleData.checkOutTime) {
      wx.showToast({ title: '请设置下班时间', icon: 'none' });
      return;
    }

    this.setData({ loading: true });

    const submitData = Object.assign({}, ruleData);
    submitData.photoVerify = submitData.photoVerify ? 1 : 0;
    submitData.fieldworkRequired = submitData.fieldworkRequired ? 1 : 0;
    submitData.enabled = submitData.enabled ? 1 : 0;
    submitData.departmentId = this.data.selectedGroupId ? Number(this.data.selectedGroupId) : null;
    if (this.data.editingRuleId) submitData.id = this.data.editingRuleId;

    app.request({
      url: '/admin/rule/save',
      method: 'POST',
      data: submitData
    }).then(res => {
      wx.showToast({ title: '保存成功', icon: 'success' });
      this.loadRuleList();
      this.loadRuleData();
    }).catch(err => {
      wx.showToast({ title: err.message || '保存失败', icon: 'none' });
    }).finally(() => {
      this.setData({ loading: false });
    });
  }
});
