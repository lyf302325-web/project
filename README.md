# 中小企业考勤管理系统

## 项目简介
本系统是一个基于微信小程序 + Spring Boot + MySQL 的中小企业考勤管理系统，实现考勤流程数字化、审批层级化、数据可视化。

## 技术栈
- **前端**：微信小程序
- **后端**：Spring Boot 2.7 + MyBatis Plus
- **数据库**：MySQL 8.0
- **安全**：Spring Security + JWT

## 项目结构
```
毕设/
├── miniprogram/          # 微信小程序前端
│   ├── pages/            # 页面文件
│   │   ├── login/        # 登录页
│   │   ├── index/        # 首页
│   │   ├── checkin/      # 打卡页
│   │   ├── attendance/   # 考勤记录
│   │   ├── leave/        # 请假管理
│   │   ├── fieldwork/    # 外勤管理
│   │   ├── appeal/       # 异常申诉
│   │   ├── profile/      # 个人中心
│   │   └── admin/        # 管理员端
│   ├── images/           # 图标资源
│   ├── app.js            # 小程序入口
│   ├── app.json          # 小程序配置
│   └── app.wxss          # 全局样式
│
└── backend/              # Spring Boot后端
    ├── src/main/java/com/attendance/
    │   ├── controller/   # 控制器
    │   ├── service/      # 服务层
    │   ├── mapper/       # 数据访问层
    │   ├── entity/       # 实体类
    │   ├── config/       # 配置类
    │   ├── security/     # 安全相关
    │   └── common/       # 公共类
    ├── src/main/resources/
    │   └── application.yml  # 配置文件
    ├── sql/
    │   └── init.sql      # 数据库初始化脚本
    └── pom.xml           # Maven配置
```

## 功能模块

### 员工端
- 登录/修改密码
- 定位签到/签退
- 查看考勤记录
- 请假申请
- 外勤报备
- 异常考勤申诉
- 导出个人报表

### 管理员端
- 员工信息管理
- 部门管理
- 考勤规则配置
- 分级审批管理
- 考勤数据统计
- 导出报表
- 异常记录修正
- 查看操作日志
- 消息通知配置

## 部署说明

### 1. 数据库配置
1. 安装 MySQL 8.0
2. 创建数据库并执行初始化脚本：
```bash
mysql -u root -p < backend/sql/init.sql
```
3. 默认管理员账号：admin，密码：123456

### 2. 后端部署
1. 修改 `backend/src/main/resources/application.yml` 中的数据库配置
2. 使用 Maven 构建项目：
```bash
cd backend
mvn clean package
```
3. 运行项目：
```bash
java -jar target/attendance-system-1.0.0.jar
```

### 3. 小程序配置
1. 使用微信开发者工具打开 `miniprogram` 目录
2. 修改 `app.js` 中的 `baseUrl` 为后端服务地址
3. 在 `project.config.json` 中修改 `appid` 为你的小程序 AppID
4. 添加 tabBar 图标文件到 `images` 目录（见下方说明）

### 4. tabBar 图标说明
需要在 `miniprogram/images/` 目录下添加以下图标文件（建议尺寸 81x81 像素）：
- `home.png` - 首页图标
- `home-active.png` - 首页选中图标
- `checkin.png` - 打卡图标
- `checkin-active.png` - 打卡选中图标
- `attendance.png` - 考勤图标
- `attendance-active.png` - 考勤选中图标
- `profile.png` - 我的图标
- `profile-active.png` - 我的选中图标

可从 iconfont.cn 或其他图标网站下载合适的图标。

## 注意事项
1. 小程序需要配置合法域名才能正常请求后端接口
2. 定位功能需要用户授权
3. 生产环境请修改 JWT 密钥和数据库密码
4. 建议使用 HTTPS 部署后端服务

## 默认账号
- 管理员：admin / 123456
