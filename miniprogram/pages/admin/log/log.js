const app = getApp();

Page({
  data: {
    startDate: '',
    endDate: '',
    logList: []
  },

  onLoad() {
    if (!app.checkLogin()) return;
    const now = new Date();
    const endDate = this.formatDate(now);
    const startDate = this.formatDate(new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000));
    this.setData({ startDate, endDate });
    this.handleSearch();
  },

  formatDate(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  },

  onStartDateChange(e) {
    this.setData({ startDate: e.detail.value });
  },

  onEndDateChange(e) {
    this.setData({ endDate: e.detail.value });
  },

  handleSearch() {
    const { startDate, endDate } = this.data;

    app.request({
      url: '/admin/log/list',
      method: 'GET',
      data: { startDate, endDate }
    }).then(res => {
      this.setData({ logList: res.data || [] });
    }).catch(err => {
      console.error('获取日志列表失败', err);
    });
  }
});
