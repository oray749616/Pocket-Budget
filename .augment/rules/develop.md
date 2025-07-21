---
type: "always_apply"
---

# Android开发标准和规范文档

## 概述

本文档定义了简洁资金管理安卓应用开发过程中的技术标准、编码规范和最佳实践。基于2025年最新的Android开发生态系统和Google官方推荐。

## 技术栈标准

### 核心技术栈
- **开发语言**: Kotlin 2.1.21+ (Android官方推荐语言)
- **架构模式**: MVVM (Model-View-ViewModel) 
- **数据库**: Room Persistence Library (SQLite抽象层)
- **UI框架**: Material Design 3 组件
- **依赖注入**: Hilt (Google推荐的DI框架)
- **异步处理**: Kotlin Coroutines + Flow
- **最低SDK版本**: API 24 (Android 7.0) - 覆盖95%+设备
- **目标SDK版本**: API 35 (Android 15)

### 开发工具标准
- **IDE**: Android Studio Ladybug | 2024.2.1+
- **构建工具**: Gradle 8.9+ with Kotlin DSL
- **版本控制**: Git with conventional commits
- **代码质量**: ktlint + detekt + SonarQube

## Kotlin编码规范

### 命名约定

#### 类和接口命名
```kotlin
// 类名使用PascalCase
class BudgetCalculator
class ExpenseRepository
interface PaymentService

// 抽象类使用Abstract前缀
abstract class AbstractBudgetManager

// 接口实现类使用Impl后缀
class BudgetRepositoryImpl : BudgetRepository
```

#### 函数和变量命名
```kotlin
// 函数名使用camelCase，动词开头
fun calculateRemainingAmount(): Double
fun addExpenseItem(expense: Expense)
suspend fun loadBudgetData(): BudgetData

// 变量名使用camelCase
val disposableAmount: Double
var currentBudgetPeriod: BudgetPeriod?
private val _uiState = MutableStateFlow(BudgetUiState())

// 常量使用SCREAMING_SNAKE_CASE
const val MAX_EXPENSE_DESCRIPTION_LENGTH = 50
const val DEFAULT_BUDGET_PERIOD_DAYS = 30
```

#### 包命名
```kotlin
// 包名使用小写，点分隔
com.budgetapp.data.database
com.budgetapp.domain.usecase
com.budgetapp.presentation.viewmodel
```

### 代码组织规范

#### 类结构顺序
```kotlin
class BudgetViewModel @Inject constructor(
    private val repository: BudgetRepository
) : ViewModel() {
    
    // 1. 伴生对象
    companion object {
        private const val TAG = "BudgetViewModel"
    }
    
    // 2. 属性声明
    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()
    
    // 3. 初始化块
    init {
        loadInitialData()
    }
    
    // 4. 公共方法
    fun addExpense(description: String, amount: Double) {
        // implementation
    }
    
    // 5. 私有方法
    private fun loadInitialData() {
        // implementation
    }
}
```

### Kotlin特性使用规范

#### 空安全
```kotlin
// 优先使用安全调用操作符
val length = text?.length ?: 0

// 使用let进行空检查
expense?.let { validExpense ->
    repository.addExpense(validExpense)
}

// 避免使用!!操作符，除非确定不为null
val definitelyNotNull = getValue()!!  // 仅在确定的情况下使用
```

#### 作用域函数使用
```kotlin
// let: 对非空对象执行操作
expense?.let { 
    addToDatabase(it)
}

// apply: 对象配置
val expense = Expense().apply {
    description = "Groceries"
    amount = 50.0
    date = System.currentTimeMillis()
}

// with: 对象的多个操作
with(budgetPeriod) {
    println("Amount: $disposableAmount")
    println("Days left: ${calculateDaysLeft()}")
}

// run: 执行代码块并返回结果
val result = run {
    val total = calculateTotal()
    val remaining = disposableAmount - total
    remaining
}
```

## MVVM架构规范

### 层次结构
```
presentation/
├── ui/
│   ├── MainActivity.kt
│   ├── fragments/
│   └── adapters/
├── viewmodel/
│   └── BudgetViewModel.kt
└── state/
    └── BudgetUiState.kt

domain/
├── usecase/
│   ├── CalculateRemainingAmountUseCase.kt
│   └── AddExpenseUseCase.kt
├── repository/
│   └── BudgetRepository.kt
└── model/
    └── DomainModels.kt

data/
├── database/
│   ├── BudgetDatabase.kt
│   ├── dao/
│   └── entities/
├── repository/
│   └── BudgetRepositoryImpl.kt
└── di/
    └── DatabaseModule.kt
```

