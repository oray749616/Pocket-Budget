# Requirements Document

## Introduction

这是一个简洁的安卓资金管理应用，专注于提供类似计算器的简单体验。与市面上复杂的记账软件不同，该应用只关注核心功能：帮助用户管理每月可支配资金，跟踪计划支出，并实时显示剩余可用金额。应用使用SQLite进行本地数据存储，确保数据持久化和离线可用性。

## Requirements

### Requirement 1

**User Story:** 作为用户，我想要在每月发工资后输入当前可支配金额，以便开始新一轮的资金管理周期。

#### Acceptance Criteria

1. WHEN 用户打开应用 THEN 系统 SHALL 显示当前可支配金额输入界面
2. WHEN 用户输入可支配金额并确认 THEN 系统 SHALL 保存该金额到SQLite数据库
3. WHEN 用户输入金额 THEN 系统 SHALL 验证输入为有效的数字格式
4. IF 输入金额无效 THEN 系统 SHALL 显示错误提示信息
5. WHEN 用户成功输入可支配金额 THEN 系统 SHALL 自动设置下个发薪日为30天后

### Requirement 2

**User Story:** 作为用户，我想要添加已安排的计划支出项目，以便跟踪我的资金分配情况。

#### Acceptance Criteria

1. WHEN 用户点击添加支出按钮 THEN 系统 SHALL 显示支出项目输入界面
2. WHEN 用户输入支出描述和金额 THEN 系统 SHALL 验证输入的完整性和有效性
3. WHEN 用户确认添加支出 THEN 系统 SHALL 将支出项目保存到SQLite数据库
4. WHEN 添加新支出后 THEN 系统 SHALL 自动更新剩余可支配金额
5. IF 支出金额超过剩余可支配金额 THEN 系统 SHALL 显示警告提示但仍允许添加

### Requirement 3

**User Story:** 作为用户，我想要实时查看剩余可支配金额，以便了解我还能花费多少钱。

#### Acceptance Criteria

1. WHEN 用户查看主界面 THEN 系统 SHALL 显示当前剩余可支配金额
2. WHEN 添加或删除支出项目 THEN 系统 SHALL 立即重新计算并显示更新后的剩余金额
3. WHEN 剩余金额为负数 THEN 系统 SHALL 以红色显示金额并显示超支提示
4. WHEN 剩余金额为正数 THEN 系统 SHALL 以绿色显示金额
5. WHEN 系统计算剩余金额 THEN 计算公式 SHALL 为：可支配金额 - 所有计划支出的总和

### Requirement 4

**User Story:** 作为用户，我想要查看距离下个发薪日的剩余天数，以便了解当前预算周期的时间进度。

#### Acceptance Criteria

1. WHEN 用户查看主界面 THEN 系统 SHALL 显示距离下个发薪日的剩余天数
2. WHEN 系统计算剩余天数 THEN 计算 SHALL 基于30天周期和当前日期
3. WHEN 到达发薪日 THEN 系统 SHALL 提示用户输入新的可支配金额
4. WHEN 剩余天数少于7天 THEN 系统 SHALL 以橙色显示天数提醒

### Requirement 5

**User Story:** 作为用户，我想要查看所有计划支出的列表，以便管理和修改我的支出计划。

#### Acceptance Criteria

1. WHEN 用户查看支出列表 THEN 系统 SHALL 显示所有已添加的支出项目
2. WHEN 显示支出项目 THEN 每个项目 SHALL 包含描述、金额和添加日期
3. WHEN 用户长按支出项目 THEN 系统 SHALL 提供删除选项
4. WHEN 用户确认删除支出项目 THEN 系统 SHALL 从数据库中移除该项目并更新剩余金额
5. WHEN 支出列表为空 THEN 系统 SHALL 显示"暂无计划支出"的提示信息

### Requirement 6

**User Story:** 作为用户，我想要应用能够保存我的数据，以便每次打开应用时都能看到之前的计算结果。

#### Acceptance Criteria

1. WHEN 用户首次启动应用 THEN 系统 SHALL 创建SQLite数据库和必要的表结构
2. WHEN 用户输入数据 THEN 系统 SHALL 立即将数据保存到本地SQLite数据库
3. WHEN 用户重新打开应用 THEN 系统 SHALL 从数据库加载之前保存的数据
4. WHEN 数据库操作失败 THEN 系统 SHALL 显示错误信息并提供重试选项
5. WHEN 应用升级 THEN 系统 SHALL 保持数据完整性并进行必要的数据库迁移

### Requirement 7

**User Story:** 作为用户，我想要一个简洁直观的界面，以便快速完成资金管理操作。

#### Acceptance Criteria

1. WHEN 用户打开应用 THEN 界面 SHALL 采用类似计算器的简洁设计风格
2. WHEN 用户操作界面 THEN 所有主要功能 SHALL 在主界面上可见和可访问
3. WHEN 用户输入数字 THEN 系统 SHALL 提供数字键盘界面
4. WHEN 用户进行操作 THEN 系统 SHALL 提供即时的视觉反馈
5. WHEN 界面显示金额 THEN 数字 SHALL 使用易读的字体和适当的字号

### Requirement 8

**User Story:** 作为用户，我想要能够重置当前预算周期，以便在特殊情况下重新开始资金管理。

#### Acceptance Criteria

1. WHEN 用户访问设置菜单 THEN 系统 SHALL 提供重置预算周期的选项
2. WHEN 用户选择重置预算周期 THEN 系统 SHALL 显示确认对话框
3. WHEN 用户确认重置 THEN 系统 SHALL 清除当前所有支出记录
4. WHEN 重置完成后 THEN 系统 SHALL 提示用户输入新的可支配金额
5. WHEN 重置操作 THEN 系统 SHALL 保留历史数据以供将来参考（可选功能）
