package com.budgetapp.domain.usecase

import com.budgetapp.data.database.entities.BudgetPeriod
import com.budgetapp.data.database.entities.Expense
import com.budgetapp.domain.model.Result
import com.budgetapp.domain.repository.BudgetRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * CalculateRemainingAmountUseCase单元测试
 * 
 * 测试金额计算Use Case的各种场景，包括边界情况。
 */
class CalculateRemainingAmountUseCaseTest {

    private lateinit var repository: BudgetRepository
    private lateinit var useCase: CalculateRemainingAmountUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = CalculateRemainingAmountUseCase(repository)
    }
    
    @Test
    fun `calculateForPeriod should return correct remaining amount when period exists`() = runTest {
        // Given
        val periodId = 1L
        val disposableAmount = 1000.0
        val totalExpenses = 300.0
        val expectedRemaining = 700.0
        
        val period = BudgetPeriod(
            id = periodId,
            disposableAmount = disposableAmount,
            paydayDate = System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L
        )
        
        coEvery { repository.getBudgetPeriodById(periodId) } returns Result.Success(period)
        coEvery { repository.getTotalExpensesForPeriod(periodId) } returns Result.Success(totalExpenses)
        
        // When
        val result = useCase.calculateForPeriod(periodId)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedRemaining, result.getOrNull())
    }
    
    @Test
    fun `calculateForPeriod should return error when period does not exist`() = runTest {
        // Given
        val periodId = 1L
        
        coEvery { repository.getBudgetPeriodById(periodId) } returns Result.Success(null)
        
        // When
        val result = useCase.calculateForPeriod(periodId)
        
        // Then
        assertTrue(result.isError)
        assertEquals("预算周期不存在", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `calculateForPeriod should return error when repository fails`() = runTest {
        // Given
        val periodId = 1L
        val exception = Exception("Database error")
        
        coEvery { repository.getBudgetPeriodById(periodId) } returns Result.Error(exception)
        
        // When
        val result = useCase.calculateForPeriod(periodId)
        
        // Then
        assertTrue(result.isError)
        assertEquals(exception, result.exceptionOrNull())
    }
    
    @Test
    fun `calculateForPeriod should handle negative remaining amount (overspending)`() = runTest {
        // Given
        val periodId = 1L
        val disposableAmount = 500.0
        val totalExpenses = 800.0
        val expectedRemaining = -300.0
        
        val period = BudgetPeriod(
            id = periodId,
            disposableAmount = disposableAmount,
            paydayDate = System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L
        )
        
        coEvery { repository.getBudgetPeriodById(periodId) } returns Result.Success(period)
        coEvery { repository.getTotalExpensesForPeriod(periodId) } returns Result.Success(totalExpenses)
        
        // When
        val result = useCase.calculateForPeriod(periodId)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedRemaining, result.getOrNull())
    }
    
    @Test
    fun `calculateWithDetails should return correct details`() = runTest {
        // Given
        val periodId = 1L
        val disposableAmount = 1000.0
        val expenses = listOf(
            Expense(1, periodId, "Groceries", 200.0),
            Expense(2, periodId, "Gas", 100.0)
        )
        val totalExpenses = 300.0
        val expectedRemaining = 700.0
        
        val period = BudgetPeriod(
            id = periodId,
            disposableAmount = disposableAmount,
            paydayDate = System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L
        )
        
        coEvery { repository.getBudgetPeriodById(periodId) } returns Result.Success(period)
        coEvery { repository.getExpensesForPeriod(periodId) } returns flowOf(expenses)
        
        // When
        val result = useCase.calculateWithDetails(periodId)
        
        // Then
        if (result.isError) {
            println("Test failed with error: ${result.exceptionOrNull()?.message}")
        }
        assertTrue("Result should be success but was error: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val details = result.getOrNull()!!
        assertEquals(disposableAmount, details.disposableAmount, 0.01)
        assertEquals(totalExpenses, details.totalExpenses, 0.01)
        assertEquals(expectedRemaining, details.remainingAmount, 0.01)
        assertEquals(2, details.expenseCount)
        assertFalse(details.isOverspent)
        assertEquals(0.0, details.overspentAmount, 0.01)
    }
    
    @Test
    fun `calculateWithDetails should handle overspending scenario`() = runTest {
        // Given
        val periodId = 1L
        val disposableAmount = 500.0
        val expenses = listOf(
            Expense(1, periodId, "Rent", 600.0),
            Expense(2, periodId, "Food", 200.0)
        )
        val totalExpenses = 800.0
        val expectedRemaining = -300.0
        val expectedOverspent = 300.0
        
        val period = BudgetPeriod(
            id = periodId,
            disposableAmount = disposableAmount,
            paydayDate = System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L
        )
        
        coEvery { repository.getBudgetPeriodById(periodId) } returns Result.Success(period)
        coEvery { repository.getExpensesForPeriod(periodId) } returns flowOf(expenses)
        
        // When
        val result = useCase.calculateWithDetails(periodId)
        
        // Then
        assertTrue(result.isSuccess)
        val details = result.getOrNull()!!
        assertEquals(disposableAmount, details.disposableAmount, 0.01)
        assertEquals(totalExpenses, details.totalExpenses, 0.01)
        assertEquals(expectedRemaining, details.remainingAmount, 0.01)
        assertEquals(2, details.expenseCount)
        assertTrue(details.isOverspent)
        assertEquals(expectedOverspent, details.overspentAmount, 0.01)
    }
    
    @Test
    fun `wouldCauseOverspending should return true when additional amount causes overspending`() = runTest {
        // Given
        val periodId = 1L
        val disposableAmount = 1000.0
        val totalExpenses = 900.0 // 剩余100.0
        val additionalAmount = 150.0 // 会导致超支

        val currentTime = System.currentTimeMillis()
        val mockPeriod = BudgetPeriod(
            id = periodId,
            disposableAmount = disposableAmount,
            createdDate = currentTime,
            paydayDate = currentTime + (30 * 24 * 60 * 60 * 1000L),
            isActive = true
        )

        val expenses = listOf(
            Expense(1, periodId, "测试支出1", 500.0, currentTime),
            Expense(2, periodId, "测试支出2", 400.0, currentTime)
        )

        coEvery { repository.getBudgetPeriodById(periodId) } returns Result.Success(mockPeriod)
        coEvery { repository.getTotalExpensesForPeriod(periodId) } returns Result.Success(totalExpenses)
        coEvery { repository.getExpensesForPeriod(periodId) } returns flowOf(expenses)

        // When
        val result = useCase.wouldCauseOverspending(periodId, additionalAmount)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
    }
    
    @Test
    fun `wouldCauseOverspending should return false when additional amount does not cause overspending`() = runTest {
        // Given
        val periodId = 1L
        val disposableAmount = 1000.0
        val totalExpenses = 700.0 // 剩余300.0
        val additionalAmount = 150.0 // 不会导致超支

        val currentTime = System.currentTimeMillis()
        val mockPeriod = BudgetPeriod(
            id = periodId,
            disposableAmount = disposableAmount,
            createdDate = currentTime,
            paydayDate = currentTime + (30 * 24 * 60 * 60 * 1000L),
            isActive = true
        )

        val expenses = listOf(
            Expense(1, periodId, "测试支出1", 400.0, currentTime),
            Expense(2, periodId, "测试支出2", 300.0, currentTime)
        )

        coEvery { repository.getBudgetPeriodById(periodId) } returns Result.Success(mockPeriod)
        coEvery { repository.getTotalExpensesForPeriod(periodId) } returns Result.Success(totalExpenses)
        coEvery { repository.getExpensesForPeriod(periodId) } returns flowOf(expenses)

        // When
        val result = useCase.wouldCauseOverspending(periodId, additionalAmount)

        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() == true)
    }
    
    @Test
    fun `RemainingAmountDetails should calculate expense percentage correctly`() {
        // Given
        val details = CalculateRemainingAmountUseCase.RemainingAmountDetails(
            disposableAmount = 1000.0,
            totalExpenses = 300.0,
            remainingAmount = 700.0,
            expenseCount = 3,
            isOverspent = false,
            overspentAmount = 0.0
        )
        
        // When
        val percentage = details.expensePercentage
        
        // Then
        assertEquals(30.0, percentage, 0.01)
    }
    
    @Test
    fun `RemainingAmountDetails should detect near overspending`() {
        // Given
        val details = CalculateRemainingAmountUseCase.RemainingAmountDetails(
            disposableAmount = 1000.0,
            totalExpenses = 950.0,
            remainingAmount = 50.0,
            expenseCount = 5,
            isOverspent = false,
            overspentAmount = 0.0
        )
        
        // When
        val isNearOverspending = details.isNearOverspending
        
        // Then
        assertTrue(isNearOverspending)
    }
}