### ViewModel规范
```kotlin
@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val calculateRemainingAmountUseCase: CalculateRemainingAmountUseCase,
    private val addExpenseUseCase: AddExpenseUseCase
) : ViewModel() {
    
    // 使用StateFlow管理UI状态
    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()
    
    // 使用SharedFlow处理一次性事件
    private val _events = MutableSharedFlow<BudgetEvent>()
    val events: SharedFlow<BudgetEvent> = _events.asSharedFlow()
    
    fun addExpense(description: String, amount: Double) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                addExpenseUseCase(description, amount)
                    .onSuccess { 
                        _events.emit(BudgetEvent.ExpenseAdded)
                        loadBudgetData()
                    }
                    .onFailure { error ->
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message
                            )
                        }
                    }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }
}
```

## Room数据库规范

### 实体定义
```kotlin
@Entity(
    tableName = "budget_periods",
    indices = [Index(value = ["created_date"])]
)
data class BudgetPeriod(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "disposable_amount")
    val disposableAmount: Double,
    
    @ColumnInfo(name = "created_date")
    val createdDate: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "payday_date") 
    val paydayDate: Long,
    
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true
) {
    // 验证方法
    fun isValid(): Boolean {
        return disposableAmount >= 0 && paydayDate > createdDate
    }
}
```

### DAO规范
```kotlin
@Dao
interface BudgetDao {
    
    // 查询方法使用Flow返回响应式数据
    @Query("SELECT * FROM budget_periods WHERE is_active = 1 ORDER BY created_date DESC LIMIT 1")
    fun getCurrentBudgetPeriod(): Flow<BudgetPeriod?>
    
    @Query("SELECT * FROM expenses WHERE budget_period_id = :periodId ORDER BY created_date DESC")
    fun getExpensesForPeriod(periodId: Long): Flow<List<Expense>>
    
    // 插入方法返回ID
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetPeriod(period: BudgetPeriod): Long
    
    @Insert
    suspend fun insertExpense(expense: Expense): Long
    
    // 更新和删除方法
    @Update
    suspend fun updateBudgetPeriod(period: BudgetPeriod): Int
    
    @Delete
    suspend fun deleteExpense(expense: Expense): Int
    
    // 事务方法
    @Transaction
    suspend fun resetBudgetPeriod(newPeriod: BudgetPeriod) {
        // 停用当前周期
        getCurrentBudgetPeriodSync()?.let { current ->
            updateBudgetPeriod(current.copy(isActive = false))
        }
        // 插入新周期
        insertBudgetPeriod(newPeriod)
    }
    
    @Query("SELECT * FROM budget_periods WHERE is_active = 1 LIMIT 1")
    suspend fun getCurrentBudgetPeriodSync(): BudgetPeriod?
}
```

### 数据库配置
```kotlin
@Database(
    entities = [BudgetPeriod::class, Expense::class],
    version = 1,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
    ]
)
@TypeConverters(Converters::class)
abstract class BudgetDatabase : RoomDatabase() {
    
    abstract fun budgetDao(): BudgetDao
    
    companion object {
        const val DATABASE_NAME = "budget_database"
    }
}

// Hilt模块
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BudgetDatabase {
        return Room.databaseBuilder(
            context,
            BudgetDatabase::class.java,
            BudgetDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration() // 仅开发阶段使用
        .build()
    }
    
    @Provides
    fun provideBudgetDao(database: BudgetDatabase): BudgetDao {
        return database.budgetDao()
    }
}
```

## Kotlin Coroutines规范

### 协程作用域使用
```kotlin
class BudgetRepository @Inject constructor(
    private val dao: BudgetDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    
    // 使用适当的调度器
    suspend fun addExpense(expense: Expense): Result<Unit> = withContext(ioDispatcher) {
        try {
            dao.insertExpense(expense)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Flow操作
    fun getBudgetData(): Flow<BudgetData> = flow {
        val period = dao.getCurrentBudgetPeriod().first()
        val expenses = period?.let { dao.getExpensesForPeriod(it.id).first() } ?: emptyList()
        emit(BudgetData(period, expenses))
    }.flowOn(ioDispatcher)
}
```

### 错误处理
```kotlin
// 使用Result包装返回值
sealed class BudgetResult<out T> {
    data class Success<T>(val data: T) : BudgetResult<T>()
    data class Error(val exception: Throwable) : BudgetResult<Nothing>()
    object Loading : BudgetResult<Nothing>()
}

// 扩展函数简化Result处理
inline fun <T> BudgetResult<T>.onSuccess(action: (T) -> Unit): BudgetResult<T> {
    if (this is BudgetResult.Success) action(data)
    return this
}

inline fun <T> BudgetResult<T>.onError(action: (Throwable) -> Unit): BudgetResult<T> {
    if (this is BudgetResult.Error) action(exception)
    return this
}
```

## Material Design 3 UI规范

### 主题配置
```kotlin
// themes.xml
<style name="Theme.BudgetApp" parent="Theme.Material3.DayNight">
    <item name="colorPrimary">@color/md_theme_primary</item>
    <item name="colorOnPrimary">@color/md_theme_on_primary</item>
    <item name="colorSecondary">@color/md_theme_secondary</item>
    <item name="colorSurface">@color/md_theme_surface</item>
    <item name="android:windowSplashScreenBackground">@color/md_theme_surface</item>
</style>
```

