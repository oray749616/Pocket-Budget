package com.budgetapp.domain.usecase

import com.budgetapp.data.database.entities.BudgetPeriod
import com.budgetapp.domain.model.Result
import com.budgetapp.domain.repository.BudgetRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension

/**
 * AddExpenseUseCase单元测试
 * 
 * 测试添加支出Use Case的各种场景，包括输入验证和边界情况。
 */
@ExtendWith(MockitoExtension::class)
class AddExpenseUseCaseTest {
    
    private lateinit var repository: BudgetRepository
    private lateinit var useCase: AddExpenseUseCase
    
    @BeforeEach
    fun setup() {
        repository = mockk()
        useCase = AddExpenseUseCase(repository)
    }
    
    @Test
    fun `execute should add expense successfully with valid input`() = runTest {
        // Given
        val description = "Groceries"
        val amount = 100.0
        val budgetPeriodId = 1L
        val expectedExpenseId = 5L
        
        coEvery { repository.validateExpenseData(description, amount) } returns Result.Success(Unit)
        coEvery { repository.addExpense(description.trim(), amount, budgetPeriodId) } returns Result.Success(expectedExpenseId)
        
        // When
        val result = useCase.execute(description, amount, budgetPeriodId)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedExpenseId, result.getOrNull())
    }
    
    @Test
    fun `execute should return error when validation fails`() = runTest {
        // Given
        val description = ""
        val amount = -50.0
        val budgetPeriodId = 1L
        val validationError = Exception("Invalid input")
        
        coEvery { repository.validateExpenseData(description, amount) } returns Result.Error(validationError)
        
        // When
        val result = useCase.execute(description, amount, budgetPeriodId)
        
        // Then
        assertTrue(result.isError)
        assertEquals(validationError, result.exceptionOrNull())
    }
    
    @Test
    fun `execute should check for active budget period when budgetPeriodId is null`() = runTest {
        // Given
        val description = "Coffee"
        val amount = 5.0
        val expectedExpenseId = 3L
        
        coEvery { repository.validateExpenseData(description, amount) } returns Result.Success(Unit)
        coEvery { repository.hasActiveBudgetPeriod() } returns Result.Success(true)
        coEvery { repository.addExpense(description.trim(), amount, null) } returns Result.Success(expectedExpenseId)
        
        // When
        val result = useCase.execute(description, amount, null)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedExpenseId, result.getOrNull())
    }
    
    @Test
    fun `execute should return error when no active budget period exists`() = runTest {
        // Given
        val description = "Coffee"
        val amount = 5.0
        
        coEvery { repository.validateExpenseData(description, amount) } returns Result.Success(Unit)
        coEvery { repository.hasActiveBudgetPeriod() } returns Result.Success(false)
        
        // When
        val result = useCase.execute(description, amount, null)
        
        // Then
        assertTrue(result.isError)
        assertEquals("没有活跃的预算周期，请先创建预算周期", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `executeWithOverspendingCheck should detect overspending`() = runTest {
        // Given
        val description = "Expensive item"
        val amount = 500.0
        val budgetPeriodId = 1L
        val remainingAmount = 300.0
        val expectedExpenseId = 7L
        
        val period = BudgetPeriod(
            id = budgetPeriodId,
            disposableAmount = 1000.0,
            paydayDate = System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L
        )
        
        coEvery { repository.validateExpenseData(description, amount) } returns Result.Success(Unit)
        coEvery { repository.calculateRemainingAmount(budgetPeriodId) } returns Result.Success(remainingAmount)
        coEvery { repository.addExpense(description.trim(), amount, budgetPeriodId) } returns Result.Success(expectedExpenseId)
        
        // When
        val result = useCase.executeWithOverspendingCheck(description, amount, budgetPeriodId)
        
        // Then
        assertTrue(result.isSuccess)
        val addResult = result.getOrNull()!!
        assertEquals(expectedExpenseId, addResult.expenseId)
        assertTrue(addResult.willOverspend)
        assertEquals(200.0, addResult.overspendAmount, 0.01)
        assertEquals(-200.0, addResult.remainingAmountAfter, 0.01)
    }
    
    @Test
    fun `executeWithOverspendingCheck should not detect overspending when amount is within budget`() = runTest {
        // Given
        val description = "Small purchase"
        val amount = 50.0
        val budgetPeriodId = 1L
        val remainingAmount = 300.0
        val expectedExpenseId = 8L
        
        coEvery { repository.validateExpenseData(description, amount) } returns Result.Success(Unit)
        coEvery { repository.calculateRemainingAmount(budgetPeriodId) } returns Result.Success(remainingAmount)
        coEvery { repository.addExpense(description.trim(), amount, budgetPeriodId) } returns Result.Success(expectedExpenseId)
        
        // When
        val result = useCase.executeWithOverspendingCheck(description, amount, budgetPeriodId)
        
        // Then
        assertTrue(result.isSuccess)
        val addResult = result.getOrNull()!!
        assertEquals(expectedExpenseId, addResult.expenseId)
        assertFalse(addResult.willOverspend)
        assertEquals(0.0, addResult.overspendAmount)
        assertEquals(250.0, addResult.remainingAmountAfter, 0.01)
    }
    
    @Test
    fun `executeBatch should add multiple expenses successfully`() = runTest {
        // Given
        val expenses = listOf(
            AddExpenseUseCase.ExpenseInput("Groceries", 100.0),
            AddExpenseUseCase.ExpenseInput("Gas", 50.0),
            AddExpenseUseCase.ExpenseInput("Coffee", 15.0)
        )
        val budgetPeriodId = 1L
        val expectedIds = listOf(10L, 11L, 12L)
        
        // Mock validation for all expenses
        expenses.forEach { expense ->
            coEvery { repository.validateExpenseData(expense.description, expense.amount) } returns Result.Success(Unit)
        }
        
        coEvery { repository.hasActiveBudgetPeriod() } returns Result.Success(true)
        
        // Mock adding each expense
        coEvery { repository.addExpense("Groceries", 100.0, budgetPeriodId) } returns Result.Success(10L)
        coEvery { repository.addExpense("Gas", 50.0, budgetPeriodId) } returns Result.Success(11L)
        coEvery { repository.addExpense("Coffee", 15.0, budgetPeriodId) } returns Result.Success(12L)
        
        // When
        val result = useCase.executeBatch(expenses, budgetPeriodId)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedIds, result.getOrNull())
    }
    
    @Test
    fun `executeBatch should return error when expense list is empty`() = runTest {
        // Given
        val expenses = emptyList<AddExpenseUseCase.ExpenseInput>()
        val budgetPeriodId = 1L
        
        // When
        val result = useCase.executeBatch(expenses, budgetPeriodId)
        
        // Then
        assertTrue(result.isError)
        assertEquals("支出列表不能为空", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `executeBatch should return error when one expense validation fails`() = runTest {
        // Given
        val expenses = listOf(
            AddExpenseUseCase.ExpenseInput("Valid expense", 100.0),
            AddExpenseUseCase.ExpenseInput("", -50.0) // Invalid expense
        )
        val budgetPeriodId = 1L
        
        coEvery { repository.validateExpenseData("Valid expense", 100.0) } returns Result.Success(Unit)
        coEvery { repository.validateExpenseData("", -50.0) } returns Result.Error(Exception("Invalid amount"))
        
        // When
        val result = useCase.executeBatch(expenses, budgetPeriodId)
        
        // Then
        assertTrue(result.isError)
        assertTrue(result.exceptionOrNull()?.message?.contains("验证失败") == true)
    }
    
    @Test
    fun `AddExpenseResult should generate correct overspend warning`() {
        // Given
        val resultWithOverspend = AddExpenseUseCase.AddExpenseResult(
            expenseId = 1L,
            willOverspend = true,
            overspendAmount = 150.0,
            remainingAmountAfter = -150.0
        )
        
        val resultWithoutOverspend = AddExpenseUseCase.AddExpenseResult(
            expenseId = 2L,
            willOverspend = false,
            overspendAmount = 0.0,
            remainingAmountAfter = 200.0
        )
        
        // When & Then
        assertNotNull(resultWithOverspend.getOverspendWarning())
        assertTrue(resultWithOverspend.getOverspendWarning()!!.contains("150.00"))
        assertNull(resultWithoutOverspend.getOverspendWarning())
    }
    
    @Test
    fun `AddExpenseResult should detect near overspending`() {
        // Given
        val nearOverspendResult = AddExpenseUseCase.AddExpenseResult(
            expenseId = 1L,
            willOverspend = false,
            overspendAmount = 0.0,
            remainingAmountAfter = 50.0 // Less than 100
        )
        
        val safeResult = AddExpenseUseCase.AddExpenseResult(
            expenseId = 2L,
            willOverspend = false,
            overspendAmount = 0.0,
            remainingAmountAfter = 500.0
        )
        
        // When & Then
        assertTrue(nearOverspendResult.isNearOverspending)
        assertFalse(safeResult.isNearOverspending)
    }
}
