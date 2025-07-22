package com.budgetapp.data.repository

import com.budgetapp.data.database.dao.BudgetDao
import com.budgetapp.data.database.entities.BudgetPeriod
import com.budgetapp.data.database.entities.Expense
import com.budgetapp.domain.model.Result
import com.budgetapp.domain.repository.BudgetRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * BudgetRepositoryImpl单元测试
 * 
 * 测试Repository层的数据访问逻辑，包括成功和失败场景
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BudgetRepositoryImplTest {
    
    @MockK
    private lateinit var budgetDao: BudgetDao
    
    private lateinit var repository: BudgetRepository
    
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        repository = BudgetRepositoryImpl(
            budgetDao = budgetDao,
            ioDispatcher = Dispatchers.Unconfined
        )
    }
    
    @Test
    fun `createBudgetPeriod should return success when data is valid`() = runTest {
        // Given
        val disposableAmount = 1000.0
        val paydayDate = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000L)
        val expectedId = 1L
        
        coEvery { budgetDao.deactivateAllBudgetPeriods() } returns Unit
        coEvery { budgetDao.insertBudgetPeriod(any()) } returns expectedId
        
        // When
        val result = repository.createBudgetPeriod(disposableAmount, paydayDate)
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(expectedId, result.data)
        coVerify { budgetDao.deactivateAllBudgetPeriods() }
        coVerify { budgetDao.insertBudgetPeriod(any()) }
    }
    
    @Test
    fun `createBudgetPeriod should return error when amount is negative`() = runTest {
        // Given
        val disposableAmount = -100.0
        val paydayDate = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000L)
        
        // When
        val result = repository.createBudgetPeriod(disposableAmount, paydayDate)
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue(result.exception.message?.contains("不能为负数") == true)
    }
    
    @Test
    fun `addExpense should return success when data is valid`() = runTest {
        // Given
        val description = "测试支出"
        val amount = 50.0
        val expectedId = 1L
        val mockPeriod = BudgetPeriod(
            id = 1L,
            disposableAmount = 1000.0,
            paydayDate = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000L),
            isActive = true
        )
        
        coEvery { budgetDao.getCurrentBudgetPeriodSync() } returns mockPeriod
        coEvery { budgetDao.insertExpense(any()) } returns expectedId
        
        // When
        val result = repository.addExpense(description, amount)
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(expectedId, result.data)
        coVerify { budgetDao.insertExpense(any()) }
    }
    
    @Test
    fun `addExpense should return error when description is empty`() = runTest {
        // Given
        val description = ""
        val amount = 50.0
        
        // When
        val result = repository.addExpense(description, amount)
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue(result.exception.message?.contains("不能为空") == true)
    }
    
    @Test
    fun `addExpense should return error when amount is zero`() = runTest {
        // Given
        val description = "测试支出"
        val amount = 0.0
        
        // When
        val result = repository.addExpense(description, amount)
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue(result.exception.message?.contains("必须大于0") == true)
    }
    
    @Test
    fun `validateExpenseData should return success for valid data`() {
        // Given
        val description = "有效的支出描述"
        val amount = 100.0
        
        // When
        val result = repository.validateExpenseData(description, amount)
        
        // Then
        assertTrue(result is Result.Success)
    }
    
    @Test
    fun `validateExpenseData should return error for blank description`() {
        // Given
        val description = "   "
        val amount = 100.0
        
        // When
        val result = repository.validateExpenseData(description, amount)
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue(result.exception.message?.contains("不能为空") == true)
    }
    
    @Test
    fun `validateExpenseData should return error for too long description`() {
        // Given
        val description = "这是一个非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常长的描述"
        val amount = 100.0
        
        // When
        val result = repository.validateExpenseData(description, amount)
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue(result.exception.message?.contains("不能超过50个字符") == true)
    }
    
    @Test
    fun `validateBudgetPeriodData should return success for valid data`() {
        // Given
        val disposableAmount = 1000.0
        val paydayDate = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000L)
        
        // When
        val result = repository.validateBudgetPeriodData(disposableAmount, paydayDate)
        
        // Then
        assertTrue(result is Result.Success)
    }
    
    @Test
    fun `validateBudgetPeriodData should return error for negative amount`() {
        // Given
        val disposableAmount = -100.0
        val paydayDate = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000L)
        
        // When
        val result = repository.validateBudgetPeriodData(disposableAmount, paydayDate)
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue(result.exception.message?.contains("不能为负数") == true)
    }
    
    @Test
    fun `validateBudgetPeriodData should return error for past payday date`() {
        // Given
        val disposableAmount = 1000.0
        val paydayDate = System.currentTimeMillis() - (24 * 60 * 60 * 1000L) // 昨天
        
        // When
        val result = repository.validateBudgetPeriodData(disposableAmount, paydayDate)
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue(result.exception.message?.contains("必须是未来的日期") == true)
    }
    
    @Test
    fun `getCurrentPeriodRemainingAmount should calculate correctly`() = runTest {
        // Given
        val mockPeriod = BudgetPeriod(
            id = 1L,
            disposableAmount = 1000.0,
            paydayDate = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000L),
            isActive = true
        )
        val totalExpenses = 300.0
        val expectedRemaining = 700.0
        
        coEvery { budgetDao.getCurrentBudgetPeriod() } returns flowOf(mockPeriod)
        coEvery { budgetDao.getCurrentPeriodTotalExpenses() } returns flowOf(totalExpenses)
        
        // When
        val result = repository.getCurrentPeriodRemainingAmount()
        
        // Then
        result.collect { remaining ->
            assertEquals(expectedRemaining, remaining)
        }
    }
}
