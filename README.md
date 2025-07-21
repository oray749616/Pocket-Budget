# 简洁资金管理安卓应用

一个基于Android平台的简洁资金管理应用，帮助用户管理日常支出和预算。

## 技术栈

- **开发语言**: Kotlin 2.1.21
- **最低SDK版本**: API 24 (Android 7.0)
- **目标SDK版本**: API 35 (Android 15)
- **架构模式**: MVVM (Model-View-ViewModel)
- **数据库**: Room Persistence Library
- **UI框架**: Material Design 3
- **依赖注入**: Hilt
- **异步处理**: Kotlin Coroutines + Flow

## 项目结构

```
app/
├── src/main/java/com/budgetapp/
│   ├── BudgetApplication.kt          # Application类，Hilt入口点
│   ├── data/                         # 数据层
│   │   ├── database/                 # Room数据库相关
│   │   ├── repository/               # Repository实现
│   │   └── di/                       # 依赖注入模块
│   ├── domain/                       # 业务逻辑层
│   │   ├── usecase/                  # Use Case业务用例
│   │   ├── repository/               # Repository接口
│   │   └── model/                    # 领域模型
│   └── presentation/                 # 表现层
│       ├── ui/                       # UI组件 (Activity, Fragment)
│       ├── viewmodel/                # ViewModel
│       └── state/                    # UI状态管理
└── src/main/res/                     # 资源文件
    ├── layout/                       # 布局文件
    ├── values/                       # 字符串、颜色、主题
    └── xml/                          # 配置文件
```

## 核心功能

1. **可支配金额管理**: 设置和显示当前预算周期的可支配金额
2. **支出记录**: 添加、查看和删除日常支出记录
3. **剩余金额计算**: 实时计算和显示剩余可用金额
4. **发薪日倒计时**: 显示距离下次发薪日的天数
5. **超支警告**: 当支出超过预算时提供警告提示

## 开发规范

项目遵循以下开发标准：

- **代码规范**: 遵循Kotlin官方编码规范
- **架构模式**: 严格按照MVVM架构分层
- **依赖注入**: 使用Hilt进行依赖管理
- **数据库**: 使用Room进行数据持久化
- **UI设计**: 遵循Material Design 3设计规范
- **测试**: 包含单元测试、集成测试和UI测试

## 构建和运行

1. 确保安装了Android Studio Ladybug | 2024.2.1+
2. 克隆项目到本地
3. 使用Android Studio打开项目
4. 等待Gradle同步完成
5. 连接Android设备或启动模拟器
6. 点击运行按钮构建和安装应用

## 开发状态

- [x] 任务1: 项目初始化和基础架构设置
- [ ] 任务2: 定义数据模型和Room数据库结构
- [ ] 任务3: 实现Repository层和数据访问逻辑
- [ ] 任务4: 实现业务逻辑Use Cases
- [ ] 任务5: 实现ViewModel和UI状态管理
- [ ] 任务6: 实现主界面UI组件
- [ ] 任务7: 实现支出管理界面
- [ ] 任务8: 实现高级功能和设置
- [ ] 任务9: 错误处理和用户体验优化
- [ ] 任务10: 集成测试和端到端测试
- [ ] 任务11: 性能优化和最终调试

## 许可证

本项目仅用于学习和演示目的。
