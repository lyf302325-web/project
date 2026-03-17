const app = getApp();

Page({
  data: {
    departmentList: [],
    employees: [],
    showModal: false,
    isEdit: false,
    editId: null,
    formData: {
      name: '',
      managerId: null,
      managerName: ''
    }
  },

  onShow() {
    if (!app.checkLogin()) return;
    this.loadDepartmentList();
    this.loadEmployees();
  },

  loadDepartmentList() {
    app.request({
      url: '/admin/department/list',
      method: 'GET'
    }).then(res => {
      this.setData({ departmentList: res.data || [] });
    }).catch(err => {
      console.error('获取部门列表失败', err);
    });
  },

  loadEmployees() {
    app.request({
      url: '/admin/employee/list',
      method: 'GET'
    }).then(res => {
      this.setData({ employees: res.data || [] });
    }).catch(err => {
      console.error('获取员工列表失败', err);
    });
  },

  showAddModal() {
    this.setData({
      showModal: true,
      isEdit: false,
      editId: null,
      formData: { name: '', managerId: null, managerName: '' }
    });
  },

  showEditModal(e) {
    const item = e.currentTarget.dataset.item;
    this.setData({
      showModal: true,
      isEdit: true,
      editId: item.id,
      formData: {
        name: item.name,
        managerId: item.managerId,
        managerName: item.managerName || ''
      }
    });
  },

  closeModal() {
    this.setData({ showModal: false });
  },

  onNameInput(e) {
    this.setData({ 'formData.name': e.detail.value });
  },

  onManagerChange(e) {
    const index = e.detail.value;
    const emp = this.data.employees[index];
    this.setData({
      'formData.managerId': emp.id,
      'formData.managerName': emp.name
    });
  },

  handleSubmit() {
    const { formData, isEdit, editId } = this.data;

    if (!formData.name.trim()) {
      wx.showToast({ title: '请输入部门名称', icon: 'none' });
      return;
    }

    const url = isEdit ? `/admin/department/update/${editId}` : '/admin/department/add';
    const method = isEdit ? 'PUT' : 'POST';

    app.request({
      url,
      method,
      data: formData
    }).then(res => {
      wx.showToast({ title: isEdit ? '修改成功' : '添加成功', icon: 'success' });
      this.closeModal();
      this.loadDepartmentList();
    }).catch(err => {
      wx.showToast({ title: err.message || '操作失败', icon: 'none' });
    });
  },

  handleDelete(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '提示',
      content: '确定要删除该部门吗？',
      success: (res) => {
        if (res.confirm) {
          app.request({
            url: `/admin/department/delete/${id}`,
            method: 'DELETE'
          }).then(res => {
            wx.showToast({ title: '删除成功', icon: 'success' });
            this.loadDepartmentList();
          }).catch(err => {
            wx.showToast({ title: err.message || '删除失败', icon: 'none' });
          });
        }
      }
    });
  }
});
