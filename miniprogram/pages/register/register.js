const app = getApp();

Page({
  data: {
    form: {
      username: '',
      name: '',
      phone: '',
      departmentId: '',
      password: ''
    },
    confirmPassword: '',
    loading: false
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({
      form: {
        ...this.data.form,
        [field]: e.detail.value
      }
    });
  },

  onConfirmPassword(e) {
    this.setData({ confirmPassword: e.detail.value });
  },

  handleSubmit() {
    const { form, confirmPassword } = this.data;

    if (!form.username.trim()) {
      wx.showToast({ title: '请输入账号', icon: 'none' });
      return;
    }
    if (!form.name.trim()) {
      wx.showToast({ title: '请输入姓名', icon: 'none' });
      return;
    }
    if (!form.password.trim()) {
      wx.showToast({ title: '请输入密码', icon: 'none' });
      return;
    }
    if (form.password !== confirmPassword) {
      wx.showToast({ title: '两次密码不一致', icon: 'none' });
      return;
    }

    const payload = {
      username: form.username,
      employeeNo: form.username,
      name: form.name,
      phone: form.phone || null,
      departmentId: form.departmentId ? Number(form.departmentId) : null,
      password: form.password
    };

    this.setData({ loading: true });

    app.request({
      url: '/auth/register',
      method: 'POST',
      data: payload
    }).then(res => {
      wx.showModal({
        title: '提示',
        content: res.message || '注册申请已提交，请等待管理员审核',
        showCancel: false,
        success: () => {
          wx.navigateBack();
        }
      });
    }).catch(err => {
      wx.showToast({ title: err.message || '提交失败', icon: 'none' });
    }).finally(() => {
      this.setData({ loading: false });
    });
  }
});
