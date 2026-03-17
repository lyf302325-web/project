const BASE = 'http://localhost:8080/api';
let TOKEN = '';
let USER_INFO = null;

// ========== 通用请求 ==========
async function request(url, method, data) {
  const opts = {
    method: method || 'GET',
    headers: { 'Content-Type': 'application/json' }
  };
  if (TOKEN) opts.headers['Authorization'] = 'Bearer ' + TOKEN;
  if (data && method !== 'GET') opts.body = JSON.stringify(data);
  if (data && method === 'GET') {
    const params = new URLSearchParams(data).toString();
    url += '?' + params;
  }
  const res = await fetch(BASE + url, opts);
  const json = await res.json();
  if (json.code !== 200) throw new Error(json.message || '请求失败');
  return json.data;
}

// ========== 登录/登出 ==========
async function doLogin() {
  const username = document.getElementById('loginUser').value;
  const password = document.getElementById('loginPass').value;
  try {
    const data = await request('/auth/login', 'POST', { username, password });
    TOKEN = data.token;
    USER_INFO = data.userInfo;
    document.getElementById('loginPage').style.display = 'none';
    document.getElementById('mainPage').style.display = 'flex';
    document.getElementById('headerUser').textContent = '当前用户: ' + USER_INFO.name;
    switchPage('dashboard', document.querySelector('.nav-item'));
  } catch (e) {
    document.getElementById('loginMsg').textContent = e.message;
  }
}

function doLogout() {
  TOKEN = '';
  USER_INFO = null;
  document.getElementById('mainPage').style.display = 'none';
  document.getElementById('loginPage').style.display = 'flex';
  document.getElementById('loginMsg').textContent = '';
}

// ========== 页面切换 ==========
function switchPage(page, el) {
  document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
  if (el) el.classList.add('active');
  const titles = { dashboard:'数据概览', employee:'员工管理', department:'部门管理', register:'注册审核', statistics:'数据统计', log:'操作日志', notice:'消息配置', rule:'考勤规则' };
  document.getElementById('headerTitle').textContent = titles[page] || '';
  const tpl = document.getElementById('tpl-' + page);
  if (tpl) {
    document.getElementById('pageContent').innerHTML = tpl.innerHTML;
  }
  const loaders = { dashboard: loadDashboard, employee: loadEmployees, department: loadDepartments, register: loadRegister, statistics: initStatistics, log: initLogs, notice: loadNotice, rule: loadRule };
  if (loaders[page]) loaders[page]();
}

// ========== 弹窗 ==========
function showModal(title, html, onOk) {
  document.getElementById('modalTitle').textContent = title;
  document.getElementById('modalContent').innerHTML = html;
  document.getElementById('modal').style.display = 'flex';
  document.getElementById('modalOk').onclick = function () { if (onOk) onOk(); };
}
function closeModal() { document.getElementById('modal').style.display = 'none'; }

// ========== 数据概览 ==========
async function loadDashboard() {
  try {
    const stats = await request('/admin/today-stats', 'GET');
    document.getElementById('dashStats').innerHTML =
      `<div class="stat-card blue"><div class="val">${stats.totalEmployees}</div><div class="lbl">总员工数</div></div>` +
      `<div class="stat-card green"><div class="val">${stats.checkedIn}</div><div class="lbl">已签到</div></div>` +
      `<div class="stat-card orange"><div class="val">${stats.late}</div><div class="lbl">迟到</div></div>` +
      `<div class="stat-card red"><div class="val">${stats.absent}</div><div class="lbl">缺勤</div></div>`;
    const pending = await request('/admin/pending-count', 'GET');
    document.getElementById('dashPending').innerHTML =
      `<p>待审批请假: <b>${pending.leave}</b> | 待审批外勤: <b>${pending.fieldwork}</b> | 待处理申诉: <b>${pending.appeal}</b></p>`;
  } catch (e) { console.error(e); }
}

