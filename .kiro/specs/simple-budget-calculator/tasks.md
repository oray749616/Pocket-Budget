# Implementation Plan - 简洁资金管理安卓应用

- [x] 1. 项目初始化和基础架构设置
  - 创建新的Android项目，配置Kotlin和最低SDK版本为API 24
  - 添加必要的依赖项：Room、Hilt、Material Design 3、Coroutines、ViewModel
  - 配置Hilt依赖注入的基础设置
  - 创建基本的包结构：data、domain、presentation
  - _Requirements: 6.1, 7.1_

- [ ] 2. 定义数据模型和Room数据库结构
  - [ ] 2.1 创建BudgetPeriod实体类
    - 实现BudgetPeriod数据类，包含id、disposableAmount、createdDate、paydayDate、isActive字段
    - 添加Room注解：@Entity、@PrimaryKey、@ColumnInfo
    - 为BudgetPeriod编写单元测试，验证数据完整性
    - _Requirements: 1.2, 4.2, 6.2_

  - [ ] 2.2 创建Expense实体类
    - 实现Expense数据类，包含id、budgetPeriodId、description、amount、createdDate字段
    - 配置与BudgetPeriod的外键关系，设置CASCADE删除
    - 为Expense实体编写单元测试
    - _Requirements: 2.2, 5.2, 6.2_

  - [ ] 2.3 实现Room数据库和DAO接口
    - 创建BudgetDao接口，定义所有数据库操作方法
    - 实现BudgetDatabase抽象类，配置Room数据库
    - 为DAO方法编写单元测试，使用内存数据库
    - _Requirements: 6.1, 6.3_

- [ ] 3. 实现Repository层和数据访问逻辑
  - [ ] 3.1 定义Repository接口
    - 创建BudgetRepository接口，定义所有数据操作的抽象方法
    - 使用Flow返回类型支持响应式数据流
    - 定义Result包装类处理操作结果和错误
    - _Requirements: 6.3, 6.4_

  - [ ] 3.2 实现Repository具体类
    - 创建BudgetRepositoryImpl类，实现BudgetRepository接口
    - 集成Room DAO，实现所有数据库操作
    - 添加错误处理和异常转换逻辑
    - 为Repository实现编写集成测试
    - _Requirements: 6.2, 6.4_

- [ ] 4. 实现业务逻辑Use Cases
  - [ ] 4.1 实现金额计算Use Case
    - 创建CalculateRemainingAmountUseCase，计算剩余可支配金额
    - 实现计算逻辑：可支配金额 - 所有支出总和
    - 为计算逻辑编写单元测试，包括边界情况
    - _Requirements: 3.2, 3.5_

  - [ ] 4.2 实现发薪日倒计时Use Case
    - 创建CalculateDaysUntilPaydayUseCase，计算距离发薪日天数
    - 基于30天周期和当前日期进行计算
    - 为日期计算编写单元测试
    - _Requirements: 4.2, 4.3_

  - [ ] 4.3 实现支出管理Use Cases
    - 创建AddExpenseUseCase，处理添加支出的业务逻辑
    - 创建DeleteExpenseUseCase，处理删除支出的业务逻辑
    - 添加输入验证：金额必须为正数，描述不能为空
    - 为支出管理Use Cases编写单元测试
    - _Requirements: 2.2, 2.3, 5.3, 5.4_

- [ ] 5. 实现ViewModel和UI状态管理
  - [ ] 5.1 创建BudgetViewModel基础结构
    - 实现BudgetViewModel类，继承ViewModel
    - 定义BudgetUiState数据类，包含所有UI状态
    - 使用StateFlow管理UI状态，支持响应式更新
    - 为ViewModel基础功能编写单元测试
    - _Requirements: 3.1, 3.4_

  - [ ] 5.2 实现ViewModel业务逻辑
    - 集成所有Use Cases到ViewModel中
    - 实现金额输入、支出添加、支出删除等功能
    - 添加错误处理和用户反馈逻辑
    - 为ViewModel业务逻辑编写单元测试
    - _Requirements: 1.2, 2.2, 2.4, 5.3_

