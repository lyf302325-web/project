const app = getApp();

Page({
  data: {
    ruleList: [],
    approvers: [],
    departments: [{ id: '', name: '全部部门' }],
    leaveTypes: [
      { id: '', name: '全部类型' },
      { id: 1, name: '事假' },
      { id: 2, name: '病假' },
      { id: 3, name: '年假' },
      { id: 4, name: '婚假' },
      { id: 5, name: '产假' },
      { id: 6, name: '丧假' }
    ],
    levels: [
      { id: 1, name: '主管(一级)' },
      { id: 2, name: '管理员(二级)' }
    ],
    loading: false
  },

  onLoad() {
    if (!app.checkLogin()) return;
    this.loadDepartments().finally(() => {
      this.loadRuleList();
    });
    this.loadApprovers();
  },

  loadDepartments() {
    return app.request({
      url: '/admin/department/list',
      method: 'GET'
    }).then(res => {
      const list = res.data || [];
      this.setData({ departments: [{ id: '', name: '全部部门' }, ...list.map(d => ({ id: d.id, name: d.name }))] });
    }).catch(() => {});
  },

  loadApprovers() {
    app.request({
      url: '/admin/employee/list',
      method: 'GET'
    }).then(res => {
      this.setData({ approvers: res.data || [] });
    }).catch(err => {
      console.error('获取审批人列表失败', err);
    });
  },

  loadRuleList() {
    app.request({
      url: '/admin/approval-flow/list',
      method: 'GET'
    }).then(res => {
      const list = (res.data || []).map(r => {
        const t = this.data.leaveTypes.find(x => String(x.id) === String(r.leaveType || '')) || this.data.leaveTypes[0];
        const d = this.data.departments.find(x => String(x.id) === String(r.departmentId || '')) || this.data.departments[0];
        const level = r.level != null ? Number(r.level) : (r.needAdmin ? 2 : 1);
        const lv = this.data.levels.find(x => Number(x.id) === Number(level)) || this.data.levels[0];
        return {
          ...r,
          recordType: r.recordType || 'leave',
          departmentId: (r.departmentId == null ? '' : r.departmentId),
          departmentName: d ? d.name : '全部部门',
          leaveType: (r.leaveType == null ? '' : r.leaveType),
          leaveTypeName: t ? t.name : '全部类型',
          level,
          levelName: lv ? lv.name : '主管(一级)'
        };
      });
      this.setData({ ruleList: list });
    }).catch(err => {
      console.error('获取审批规则失败', err);
    });
  },

  addRule() {
    const ruleList = this.data.ruleList;
    ruleList.push({
      recordType: 'leave',
      departmentId: '',
      departmentName: '全部部门',
      leaveType: '',
      leaveTypeName: '全部类型',
      minDays: '',
      maxDays: '',
      level: 1,
      levelName: '主管(一级)',
      approverId: null,
      approverName: ''
    });
    this.setData({ ruleList });
  },

  deleteRule(e) {
    const index = e.currentTarget.dataset.index;
    const ruleList = this.data.ruleList;
    ruleList.splice(index, 1);
    this.setData({ ruleList });
  },

  onMinDaysInput(e) {
    const index = e.currentTarget.dataset.index;
    this.setData({
      [`ruleList[${index}].minDays`]: e.detail.value
    });
  },

  onMaxDaysInput(e) {
    const index = e.currentTarget.dataset.index;
    this.setData({
      [`ruleList[${index}].maxDays`]: e.detail.value
    });
  },

  onDepartmentChange(e) {
    const index = e.currentTarget.dataset.index;
    const deptIndex = Number(e.detail.value) || 0;
    const dept = this.data.departments[deptIndex];
    this.setData({
      [`ruleList[${index}].departmentId`]: dept.id,
      [`ruleList[${index}].departmentName`]: dept.name
    });
  },

  onLeaveTypeChange(e) {
    const index = e.currentTarget.dataset.index;
    const typeIndex = Number(e.detail.value) || 0;
    const type = this.data.leaveTypes[typeIndex];
    this.setData({
      [`ruleList[${index}].leaveType`]: type.id,
      [`ruleList[${index}].leaveTypeName`]: type.name
    });
  },

  onLevelChange(e) {
    const index = e.currentTarget.dataset.index;
    const lvIndex = Number(e.detail.value) || 0;
    const lv = this.data.levels[lvIndex];
    const patch = {
      [`ruleList[${index}].level`]: lv.id,
      [`ruleList[${index}].levelName`]: lv.name
    };
    if (Number(lv.id) !== 2) {
      patch[`ruleList[${index}].approverId`] = null;
      patch[`ruleList[${index}].approverName`] = '';
    }
    this.setData(patch);
  },

  onApproverChange(e) {
    const index = e.currentTarget.dataset.index;
    const approverIndex = e.detail.value;
    const approver = this.data.approvers[approverIndex];
    this.setData({
      [`ruleList[${index}].approverId`]: approver.id,
      [`ruleList[${index}].approverName`]: approver.name
    });
  },

  handleSubmit() {
    const { ruleList } = this.data;

    for (let i = 0; i < ruleList.length; i++) {
      const rule = ruleList[i];
      if (!rule.minDays || !rule.maxDays) {
        wx.showToast({ title: `规则${i + 1}天数范围不完整`, icon: 'none' });
        return;
      }
      if (Number(rule.level) === 2 && !rule.approverId) {
        wx.showToast({ title: `规则${i + 1}请选择审批人`, icon: 'none' });
        return;
      }
    }

    this.setData({ loading: true });

    app.request({
      url: '/admin/approval-flow/save',
      method: 'POST',
      data: { rules: ruleList }
    }).then(res => {
      wx.showToast({ title: '保存成功', icon: 'success' });
    }).catch(err => {
      wx.showToast({ title: err.message || '保存失败', icon: 'none' });
    }).finally(() => {
      this.setData({ loading: false });
    });
  }
});