// ========== 员工管理 ==========
async function loadEmployees() {
  try {
    const keyword = document.getElementById('empKeyword') ? document.getElementById('empKeyword').value : '';
    const list = await request('/admin/employee/list', 'GET', { keyword });
    const tbody = document.getElementById('empBody');
    tbody.innerHTML = list.map(u =>
      `<tr>
        <td>${u.employeeNo||''}</td><td>${u.name||''}</td><td>${u.username||''}</td>
        <td>${u.departmentName||''}</td><td>${u.position||''}</td><td>${u.phone||''}</td>
        <td>${u.status===1?'<span class="tag tag-green">正常</span>':'<span class="tag tag-red">禁用</span>'}</td>
        <td>
          <button class="btn-sm btn-primary" onclick="editEmployee(${u.id})">编辑</button>
          <button class="btn-sm btn-danger" onclick="deleteEmployee(${u.id})">删除</button>
          <button class="btn-sm" onclick="resetPwd(${u.id})">重置密码</button>
        </td>
      </tr>`
    ).join('');
  } catch (e) { console.error(e); }
}

function showAddEmployee() {
  const html = `
    <div class="form-row"><label>工号</label><input id="f_empNo"></div>
    <div class="form-row"><label>姓名</label><input id="f_name"></div>
    <div class="form-row"><label>账号</label><input id="f_user"></div>
    <div class="form-row"><label>密码</label><input id="f_pass" value="123456"></div>
    <div class="form-row"><label>手机号</label><input id="f_phone"></div>
    <div class="form-row"><label>部门ID</label><input id="f_dept" type="number"></div>
    <div class="form-row"><label>岗位</label><input id="f_pos"></div>`;
  showModal('添加员工', html, async () => {
    try {
      await request('/admin/employee/add', 'POST', {
        employeeNo: document.getElementById('f_empNo').value,
        name: document.getElementById('f_name').value,
        username: document.getElementById('f_user').value,
        password: document.getElementById('f_pass').value,
        phone: document.getElementById('f_phone').value,
        departmentId: document.getElementById('f_dept').value || null,
        position: document.getElementById('f_pos').value
      });
      closeModal(); loadEmployees();
    } catch (e) { alert(e.message); }
  });
}

async function editEmployee(id) {
  try {
    const u = await request(`/admin/employee/detail/${id}`, 'GET');
    const html = `
      <div class="form-row"><label>工号</label><input id="f_empNo" value="${u.employeeNo||''}"></div>
      <div class="form-row"><label>姓名</label><input id="f_name" value="${u.name||''}"></div>
      <div class="form-row"><label>账号</label><input id="f_user" value="${u.username||''}"></div>
      <div class="form-row"><label>手机号</label><input id="f_phone" value="${u.phone||''}"></div>
      <div class="form-row"><label>部门ID</label><input id="f_dept" type="number" value="${u.departmentId||''}"></div>
      <div class="form-row"><label>岗位</label><input id="f_pos" value="${u.position||''}"></div>`;
    showModal('编辑员工', html, async () => {
      try {
        await request(`/admin/employee/update/${id}`, 'PUT', {
          employeeNo: document.getElementById('f_empNo').value,
          name: document.getElementById('f_name').value,
          username: document.getElementById('f_user').value,
          phone: document.getElementById('f_phone').value,
          departmentId: document.getElementById('f_dept').value || null,
          position: document.getElementById('f_pos').value
        });
        closeModal(); loadEmployees();
      } catch (e) { alert(e.message); }
    });
  } catch (e) { alert(e.message); }
}

async function deleteEmployee(id) {
  if (!confirm('确定删除该员工？')) return;
  try { await request(`/admin/employee/delete/${id}`, 'DELETE'); loadEmployees(); } catch (e) { alert(e.message); }
}

async function resetPwd(id) {
  if (!confirm('确定重置密码为123456？')) return;
  try { await request(`/admin/employee/reset-password/${id}`, 'POST'); alert('重置成功'); } catch (e) { alert(e.message); }
}

