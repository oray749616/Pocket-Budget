package com.budgetapp.presentation.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.budgetapp.data.database.entities.BudgetPeriod
import com.budgetapp.data.database.entities.Expense
import com.budgetapp.domain.model.Result
import com.budgetapp.domain.repository.BudgetRepository
import com.budgetapp.domain.usecase.AddExpenseUseCase
import com.budgetapp.domain.usecase.CalculateDaysUntilPaydayUseCase
import com.budgetapp.domain.usecase.CalculateRemainingAmountUseCase
import com.budgetapp.domain.usecase.DeleteExpenseUseCase
import com.budgetapp.presentation.state.BudgetEvent
import com.budgetapp.presentation.state.BudgetUiState
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * BudgetViewModelTest
 * 
 * BudgetViewModel的单元测试类，测试ViewModel的基础功能和业务逻辑。
 * 使用MockK进行依赖模拟，使用Kotlin Coroutines Test进行异步测试。
 * 
 * Requirements:
 * - 3.1: 测试剩余可支配金额的管理
 * - 3.4: 测试响应式UI更新
 * - 1.2: 测试可支配金额输入处理
 * - 2.2: 测试支出添加功能
 * - 2.4: 测试支出删除功能
 * - 5.3: 测试支出列表管理
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModelTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    // Mock dependencies
    private val mockBudgetRepository = mockk<BudgetRepository>()
    private val mockCalculateRemainingAmountUseCase = mockk<CalculateRemainingAmountUseCase>()
    private val mockCalculateDaysUntilPaydayUseCase = mockk<CalculateDaysUntilPaydayUseCase>()
    private val mockAddExpenseUseCase = mockk<AddExpenseUseCase>()
    private val mockDeleteExpenseUseCase = mockk<DeleteExpenseUseCase>()
    
    private lateinit var viewModel: BudgetViewModel
    private val testDispatcher = StandardTestDispatcher()
    
    // Test data
    private val testBudgetPeriod = BudgetPeriod(
        id = 1L,
        disposableAmount = 1000.0,
        createdDate = System.currentTimeMillis(),
        paydayDate = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000L),
        isActive = true
    )
    
    private val testExpenses = listOf(
        Expense(
            id = 1L,
            budgetPeriodId = 1L,
            description = "Groceries",
            amount = 100.0,
            createdDate = System.currentTimeMillis()
        ),
        Expense(
            id = 2L,
            budgetPeriodId = 1L,
            description = "Gas",
            amount = 50.0,
            createdDate = System.currentTimeMillis()
        )
    )
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Setup default mock behaviors
        every { mockBudgetRepository.getCurrentBudgetPeriod() } returns flowOf(testBudgetPeriod)
        every { mockBudgetRepository.getExpensesForCurrentPeriod() } returns flowOf(testExpenses)
        every { mockCalculateRemainingAmountUseCase() } returns flowOf(850.0) // 1000 - 150
        every { mockCalculateDaysUntilPaydayUseCase() } returns flowOf(15)
        
        viewModel = BudgetViewModel(
            budgetRepository = mockBudgetRepository,
            calculateRemainingAmountUseCase = mockCalculateRemainingAmountUseCase,
            calculateDaysUntilPaydayUseCase = mockCalculateDaysUntilPaydayUseCase,
            addExpenseUseCase = mockAddExpenseUseCase,
            deleteExpenseUseCase = mockDeleteExpenseUseCase
        )
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }
    
    // ==================== 初始化测试 ====================
    
    @Test
    fun `初始化时应该加载数据并设置正确的初始状态`() = runTest {
        // When - ViewModel初始化时会自动加载数据
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val uiState = viewModel.uiState.value
        assertEquals(testBudgetPeriod, uiState.currentBudgetPeriod)
        assertEquals(testExpenses, uiState.expenses)
        assertEquals(850.0, uiState.remainingAmount)
        assertEquals(15, uiState.daysUntilPayday)
        assertEquals(150.0, uiState.totalExpenses)
        assertFalse(uiState.isOverBudget)
        assertTrue(uiState.hasActiveBudgetPeriod)
        assertTrue(uiState.hasExpenses)
        assertFalse(uiState.isLoading)
    }
    
    @Test
    fun `当没有预算周期时应该显示空状态`() = runTest {
        // Given
        every { mockBudgetRepository.getCurrentBudgetPeriod() } returns flowOf(null)
        every { mockBudgetRepository.getExpensesForCurrentPeriod() } returns flowOf(emptyList())
        every { mockCalculateRemainingAmountUseCase() } returns flowOf(0.0)
        
        // When
        val newViewModel = BudgetViewModel(
            budgetRepository = mockBudgetRepository,
            calculateRemainingAmountUseCase = mockCalculateRemainingAmountUseCase,
            calculateDaysUntilPaydayUseCase = mockCalculateDaysUntilPaydayUseCase,
            addExpenseUseCase = mockAddExpenseUseCase,
            deleteExpenseUseCase = mockDeleteExpenseUseCase
        )
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val uiState = newViewModel.uiState.value
        assertEquals(null, uiState.currentBudgetPeriod)
        assertTrue(uiState.expenses.isEmpty())
        assertFalse(uiState.hasActiveBudgetPeriod)
        assertFalse(uiState.hasExpenses)
        assertTrue(uiState.showEmptyState)
    }
    
    // ==================== 输入验证测试 ====================
    
    @Test
    fun `更新支出描述时应该正确验证输入`() = runTest {
        // When - 输入有效描述
        viewModel.updateExpenseDescription("Valid description")
        
        // Then
        val uiState = viewModel.uiState.value
        assertEquals("Valid description", uiState.expenseDescription)
        assertFalse(uiState.inputErrors.containsKey("description"))
        
        // When - 输入空描述
        viewModel.updateExpenseDescription("")
        
        // Then
        val uiState2 = viewModel.uiState.value
        assertEquals("", uiState2.expenseDescription)
        assertTrue(uiState2.inputErrors.containsKey("description"))
        assertEquals("支出描述不能为空", uiState2.inputErrors["description"])
        
        // When - 输入过长描述
        viewModel.updateExpenseDescription("a".repeat(51))
        
        // Then
        val uiState3 = viewModel.uiState.value
        assertTrue(uiState3.inputErrors.containsKey("description"))
        assertEquals("支出描述不能超过50个字符", uiState3.inputErrors["description"])
    }
    
    @Test
    fun `更新支出金额时应该正确验证输入`() = runTest {
        // When - 输入有效金额
        viewModel.updateExpenseAmount("100.50")
        
        // Then
        val uiState = viewModel.uiState.value
        assertEquals("100.50", uiState.expenseAmount)
        assertFalse(uiState.inputErrors.containsKey("amount"))
        
        // When - 输入空金额
        viewModel.updateExpenseAmount("")
        
        // Then
        val uiState2 = viewModel.uiState.value
        assertTrue(uiState2.inputErrors.containsKey("amount"))
        assertEquals("支出金额不能为空", uiState2.inputErrors["amount"])
        
        // When - 输入负数
        viewModel.updateExpenseAmount("-10")
        
        // Then
        val uiState3 = viewModel.uiState.value
        assertTrue(uiState3.inputErrors.containsKey("amount"))
        assertEquals("支出金额必须大于0", uiState3.inputErrors["amount"])
        
        // When - 输入无效格式
        viewModel.updateExpenseAmount("abc")
        
        // Then
        val uiState4 = viewModel.uiState.value
        assertTrue(uiState4.inputErrors.containsKey("amount"))
        assertEquals("请输入有效的金额", uiState4.inputErrors["amount"])
    }
    
    @Test
    fun `更新可支配金额时应该正确验证输入`() = runTest {
        // When - 输入有效金额
        viewModel.updateDisposableAmount("1000.00")
        
        // Then
        val uiState = viewModel.uiState.value
        assertEquals("1000.00", uiState.disposableAmountInput)
        assertFalse(uiState.inputErrors.containsKey("disposableAmount"))
        
        // When - 输入负数
        viewModel.updateDisposableAmount("-100")
        
        // Then
        val uiState2 = viewModel.uiState.value
        assertTrue(uiState2.inputErrors.containsKey("disposableAmount"))
        assertEquals("可支配金额不能为负数", uiState2.inputErrors["disposableAmount"])
    }
    
    // ==================== 对话框状态测试 ====================
    
    @Test
    fun `显示和隐藏添加支出对话框应该正确更新状态`() = runTest {
        // When - 显示对话框
        viewModel.showAddExpenseDialog()
        
        // Then
        val uiState = viewModel.uiState.value
        assertTrue(uiState.showAddExpenseDialog)
        assertEquals("", uiState.expenseDescription)
        assertEquals("", uiState.expenseAmount)
        assertTrue(uiState.inputErrors.isEmpty())
        
        // When - 隐藏对话框
        viewModel.hideAddExpenseDialog()
        
        // Then
        val uiState2 = viewModel.uiState.value
        assertFalse(uiState2.showAddExpenseDialog)
    }
    
    @Test
    fun `显示和隐藏删除确认对话框应该正确更新状态`() = runTest {
        // When - 显示对话框
        viewModel.showDeleteConfirmDialog(1L)
        
        // Then
        val uiState = viewModel.uiState.value
        assertTrue(uiState.showDeleteConfirmDialog)
        assertEquals(1L, uiState.expenseToDelete)
        
        // When - 隐藏对话框
        viewModel.hideDeleteConfirmDialog()
        
        // Then
        val uiState2 = viewModel.uiState.value
        assertFalse(uiState2.showDeleteConfirmDialog)
        assertEquals(null, uiState2.expenseToDelete)
    }
    
    // ==================== 错误处理测试 ====================

    @Test
    fun `清除错误消息应该正确更新状态`() = runTest {
        // Given - 设置错误状态
        every { mockBudgetRepository.getCurrentBudgetPeriod() } returns flow {
            throw Exception("Test error")
        }

        val errorViewModel = BudgetViewModel(
            budgetRepository = mockBudgetRepository,
            calculateRemainingAmountUseCase = mockCalculateRemainingAmountUseCase,
            calculateDaysUntilPaydayUseCase = mockCalculateDaysUntilPaydayUseCase,
            addExpenseUseCase = mockAddExpenseUseCase,
            deleteExpenseUseCase = mockDeleteExpenseUseCase
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // When - 清除错误
        errorViewModel.clearError()

        // Then
        val uiState = errorViewModel.uiState.value
        assertEquals(null, uiState.errorMessage)
    }

    // ==================== 业务逻辑测试 ====================

    @Test
    fun `创建预算周期成功时应该正确更新状态和发送事件`() = runTest {
        // Given
        coEvery { mockBudgetRepository.createNewBudgetPeriod(1000.0) } returns Result.Success(1L)

        val events = mutableListOf<BudgetEvent>()
        val eventJob = launch {
            viewModel.events.collect { events.add(it) }
        }

        // When
        viewModel.createBudgetPeriod(1000.0)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertFalse(uiState.showResetBudgetDialog)
        assertEquals("", uiState.disposableAmountInput)
        assertEquals("预算周期创建成功", uiState.successMessage)

        // 验证事件
        assertTrue(events.any { it is BudgetEvent.BudgetPeriodReset })
        assertTrue(events.any { it is BudgetEvent.ShowSuccess })

        coVerify { mockBudgetRepository.createNewBudgetPeriod(1000.0) }

        eventJob.cancel()
    }

    @Test
    fun `创建预算周期失败时应该显示错误信息`() = runTest {
        // Given
        coEvery { mockBudgetRepository.createNewBudgetPeriod(1000.0) } returns
            Result.Error(Exception("创建失败"))

        val events = mutableListOf<BudgetEvent>()
        val eventJob = launch {
            viewModel.events.collect { events.add(it) }
        }

        // When
        viewModel.createBudgetPeriod(1000.0)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertEquals("创建失败", uiState.errorMessage)

        // 验证错误事件
        assertTrue(events.any { it is BudgetEvent.ShowError })

        eventJob.cancel()
    }

    @Test
    fun `添加支出成功时应该正确更新状态和发送事件`() = runTest {
        // Given
        val addResult = AddExpenseUseCase.AddExpenseResult(
            expenseId = 3L,
            willOverspend = false,
            overspendAmount = 0.0,
            remainingAmountAfter = 750.0
        )
        coEvery { mockAddExpenseUseCase.executeWithOverspendingCheck("Coffee", 100.0) } returns
            Result.Success(addResult)

        val events = mutableListOf<BudgetEvent>()
        val eventJob = launch {
            viewModel.events.collect { events.add(it) }
        }

        // When
        viewModel.addExpense("Coffee", 100.0)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertFalse(uiState.showAddExpenseDialog)
        assertEquals("", uiState.expenseDescription)
        assertEquals("", uiState.expenseAmount)
        assertTrue(uiState.inputErrors.isEmpty())
        assertEquals("支出添加成功", uiState.successMessage)

        // 验证事件
        assertTrue(events.any { it is BudgetEvent.ExpenseAdded })
        assertTrue(events.any { it is BudgetEvent.ShowSuccess })

        coVerify { mockAddExpenseUseCase.executeWithOverspendingCheck("Coffee", 100.0) }

        eventJob.cancel()
    }

    @Test
    fun `添加支出导致超支时应该显示超支警告`() = runTest {
        // Given
        val addResult = AddExpenseUseCase.AddExpenseResult(
            expenseId = 3L,
            willOverspend = true,
            overspendAmount = 50.0,
            remainingAmountAfter = -50.0
        )
        coEvery { mockAddExpenseUseCase.executeWithOverspendingCheck("Expensive item", 900.0) } returns
            Result.Success(addResult)

        val events = mutableListOf<BudgetEvent>()
        val eventJob = launch {
            viewModel.events.collect { events.add(it) }
        }

        // When
        viewModel.addExpense("Expensive item", 900.0)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertEquals("支出添加成功", uiState.successMessage)

        // 验证超支警告事件
        assertTrue(events.any { it is BudgetEvent.ShowOverspendWarning })
        val warningEvent = events.find { it is BudgetEvent.ShowOverspendWarning } as BudgetEvent.ShowOverspendWarning
        assertEquals(50.0, warningEvent.overspendAmount)

        eventJob.cancel()
    }

    @Test
    fun `删除支出成功时应该正确更新状态和发送事件`() = runTest {
        // Given
        val deleteResult = DeleteExpenseUseCase.DeleteExpenseResult(
            deletedExpense = testExpenses[0],
            affectedRows = 1,
            wasSuccessful = true
        )
        coEvery { mockDeleteExpenseUseCase.executeById(1L) } returns Result.Success(deleteResult)

        val events = mutableListOf<BudgetEvent>()
        val eventJob = launch {
            viewModel.events.collect { events.add(it) }
        }

        // When
        viewModel.deleteExpense(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertFalse(uiState.showDeleteConfirmDialog)
        assertEquals(null, uiState.expenseToDelete)
        assertEquals("支出删除成功", uiState.successMessage)

        // 验证事件
        assertTrue(events.any { it is BudgetEvent.ExpenseDeleted })
        assertTrue(events.any { it is BudgetEvent.ShowSuccess })

        val deletedEvent = events.find { it is BudgetEvent.ExpenseDeleted } as BudgetEvent.ExpenseDeleted
        assertEquals("Groceries", deletedEvent.expenseDescription)

        coVerify { mockDeleteExpenseUseCase.executeById(1L) }

        eventJob.cancel()
    }
}
