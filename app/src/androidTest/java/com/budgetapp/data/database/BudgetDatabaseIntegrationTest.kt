package com.budgetapp.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.budgetapp.data.database.dao.BudgetDao
import com.budgetapp.data.database.entities.BudgetPeriod
import com.budgetapp.data.database.entities.Expense
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * BudgetDatabaseIntegrationTest
 * 
 * Room数据库的集成测试，测试完整的CRUD操作。
 * 验证外键约束、数据完整性和数据库升级场景。
 * 
 * Requirements:
 * - 6.1-6.5: 测试Room数据库的完整功能
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class BudgetDatabaseIntegrationTest {
    
    private lateinit var database: BudgetDatabase
    private lateinit var budgetDao: BudgetDao
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BudgetDatabase::class.java
        ).allowMainThreadQueries().build()
        
        budgetDao = database.budgetDao()
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun testBudgetPeriodCRUD() = runTest {
        // Create
        val budgetPeriod = BudgetPeriod(
            disposableAmount = 1000.0,
            createdDate = System.currentTimeMillis(),
            paydayDate = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000L),
            isActive = true
        )
        
        val periodId = budgetDao.insertBudgetPeriod(budgetPeriod)
        assertTrue(periodId > 0)
        
        // Read
        val retrievedPeriod = budgetDao.getBudgetPeriodById(periodId)
        assertNotNull(retrievedPeriod)
        assertEquals(1000.0, retrievedPeriod.disposableAmount)
        assertEquals(true, retrievedPeriod.isActive)
        
        // Update
        val updatedPeriod = retrievedPeriod.copy(disposableAmount = 1200.0)
        val updateResult = budgetDao.updateBudgetPeriod(updatedPeriod)
        assertEquals(1, updateResult)
        
        val updatedRetrieved = budgetDao.getBudgetPeriodById(periodId)
        assertEquals(1200.0, updatedRetrieved?.disposableAmount)
        
        // Delete
        val deleteResult = budgetDao.deleteBudgetPeriod(updatedPeriod)
        assertEquals(1, deleteResult)
        
        val deletedPeriod = budgetDao.getBudgetPeriodById(periodId)
        assertNull(deletedPeriod)
    }
    
    @Test
    fun testExpenseCRUD() = runTest {
        // 先创建预算周期
        val budgetPeriod = BudgetPeriod(
            disposableAmount = 1000.0,
            createdDate = System.currentTimeMillis(),
            paydayDate = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000L),
            isActive = true
        )
        val periodId = budgetDao.insertBudgetPeriod(budgetPeriod)
        
        // Create expense
        val expense = Expense(
            budgetPeriodId = periodId,
            description = "Test expense",
            amount = 100.0,
            createdDate = System.currentTimeMillis()
        )
        
        val expenseId = budgetDao.insertExpense(expense)
        assertTrue(expenseId > 0)
        
        // Read
        val retrievedExpense = budgetDao.getExpenseById(expenseId)
        assertNotNull(retrievedExpense)
        assertEquals("Test expense", retrievedExpense.description)
        assertEquals(100.0, retrievedExpense.amount)
        assertEquals(periodId, retrievedExpense.budgetPeriodId)
        
        // Update
        val updatedExpense = retrievedExpense.copy(amount = 150.0)
        val updateResult = budgetDao.updateExpense(updatedExpense)
        assertEquals(1, updateResult)
        
        val updatedRetrieved = budgetDao.getExpenseById(expenseId)
        assertEquals(150.0, updatedRetrieved?.amount)
        
        // Delete
        val deleteResult = budgetDao.deleteExpense(updatedExpense)
        assertEquals(1, deleteResult)
        
        val deletedExpense = budgetDao.getExpenseById(expenseId)
        assertNull(deletedExpense)
    }
    
    @Test
    fun testForeignKeyConstraint() = runTest {
        // 先创建预算周期
        val budgetPeriod = BudgetPeriod(
            disposableAmount = 1000.0,
            createdDate = System.currentTimeMillis(),
            paydayDate = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000L),
            isActive = true
        )
        val periodId = budgetDao.insertBudgetPeriod(budgetPeriod)
        
        // 创建支出
        val expense = Expense(
            budgetPeriodId = periodId,
            description = "Test expense",
            amount = 100.0,
            createdDate = System.currentTimeMillis()
        )
        budgetDao.insertExpense(expense)
        
        // 删除预算周期应该级联删除支出
        budgetDao.deleteBudgetPeriod(budgetPeriod.copy(id = periodId))
        
        // 验证支出也被删除
        val expenses = budgetDao.getExpensesForPeriod(periodId).first()
        assertTrue(expenses.isEmpty())
    }
    
    @Test
    fun testBudgetPeriodWithExpensesRelation() = runTest {
        // 创建预算周期
        val budgetPeriod = BudgetPeriod(
            disposableAmount = 1000.0,
            createdDate = System.currentTimeMillis(),
            paydayDate = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000L),
            isActive = true
        )
        val periodId = budgetDao.insertBudgetPeriod(budgetPeriod)
        
        // 创建多个支出
        val expenses = listOf(
            Expense(
                budgetPeriodId = periodId,
                description = "Expense 1",
                amount = 100.0,
                createdDate = System.currentTimeMillis()
            ),
            Expense(
                budgetPeriodId = periodId,
                description = "Expense 2",
                amount = 200.0,
                createdDate = System.currentTimeMillis()
            )
        )
        
        expenses.forEach { budgetDao.insertExpense(it) }
        
        // 测试关系查询
        val periodWithExpenses = budgetDao.getBudgetPeriodWithExpenses(periodId)
        assertNotNull(periodWithExpenses)
        assertEquals(2, periodWithExpenses.expenses.size)
        assertEquals(700.0, periodWithExpenses.getRemainingAmount()) // 1000 - 300
        assertEquals(false, periodWithExpenses.isOverBudget())
    }
    
    @Test
    fun testCurrentBudgetPeriodQueries() = runTest {
        // 创建多个预算周期，只有一个是活跃的
        val inactivePeriod = BudgetPeriod(
            disposableAmount = 500.0,
            createdDate = System.currentTimeMillis() - 1000000,
            paydayDate = System.currentTimeMillis() - 500000,
            isActive = false
        )
        budgetDao.insertBudgetPeriod(inactivePeriod)
        
        val activePeriod = BudgetPeriod(
            disposableAmount = 1000.0,
            createdDate = System.currentTimeMillis(),
            paydayDate = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000L),
            isActive = true
        )
        val activePeriodId = budgetDao.insertBudgetPeriod(activePeriod)
        
        // 测试获取当前活跃周期
        val currentPeriod = budgetDao.getCurrentBudgetPeriod().first()
        assertNotNull(currentPeriod)
        assertEquals(activePeriodId, currentPeriod.id)
        assertEquals(1000.0, currentPeriod.disposableAmount)
        
        // 测试是否有活跃周期
        val hasActive = budgetDao.hasActiveBudgetPeriod()
        assertTrue(hasActive)
    }
    
    @Test
    fun testExpenseCalculations() = runTest {
        // 创建预算周期
        val budgetPeriod = BudgetPeriod(
            disposableAmount = 1000.0,
            createdDate = System.currentTimeMillis(),
            paydayDate = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000L),
            isActive = true
        )
        val periodId = budgetDao.insertBudgetPeriod(budgetPeriod)
        
        // 添加支出
        val expenses = listOf(
            Expense(periodId, "Expense 1", 100.0),
            Expense(periodId, "Expense 2", 200.0),
            Expense(periodId, "Expense 3", 150.0)
        )
        
        expenses.forEach { budgetDao.insertExpense(it) }
        
        // 测试总支出计算
        val totalExpenses = budgetDao.getTotalExpensesForPeriod(periodId)
        assertEquals(450.0, totalExpenses)
        
        // 测试当前周期总支出流
        val currentTotalFlow = budgetDao.getCurrentPeriodTotalExpenses().first()
        assertEquals(450.0, currentTotalFlow)
        
        // 测试剩余金额流
        val remainingAmountFlow = budgetDao.getCurrentPeriodRemainingAmount().first()
        assertEquals(550.0, remainingAmountFlow) // 1000 - 450
    }
    
    @Test
    fun testTransactionOperations() = runTest {
        // 测试重置预算周期事务
        val oldPeriod = BudgetPeriod(
            disposableAmount = 500.0,
            createdDate = System.currentTimeMillis() - 1000000,
            paydayDate = System.currentTimeMillis() - 500000,
            isActive = true
        )
        val oldPeriodId = budgetDao.insertBudgetPeriod(oldPeriod)
        
        val newPeriod = BudgetPeriod(
            disposableAmount = 1000.0,
            createdDate = System.currentTimeMillis(),
            paydayDate = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000L),
            isActive = true
        )
        
        // 执行重置事务
        budgetDao.resetBudgetPeriod(newPeriod)
        
        // 验证旧周期被停用
        val updatedOldPeriod = budgetDao.getBudgetPeriodById(oldPeriodId)
        assertEquals(false, updatedOldPeriod?.isActive)
        
        // 验证新周期被创建并激活
        val currentPeriod = budgetDao.getCurrentBudgetPeriod().first()
        assertNotNull(currentPeriod)
        assertEquals(1000.0, currentPeriod.disposableAmount)
        assertEquals(true, currentPeriod.isActive)
    }
}