// ========== 部门管理 ==========
async function loadDepartments() {
  try {
    const list = await request('/admin/department/list', 'GET');
    document.getElementById('deptBody').innerHTML = list.map(d =>
      `<tr><td>${d.id}</td><td>${d.name}</td><td>${d.managerName||'未设置'}</td>
       <td><button class="btn-sm btn-primary" onclick="editDept(${d.id},'${d.name}',${d.managerId||'null'})">编辑</button>
       <button class="btn-sm btn-danger" onclick="deleteDept(${d.id})">删除</button></td></tr>`
    ).join('');
  } catch (e) { console.error(e); }
}

function showAddDept() {
  const html = `<div class="form-row"><label>部门名称</label><input id="f_dname"></div>
    <div class="form-row"><label>负责人ID</label><input id="f_dmgr" type="number"></div>`;
  showModal('添加部门', html, async () => {
    try {
      await request('/admin/department/add', 'POST', { name: document.getElementById('f_dname').value, managerId: document.getElementById('f_dmgr').value || null });
      closeModal(); loadDepartments();
    } catch (e) { alert(e.message); }
  });
}

function editDept(id, name, managerId) {
  const html = `<div class="form-row"><label>部门名称</label><input id="f_dname" value="${name}"></div>
    <div class="form-row"><label>负责人ID</label><input id="f_dmgr" type="number" value="${managerId||''}"></div>`;
  showModal('编辑部门', html, async () => {
    try {
      await request(`/admin/department/update/${id}`, 'PUT', { name: document.getElementById('f_dname').value, managerId: document.getElementById('f_dmgr').value || null });
      closeModal(); loadDepartments();
    } catch (e) { alert(e.message); }
  });
}

async function deleteDept(id) {
  if (!confirm('确定删除该部门？')) return;
  try { await request(`/admin/department/delete/${id}`, 'DELETE'); loadDepartments(); } catch (e) { alert(e.message); }
}

// ========== 注册审核 ==========
async function loadRegister() {
  try {
    const list = await request('/admin/employee/pending', 'GET');
    const tbody = document.getElementById('regBody');
    if (!list.length) { tbody.innerHTML = '<tr><td colspan="5" style="text-align:center;color:#999">暂无待审核申请</td></tr>'; return; }
    tbody.innerHTML = list.map(u =>
      `<tr><td>${u.username}</td><td>${u.name}</td><td>${u.employeeNo||''}</td><td>${u.phone||''}</td>
       <td><button class="btn-sm btn-success" onclick="approveReg(${u.id},1)">通过</button>
       <button class="btn-sm btn-danger" onclick="approveReg(${u.id},2)">驳回</button></td></tr>`
    ).join('');
  } catch (e) { console.error(e); }
}

async function approveReg(id, status) {
  try { await request(`/admin/employee/approve/${id}`, 'POST', { registerStatus: status }); loadRegister(); } catch (e) { alert(e.message); }
}

// ========== 数据统计 ==========
function initStatistics() {
  const now = new Date();
  const m = now.getFullYear() + '-' + String(now.getMonth()+1).padStart(2,'0');
  document.getElementById('statMonth').value = m;
  loadStatistics();
}

async function loadStatistics() {
  try {
    const month = document.getElementById('statMonth').value;
    const data = await request('/admin/statistics', 'GET', { month });
    const s = data.summary;
    document.getElementById('statSummary').innerHTML =
      `<div class="stat-card green"><div class="val">${s.normal}</div><div class="lbl">正常</div></div>` +
      `<div class="stat-card orange"><div class="val">${s.late}</div><div class="lbl">迟到</div></div>` +
      `<div class="stat-card orange"><div class="val">${s.early}</div><div class="lbl">早退</div></div>` +
      `<div class="stat-card red"><div class="val">${s.absent}</div><div class="lbl">缺勤</div></div>` +
      `<div class="stat-card blue"><div class="val">${s.leave}</div><div class="lbl">请假</div></div>`;
    document.getElementById('statBody').innerHTML = (data.employees||[]).map(e =>
      `<tr><td>${e.name}</td><td>${e.employeeNo}</td><td>${e.normal}</td><td>${e.late}</td><td>${e.early}</td><td>${e.absent}</td><td>${e.leave}</td></tr>`
    ).join('');
  } catch (e) { console.error(e); }
}

