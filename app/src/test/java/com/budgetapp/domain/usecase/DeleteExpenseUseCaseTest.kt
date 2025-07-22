package com.budgetapp.domain.usecase

import com.budgetapp.data.database.entities.Expense
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
 * DeleteExpenseUseCase单元测试
 * 
 * 测试删除支出Use Case的各种场景，包括单个删除和批量删除。
 */
@ExtendWith(MockitoExtension::class)
class DeleteExpenseUseCaseTest {
    
    private lateinit var repository: BudgetRepository
    private lateinit var useCase: DeleteExpenseUseCase
    
    @BeforeEach
    fun setup() {
        repository = mockk()
        useCase = DeleteExpenseUseCase(repository)
    }
    
    @Test
    fun `executeById should delete expense successfully when expense exists`() = runTest {
        // Given
        val expenseId = 1L
        val expense = Expense(
            id = expenseId,
            budgetPeriodId = 1L,
            description = "Test expense",
            amount = 100.0
        )
        val affectedRows = 1
        
        coEvery { repository.getExpenseById(expenseId) } returns Result.Success(expense)
        coEvery { repository.deleteExpenseById(expenseId) } returns Result.Success(affectedRows)
        
        // When
        val result = useCase.executeById(expenseId)
        
        // Then
        assertTrue(result.isSuccess)
        val deleteResult = result.getOrNull()!!
        assertEquals(expense, deleteResult.deletedExpense)
        assertEquals(affectedRows, deleteResult.affectedRows)
        assertTrue(deleteResult.wasSuccessful)
    }
    
