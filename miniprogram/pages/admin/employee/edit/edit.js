const app = getApp();

Page({
  data: {
    isEdit: false,
    employeeId: null,
    formData: {
      employeeNo: '',
      name: '',
      departmentId: null,
      departmentName: '',
      hireDate: '',
      resignDate: '',
      position: '',
      phone: '',
      username: '',
      password: '',
      roleType: null,
      roleName: ''
    },
    departments: [],
    roles: [
      { id: 0, name: '普通员工' },
      { id: 1, name: '管理员' },
      { id: 2, name: '部门主管' }
    ],
    loading: false
  },

  onLoad(options) {
    if (!app.checkLogin()) return;
    this.loadDepartments();
    
    if (options.id) {
      this.setData({ isEdit: true, employeeId: options.id });
      wx.setNavigationBarTitle({ title: '编辑员工' });
      this.loadEmployeeDetail(options.id);
    } else {
      wx.setNavigationBarTitle({ title: '添加员工' });
    }
  },

  loadDepartments() {
    app.request({
      url: '/admin/department/list',
      method: 'GET'
    }).then(res => {
      this.setData({ departments: res.data || [] });
    }).catch(err => {
      console.error('获取部门列表失败', err);
    });
  },

  loadEmployeeDetail(id) {
    app.request({
      url: `/admin/employee/detail/${id}`,
      method: 'GET'
    }).then(res => {
      const data = res.data || {};
      this.setData({
        formData: {
          employeeNo: data.employeeNo || '',
          name: data.name || '',
          departmentId: data.departmentId,
          departmentName: data.departmentName || '',
          hireDate: data.hireDate || '',
          resignDate: data.resignDate || '',
          position: data.position || '',
          phone: data.phone || '',
          username: data.username || '',
          roleType: data.roleType,
          roleName: data.roleType === 1 ? '管理员' : data.roleType === 2 ? '部门主管' : '普通员工'
        }
      });
    }).catch(err => {
      console.error('获取员工详情失败', err);
    });
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({
      [`formData.${field}`]: e.detail.value
    });
  },

  onDepartmentChange(e) {
    const index = e.detail.value;
    const dept = this.data.departments[index];
    this.setData({
      'formData.departmentId': dept.id,
      'formData.departmentName': dept.name
    });
  },

  onRoleChange(e) {
    const index = e.detail.value;
    const role = this.data.roles[index];
    this.setData({
      'formData.roleType': role.id,
      'formData.roleName': role.name
    });
  },

  onHireDateChange(e) {
    this.setData({ 'formData.hireDate': e.detail.value });
  },

  onResignDateChange(e) {
    this.setData({ 'formData.resignDate': e.detail.value });
  },

  handleSubmit() {
    const { formData, isEdit, employeeId } = this.data;

    if (!formData.employeeNo.trim()) {
      wx.showToast({ title: '请输入工号', icon: 'none' });
      return;
    }
    if (!formData.name.trim()) {
      wx.showToast({ title: '请输入姓名', icon: 'none' });
      return;
    }
    if (!formData.username.trim()) {
      wx.showToast({ title: '请输入登录账号', icon: 'none' });
      return;
    }
    if (!isEdit && !formData.password.trim()) {
      wx.showToast({ title: '请输入初始密码', icon: 'none' });
      return;
    }

    this.setData({ loading: true });

    const url = isEdit ? `/admin/employee/update/${employeeId}` : '/admin/employee/add';
    const method = isEdit ? 'PUT' : 'POST';

    app.request({
      url,
      method,
      data: formData
    }).then(res => {
      wx.showToast({ title: isEdit ? '修改成功' : '添加成功', icon: 'success' });
      setTimeout(() => {
        wx.navigateBack();
      }, 1500);
    }).catch(err => {
      wx.showToast({ title: err.message || '操作失败', icon: 'none' });
    }).finally(() => {
      this.setData({ loading: false });
    });
  },

  handleResetPassword() {
    wx.showModal({
      title: '提示',
      content: '确定要重置该员工密码吗？重置后密码为123456',
      success: (res) => {
        if (res.confirm) {
          app.request({
            url: `/admin/employee/reset-password/${this.data.employeeId}`,
            method: 'POST'
          }).then(res => {
            wx.showToast({ title: '重置成功', icon: 'success' });
          }).catch(err => {
            wx.showToast({ title: err.message || '重置失败', icon: 'none' });
          });
        }
      }
    });
  }
});