- [ ] 6. 实现主界面UI组件
  - [ ] 6.1 创建MainActivity和基础布局
    - 实现MainActivity，配置Material Design 3主题
    - 创建主布局文件，使用ConstraintLayout作为根布局
    - 配置Fragment容器和导航结构
    - _Requirements: 7.1, 7.2_

  - [ ] 6.2 实现金额显示和输入界面
    - 创建大号数字显示组件，类似计算器显示屏
    - 实现TextInputLayout用于可支配金额输入
    - 添加数字键盘支持和输入验证
    - 实现剩余金额的颜色状态：正数绿色，负数红色
    - _Requirements: 1.1, 1.3, 3.1, 3.3, 7.3_

  - [ ] 6.3 实现发薪日倒计时显示
    - 创建倒计时显示组件，显示剩余天数
    - 实现颜色提醒：少于7天显示橙色
    - 添加发薪日到达时的提示功能
    - _Requirements: 4.1, 4.4_

- [ ] 7. 实现支出管理界面
  - [ ] 7.1 创建支出列表界面
    - 实现RecyclerView显示支出列表
    - 创建支出项目的MaterialCardView布局
    - 显示支出描述、金额和添加日期
    - 实现空列表状态的提示信息
    - _Requirements: 5.1, 5.2, 5.5_

  - [ ] 7.2 实现支出添加功能
    - 创建FloatingActionButton作为添加支出的主要入口
    - 实现支出添加对话框，包含描述和金额输入
    - 添加输入验证和错误提示
    - 实现添加成功后的UI更新
    - _Requirements: 2.1, 2.2, 2.3_

  - [ ] 7.3 实现支出删除功能
    - 添加长按支出项目的删除选项
    - 实现MaterialAlertDialog确认删除操作
    - 添加删除后的UI更新和用户反馈
    - _Requirements: 5.3, 5.4_

- [ ] 8. 实现高级功能和设置
  - [ ] 8.1 实现预算周期重置功能
    - 创建设置菜单，提供重置选项
    - 实现重置确认对话框
    - 清除当前支出记录，提示输入新的可支配金额
    - _Requirements: 8.1, 8.2, 8.3, 8.4_

  - [ ] 8.2 实现超支警告功能
    - 添加超支检测逻辑
    - 实现超支时的警告提示，但仍允许添加支出
    - 在UI中突出显示超支状态
    - _Requirements: 2.5, 3.3_

- [ ] 9. 错误处理和用户体验优化
  - [ ] 9.1 实现全局错误处理
    - 创建统一的错误处理机制
    - 实现用户友好的错误信息显示
    - 添加网络错误和数据库错误的重试机制
    - _Requirements: 6.4_

  - [ ] 9.2 添加加载状态和用户反馈
    - 实现Material Design LoadingIndicator
    - 使用Snackbar显示操作结果反馈
    - 添加按钮点击的视觉反馈效果
    - _Requirements: 7.4, 7.5_

- [ ] 10. 集成测试和端到端测试
  - [ ] 10.1 编写UI集成测试
    - 使用Espresso测试主要用户流程
    - 测试金额输入、支出添加、支出删除的完整流程
    - 验证UI状态变化和数据持久化
    - _Requirements: 1.1-1.5, 2.1-2.5, 5.1-5.5_

  - [ ] 10.2 编写数据库集成测试
    - 测试Room数据库的完整CRUD操作
    - 验证外键约束和数据完整性
    - 测试数据库升级和迁移场景
    - _Requirements: 6.1-6.5_

- [ ] 11. 性能优化和最终调试
  - [ ] 11.1 优化应用性能
    - 分析内存使用和数据库查询性能
    - 优化RecyclerView的滚动性能
    - 实现适当的数据缓存策略
    - _Requirements: 7.1-7.5_

  - [ ] 11.2 最终测试和调试
    - 在不同设备和屏幕尺寸上测试应用
    - 验证所有需求的完整实现
    - 修复发现的bug和用户体验问题
    - _Requirements: 1.1-8.5_