    @Test
    fun `executeById should return error when expense does not exist`() = runTest {
        // Given
        val expenseId = 1L
        
        coEvery { repository.getExpenseById(expenseId) } returns Result.Success(null)
        
        // When
        val result = useCase.executeById(expenseId)
        
        // Then
        assertTrue(result.isError)
        assertEquals("支出不存在", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `executeById should return error when expense ID is invalid`() = runTest {
        // Given
        val invalidExpenseId = -1L
        
        // When
        val result = useCase.executeById(invalidExpenseId)
        
        // Then
        assertTrue(result.isError)
        assertEquals("无效的支出ID", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `execute should delete expense successfully with valid expense object`() = runTest {
        // Given
        val expense = Expense(
            id = 1L,
            budgetPeriodId = 1L,
            description = "Test expense",
            amount = 100.0
        )
        val affectedRows = 1
        
        coEvery { repository.deleteExpense(expense) } returns Result.Success(affectedRows)
        
        // When
        val result = useCase.execute(expense)
        
        // Then
        assertTrue(result.isSuccess)
        val deleteResult = result.getOrNull()!!
        assertEquals(expense, deleteResult.deletedExpense)
        assertEquals(affectedRows, deleteResult.affectedRows)
        assertTrue(deleteResult.wasSuccessful)
    }
    
    @Test
    fun `execute should return error when expense object is invalid`() = runTest {
        // Given
        val invalidExpense = Expense(
            id = 0L, // Invalid ID
            budgetPeriodId = 1L,
            description = "",
            amount = -100.0
        )
        
        // When
        val result = useCase.execute(invalidExpense)
        
        // Then
        assertTrue(result.isError)
        assertEquals("无效的支出对象", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `executeBatch should delete multiple expenses successfully`() = runTest {
        // Given
        val expenseIds = listOf(1L, 2L, 3L)
        val expenses = listOf(
            Expense(1L, 1L, "Expense 1", 100.0),
            Expense(2L, 1L, "Expense 2", 200.0),
            Expense(3L, 1L, "Expense 3", 300.0)
        )
        
        // Mock getting each expense
        coEvery { repository.getExpenseById(1L) } returns Result.Success(expenses[0])
        coEvery { repository.getExpenseById(2L) } returns Result.Success(expenses[1])
        coEvery { repository.getExpenseById(3L) } returns Result.Success(expenses[2])
        
        // Mock deleting each expense
        coEvery { repository.deleteExpenseById(1L) } returns Result.Success(1)
        coEvery { repository.deleteExpenseById(2L) } returns Result.Success(1)
        coEvery { repository.deleteExpenseById(3L) } returns Result.Success(1)
        
        // When
        val result = useCase.executeBatch(expenseIds)
        
        // Then
        assertTrue(result.isSuccess)
        val batchResult = result.getOrNull()!!
        assertEquals(3, batchResult.successCount)
        assertEquals(0, batchResult.failureCount)
        assertEquals(expenses, batchResult.deletedExpenses)
        assertTrue(batchResult.isAllSuccessful)
        assertEquals(600.0, batchResult.totalDeletedAmount, 0.01)
    }
    
    @Test
    fun `executeBatch should handle partial failures`() = runTest {
        // Given
        val expenseIds = listOf(1L, 2L, 3L)
        val successfulExpenses = listOf(
            Expense(1L, 1L, "Expense 1", 100.0),
            Expense(3L, 1L, "Expense 3", 300.0)
        )
        
        // Mock getting expenses (2L fails)
        coEvery { repository.getExpenseById(1L) } returns Result.Success(successfulExpenses[0])
        coEvery { repository.getExpenseById(2L) } returns Result.Error(Exception("Not found"))
        coEvery { repository.getExpenseById(3L) } returns Result.Success(successfulExpenses[1])
        
        // Mock deleting successful expenses
        coEvery { repository.deleteExpenseById(1L) } returns Result.Success(1)
        coEvery { repository.deleteExpenseById(3L) } returns Result.Success(1)
        
        // When
        val result = useCase.executeBatch(expenseIds)
        
        // Then
        assertTrue(result.isSuccess)
        val batchResult = result.getOrNull()!!
        assertEquals(2, batchResult.successCount)
        assertEquals(1, batchResult.failureCount)
        assertEquals(successfulExpenses, batchResult.deletedExpenses)
        assertEquals(listOf(2L), batchResult.failedIds)
        assertTrue(batchResult.isPartiallySuccessful)
        assertEquals(400.0, batchResult.totalDeletedAmount, 0.01)
    }
    
    @Test
    fun `executeBatch should return error when expense ID list is empty`() = runTest {
        // Given
        val emptyExpenseIds = emptyList<Long>()
        
        // When
        val result = useCase.executeBatch(emptyExpenseIds)
        
        // Then
        assertTrue(result.isError)
        assertEquals("支出ID列表不能为空", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `executeBatch should return error when expense ID list contains invalid IDs`() = runTest {
        // Given
        val invalidExpenseIds = listOf(1L, -1L, 3L) // -1L is invalid
        
        // When
        val result = useCase.executeBatch(invalidExpenseIds)
        
        // Then
        assertTrue(result.isError)
        assertTrue(result.exceptionOrNull()?.message?.contains("无效的支出ID") == true)
    }
    
    @Test
    fun `deleteAllForPeriod should delete all expenses for given period`() = runTest {
        // Given
        val budgetPeriodId = 1L
        val expenses = listOf(
            Expense(1L, budgetPeriodId, "Expense 1", 100.0),
            Expense(2L, budgetPeriodId, "Expense 2", 200.0)
        )
        
        coEvery { repository.getExpensesForPeriod(budgetPeriodId) } returns flowOf(expenses)
        coEvery { repository.getExpenseById(1L) } returns Result.Success(expenses[0])
        coEvery { repository.getExpenseById(2L) } returns Result.Success(expenses[1])
        coEvery { repository.deleteExpenseById(1L) } returns Result.Success(1)
        coEvery { repository.deleteExpenseById(2L) } returns Result.Success(1)
        
        // When
        val result = useCase.deleteAllForPeriod(budgetPeriodId)
        
        // Then
        assertTrue(result.isSuccess)
        val batchResult = result.getOrNull()!!
        assertEquals(2, batchResult.successCount)
        assertEquals(0, batchResult.failureCount)
        assertTrue(batchResult.isAllSuccessful)
    }
    
    @Test
    fun `canDelete should return true for valid expense`() = runTest {
        // Given
        val expenseId = 1L
        val expense = Expense(
            id = expenseId,
            budgetPeriodId = 1L,
            description = "Valid expense",
            amount = 100.0
        )
        
        coEvery { repository.getExpenseById(expenseId) } returns Result.Success(expense)
        
        // When
        val result = useCase.canDelete(expenseId)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
    }
    
    @Test
    fun `canDelete should return false for invalid expense ID`() = runTest {
        // Given
        val invalidExpenseId = -1L
        
        // When
        val result = useCase.canDelete(invalidExpenseId)
        
        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() == true)
    }
    
    @Test
    fun `DeleteExpenseResult should generate correct success message`() {
        // Given
        val expense = Expense(
            id = 1L,
            budgetPeriodId = 1L,
            description = "Test expense",
            amount = 123.45
        )
        val deleteResult = DeleteExpenseUseCase.DeleteExpenseResult(
            deletedExpense = expense,
            affectedRows = 1,
            wasSuccessful = true
        )
        
        // When
        val message = deleteResult.getSuccessMessage()
        
        // Then
        assertTrue(message.contains("Test expense"))
        assertTrue(message.contains("123.45"))
    }
    
    @Test
    fun `BatchDeleteResult should generate correct result messages`() {
        // Given
        val expenses = listOf(
            Expense(1L, 1L, "Expense 1", 100.0),
            Expense(2L, 1L, "Expense 2", 200.0)
        )
        
        val allSuccessfulResult = DeleteExpenseUseCase.BatchDeleteResult(
            deletedExpenses = expenses,
            failedIds = emptyList(),
            totalAffectedRows = 2,
            successCount = 2,
            failureCount = 0
        )
        
        val partialSuccessResult = DeleteExpenseUseCase.BatchDeleteResult(
            deletedExpenses = listOf(expenses[0]),
            failedIds = listOf(2L),
            totalAffectedRows = 1,
            successCount = 1,
            failureCount = 1
        )
        
        // When & Then
        assertTrue(allSuccessfulResult.getResultMessage().contains("成功删除 2 项支出"))
        assertTrue(partialSuccessResult.getResultMessage().contains("成功删除 1 项支出，1 项失败"))
    }
}