async function exportCSV() {
  try {
    const month = document.getElementById('statMonth').value;
    const csv = await request('/admin/statistics/export', 'GET', { month });
    showModal('导出CSV', `<pre style="white-space:pre-wrap;font-size:12px;max-height:400px;overflow:auto">${csv}</pre>`, closeModal);
  } catch (e) { alert(e.message); }
}

// ========== 操作日志 ==========
function initLogs() {
  const now = new Date();
  const end = now.toISOString().slice(0,10);
  const start = new Date(now.getTime()-7*86400000).toISOString().slice(0,10);
  document.getElementById('logStart').value = start;
  document.getElementById('logEnd').value = end;
  loadLogs();
}

async function loadLogs() {
  try {
    const startDate = document.getElementById('logStart').value;
    const endDate = document.getElementById('logEnd').value;
    const list = await request('/admin/log/list', 'GET', { startDate, endDate });
    document.getElementById('logBody').innerHTML = list.map(l =>
      `<tr><td>${l.operatorName||''}</td><td>${l.action||''}</td><td>${l.detail||''}</td><td>${l.ip||''}</td><td>${l.createTime||''}</td></tr>`
    ).join('');
  } catch (e) { console.error(e); }
}

// ========== 消息配置 ==========
async function loadNotice() {
  try {
    const c = await request('/admin/notice/config', 'GET');
    document.getElementById('ncAlert').checked = c.attendanceAlert;
    document.getElementById('ncApproval').checked = c.approvalResult;
    document.getElementById('ncPending').checked = c.pendingApproval;
    document.getElementById('ncCheckin').checked = c.checkInReminder;
    document.getElementById('ncMorning').value = c.morningReminderTime || '08:50';
    document.getElementById('ncEvening').value = c.eveningReminderTime || '18:00';
  } catch (e) { console.error(e); }
}

async function saveNotice() {
  try {
    await request('/admin/notice/config', 'POST', {
      attendanceAlert: document.getElementById('ncAlert').checked,
      approvalResult: document.getElementById('ncApproval').checked,
      pendingApproval: document.getElementById('ncPending').checked,
      checkInReminder: document.getElementById('ncCheckin').checked,
      morningReminderTime: document.getElementById('ncMorning').value,
      eveningReminderTime: document.getElementById('ncEvening').value
    });
    alert('保存成功');
  } catch (e) { alert(e.message); }
}

// ========== 考勤规则 ==========
async function loadRule() {
  try {
    const r = await request('/admin/rule/get', 'GET');
    if (r) {
      document.getElementById('ruleIn').value = r.checkInTime || '09:00';
      document.getElementById('ruleOut').value = r.checkOutTime || '18:00';
      document.getElementById('ruleLate').value = r.lateThreshold || 30;
      document.getElementById('ruleEarly').value = r.earlyThreshold || 30;
      document.getElementById('ruleRange').value = r.checkInRange || 500;
    }
  } catch (e) { console.error(e); }
}

async function saveRule() {
  try {
    await request('/admin/rule/save', 'POST', {
      checkInTime: document.getElementById('ruleIn').value,
      checkOutTime: document.getElementById('ruleOut').value,
      lateThreshold: parseInt(document.getElementById('ruleLate').value),
      earlyThreshold: parseInt(document.getElementById('ruleEarly').value),
      checkInRange: parseInt(document.getElementById('ruleRange').value)
    });
    alert('保存成功');
  } catch (e) { alert(e.message); }
}
