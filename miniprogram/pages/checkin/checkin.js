const app = getApp();

Page({
  data: {
    currentTime: '',
    currentDate: '',
    location: {},
    locationValid: false,
    canCheckIn: false,
    checkType: 'in',
    todayRecord: {},
    ruleInfo: {},
    photoVerify: false,
    photoPath: ''
  },

  timer: null,

  onLoad() {
    this.updateTime();
    this.timer = setInterval(() => {
      this.updateTime();
    }, 1000);
  },

  onShow() {
    if (!app.checkLogin()) return;
    this.getLocation();
    this.loadTodayRecord();
    this.loadRuleInfo();
    this.ensureSubscribeAbnormal();
  },

  onUnload() {
    if (this.timer) {
      clearInterval(this.timer);
    }
  },

  updateTime() {
    const now = new Date();
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    const seconds = String(now.getSeconds()).padStart(2, '0');
    
    const year = now.getFullYear();
    const month = now.getMonth() + 1;
    const day = now.getDate();
    const weekDays = ['日', '一', '二', '三', '四', '五', '六'];
    const weekDay = weekDays[now.getDay()];

    this.setData({
      currentTime: `${hours}:${minutes}:${seconds}`,
      currentDate: `${year}年${month}月${day}日 星期${weekDay}`
    });
  },

  getLocation() {
    wx.showLoading({ title: '获取位置中...' });
    wx.getLocation({
      type: 'gcj02',
      success: (res) => {
        this.setData({
          location: {
            latitude: res.latitude,
            longitude: res.longitude,
            address: '位置获取成功'
          }
        });
        this.getAddressFromLocation(res.latitude, res.longitude);
        this.checkLocationValid(res.latitude, res.longitude);
      },
      fail: (err) => {
        console.error('获取位置失败', err);
        this.setData({
          location: { address: '位置获取失败，请检查定位权限' },
          locationValid: false,
          canCheckIn: false
        });
        wx.showToast({ title: '请开启定位权限', icon: 'none' });
      },
      complete: () => {
        wx.hideLoading();
      }
    });
  },

  getAddressFromLocation(latitude, longitude) {
    // 这里可以调用逆地理编码接口获取详细地址
    // 简化处理，直接显示经纬度
    this.setData({
      'location.address': `经度: ${longitude.toFixed(6)}, 纬度: ${latitude.toFixed(6)}`
    });
  },

  checkLocationValid(latitude, longitude) {
    app.request({
      url: '/attendance/check-location',
      method: 'POST',
      data: { latitude, longitude }
    }).then(res => {
      this.setData({
        locationValid: res.data.valid,
        canCheckIn: res.data.valid
      });
    }).catch(err => {
      console.error('校验位置失败', err);
      // 开发阶段默认允许打卡
      this.setData({
        locationValid: true,
        canCheckIn: true
      });
    });
  },

  refreshLocation() {
    this.getLocation();
  },

  loadTodayRecord() {
    app.request({
      url: '/attendance/today',
      method: 'GET'
    }).then(res => {
      const record = res.data || {};
      this.setData({
        todayRecord: record,
        checkType: record.checkInTime && !record.checkOutTime ? 'out' : 'in'
      });
    }).catch(err => {
      console.error('获取今日记录失败', err);
    });
  },

  loadRuleInfo() {
    app.request({
      url: '/attendance/rule',
      method: 'GET'
    }).then(res => {
      const rule = res.data || {};
      this.setData({
        ruleInfo: rule,
        photoVerify: rule.photoVerify == 1
      });
    }).catch(err => {
      console.error('获取考勤规则失败', err);
    });
  },

  ensureSubscribeAbnormal() {
    const templateId = app.globalData.templateIdAbnormal;
    if (!templateId) return;
    const key = `subscribed_${templateId}`;
    if (wx.getStorageSync(key)) return;
    wx.requestSubscribeMessage({
      tmplIds: [templateId],
      success: (res) => {
        if (res && res[templateId] === 'accept') {
          wx.setStorageSync(key, true);
        } else {
          wx.setStorageSync(key, true);
        }
      },
      fail: () => {
        wx.setStorageSync(key, true);
      }
    });
  },

  takePhoto() {
    return new Promise((resolve, reject) => {
      wx.chooseMedia({
        count: 1,
        mediaType: ['image'],
        sourceType: ['camera'],
        camera: 'front',
        success: (res) => {
          const tempFilePath = res.tempFiles[0].tempFilePath;
          this.setData({ photoPath: tempFilePath });
          wx.showLoading({ title: '上传照片中...' });
          wx.uploadFile({
            url: app.globalData.baseUrl + '/file/upload',
            filePath: tempFilePath,
            name: 'file',
            header: {
              'Authorization': app.globalData.token ? 'Bearer ' + app.globalData.token : ''
            },
            success: (uploadRes) => {
              const data = JSON.parse(uploadRes.data);
              if (data.code === 200) {
                resolve(data.data);
              } else {
                reject(new Error(data.message || '上传失败'));
              }
            },
            fail: () => reject(new Error('上传失败')),
            complete: () => wx.hideLoading()
          });
        },
        fail: () => reject(new Error('取消拍照'))
      });
    });
  },

  handleCheckin() {
    if (!this.data.canCheckIn) {
      wx.showToast({ title: '不在打卡范围内', icon: 'none' });
      return;
    }

    if (this.data.photoVerify) {
      this.takePhoto().then(photoUrl => {
        this.doCheckin(photoUrl);
      }).catch(err => {
        if (err.message !== '取消拍照') {
          wx.showToast({ title: err.message || '拍照失败', icon: 'none' });
        }
      });
    } else {
      this.doCheckin('');
    }
  },

  doCheckin(photo) {
    const { location, checkType } = this.data;
    
    wx.showLoading({ title: '打卡中...' });
    
    app.request({
      url: '/attendance/checkin',
      method: 'POST',
      data: {
        type: checkType,
        latitude: location.latitude,
        longitude: location.longitude,
        address: location.address,
        photo: photo
      }
    }).then(res => {
      wx.showToast({ title: checkType === 'in' ? '签到成功' : '签退成功', icon: 'success' });
      this.setData({ photoPath: '' });
      this.loadTodayRecord();
    }).catch(err => {
      wx.showToast({ title: err.message || '打卡失败', icon: 'none' });
    }).finally(() => {
      wx.hideLoading();
    });
  }
});