### 组件使用规范
```xml
<!-- 金额输入使用OutlinedBox样式 -->
<com.google.android.material.textfield.TextInputLayout
    android:id="@+id/amountInputLayout"
    style="@style/Widget.Material3.TextInputLayout.OutlinedBox"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:hint="@string/disposable_amount"
    app:helperText="@string/amount_helper_text"
    app:errorEnabled="true">

    <com.google.android.material.textfield.TextInputEditText
        android:id="@+id/amountEditText"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:inputType="numberDecimal"
        android:maxLength="10" />
</com.google.android.material.textfield.TextInputLayout>

<!-- 操作按钮使用Material3样式 -->
<com.google.android.material.button.MaterialButton
    android:id="@+id/addExpenseButton"
    style="@style/Widget.Material3.Button"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="@string/add_expense"
    app:icon="@drawable/ic_add_24"
    app:iconGravity="start" />

<!-- 支出列表项使用CardView -->
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="8dp"
    app:cardElevation="2dp"
    app:cardCornerRadius="12dp"
    style="@style/Widget.Material3.CardView.Elevated">
    
    <!-- 卡片内容 -->
    
</com.google.android.material.card.MaterialCardView>
```

## 测试规范

### 单元测试
```kotlin
@ExtendWith(MockKExtension::class)
class BudgetViewModelTest {
    
    @MockK
    private lateinit var calculateRemainingAmountUseCase: CalculateRemainingAmountUseCase
    
    @MockK
    private lateinit var addExpenseUseCase: AddExpenseUseCase
    
    private lateinit var viewModel: BudgetViewModel
    
    @BeforeEach
    fun setup() {
        viewModel = BudgetViewModel(
            calculateRemainingAmountUseCase,
            addExpenseUseCase
        )
    }
    
    @Test
    fun `addExpense should update UI state correctly`() = runTest {
        // Given
        val description = "Test expense"
        val amount = 100.0
        coEvery { addExpenseUseCase(description, amount) } returns Result.success(Unit)
        
        // When
        viewModel.addExpense(description, amount)
        
        // Then
        coVerify { addExpenseUseCase(description, amount) }
        // 验证UI状态更新
    }
}
```

### 集成测试
```kotlin
@HiltAndroidTest
class BudgetRepositoryTest {
    
    @get:Rule
    val hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var database: BudgetDatabase
    
    @Inject
    lateinit var repository: BudgetRepository
    
    @Before
    fun setup() {
        hiltRule.inject()
    }
    
    @Test
    fun testAddExpenseIntegration() = runTest {
        // 集成测试逻辑
    }
}
```

## 代码质量标准

### 静态分析配置
```kotlin
// build.gradle.kts
plugins {
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
    id("io.gitlab.arturbosch.detekt") version "1.23.4"
}

ktlint {
    version.set("0.50.0")
    android.set(true)
    outputColorName.set("RED")
}

detekt {
    config = files("$projectDir/config/detekt/detekt.yml")
    buildUponDefaultConfig = true
}
```

### 代码覆盖率要求
- 单元测试覆盖率: ≥ 80%
- 集成测试覆盖率: ≥ 60%
- UI测试覆盖率: ≥ 40%

## 性能优化标准

### 内存管理
- 避免内存泄漏：正确使用ViewModel和LiveData/StateFlow
- 图片优化：使用适当的图片格式和尺寸
- 列表优化：RecyclerView使用ViewHolder模式和DiffUtil

### 网络优化
- 使用OkHttp连接池
- 实现请求缓存策略
- 网络错误重试机制

### 数据库优化
- 合理使用索引
- 避免主线程数据库操作
- 使用分页加载大数据集

## 安全标准

### 数据保护
- 敏感数据加密存储
- 使用Android Keystore
- 防止SQL注入（Room自动处理）

### 代码混淆
```kotlin
// proguard-rules.pro
-keep class com.budgetapp.data.database.entities.** { *; }
-keep class com.budgetapp.domain.model.** { *; }
```

## 发布标准

### 版本管理
- 使用语义化版本控制 (Semantic Versioning)
- Git标签标记发布版本
- 维护CHANGELOG.md

### 构建配置
```kotlin
// build.gradle.kts
android {
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

## 文档标准

### 代码注释
```kotlin
/**
 * 计算剩余可支配金额
 * 
 * @param budgetPeriod 当前预算周期
 * @param expenses 支出列表
 * @return 剩余金额，如果为负数表示超支
 */
fun calculateRemainingAmount(
    budgetPeriod: BudgetPeriod,
    expenses: List<Expense>
): Double {
    return budgetPeriod.disposableAmount - expenses.sumOf { it.amount }
}
```

### README要求
- 项目描述和功能
- 安装和运行说明
- 架构说明
- 贡献指南
- 许可证信息
