package com.budgetapp.data.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.budgetapp.data.database.BudgetDatabase
import com.budgetapp.data.database.entities.BudgetPeriod
import com.budgetapp.data.database.entities.Expense
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * BudgetDao的单元测试
 * 
 * 使用内存数据库进行测试，确保DAO方法的正确性
 * 测试所有CRUD操作和复杂查询
 */
@RunWith(AndroidJUnit4::class)
class BudgetDaoTest {
    
    private lateinit var database: BudgetDatabase
    private lateinit var budgetDao: BudgetDao
    
    @Before
    fun setUp() {
        // 创建内存数据库用于测试
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BudgetDatabase::class.java
        )
        .allowMainThreadQueries()
        .build()
        
        budgetDao = database.budgetDao()
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    // ==================== 预算周期测试 ====================
    
    @Test
    fun insertAndGetBudgetPeriod() = runTest {
        // Given
        val budgetPeriod = BudgetPeriod.create(5000.0)
        
        // When
        val insertedId = budgetDao.insertBudgetPeriod(budgetPeriod)
        val retrieved = budgetDao.getBudgetPeriodById(insertedId)
        
        // Then
        assertNotNull(retrieved)
        assertEquals(budgetPeriod.disposableAmount, retrieved!!.disposableAmount, 0.01)
        assertEquals(budgetPeriod.isActive, retrieved.isActive)
    }
    
    @Test
    fun getCurrentBudgetPeriod_returnsActivePeriod() = runTest {
        // Given
        val activePeriod = BudgetPeriod.create(3000.0)
        val inactivePeriod = BudgetPeriod.create(2000.0).copy(isActive = false)
        
        budgetDao.insertBudgetPeriod(inactivePeriod)
        budgetDao.insertBudgetPeriod(activePeriod)
        
        // When
        val current = budgetDao.getCurrentBudgetPeriod().first()
        
        // Then
        assertNotNull(current)
        assertTrue(current!!.isActive)
        assertEquals(3000.0, current.disposableAmount, 0.01)
    }
    
    @Test
    fun getCurrentBudgetPeriod_returnsNullWhenNoActivePeriod() = runTest {
        // Given
        val inactivePeriod = BudgetPeriod.create(2000.0).copy(isActive = false)
        budgetDao.insertBudgetPeriod(inactivePeriod)
        
        // When
        val current = budgetDao.getCurrentBudgetPeriod().first()
        
        // Then
        assertNull(current)
    }
    
    @Test
    fun updateBudgetPeriod() = runTest {
        // Given
        val budgetPeriod = BudgetPeriod.create(4000.0)
        val insertedId = budgetDao.insertBudgetPeriod(budgetPeriod)
        
        // When
        val updatedPeriod = budgetPeriod.copy(id = insertedId, disposableAmount = 5000.0)
        val updateCount = budgetDao.updateBudgetPeriod(updatedPeriod)
        val retrieved = budgetDao.getBudgetPeriodById(insertedId)
        
        // Then
        assertEquals(1, updateCount)
        assertEquals(5000.0, retrieved!!.disposableAmount, 0.01)
    }
    
    @Test
    fun deleteBudgetPeriod() = runTest {
        // Given
        val budgetPeriod = BudgetPeriod.create(3000.0)
        val insertedId = budgetDao.insertBudgetPeriod(budgetPeriod)
        
        // When
        val periodToDelete = budgetPeriod.copy(id = insertedId)
        val deleteCount = budgetDao.deleteBudgetPeriod(periodToDelete)
        val retrieved = budgetDao.getBudgetPeriodById(insertedId)
        
        // Then
        assertEquals(1, deleteCount)
        assertNull(retrieved)
    }
    
    @Test
    fun deactivateAllBudgetPeriods() = runTest {
        // Given
        val period1 = BudgetPeriod.create(1000.0)
        val period2 = BudgetPeriod.create(2000.0)
        budgetDao.insertBudgetPeriod(period1)
        budgetDao.insertBudgetPeriod(period2)
        
        // When
        budgetDao.deactivateAllBudgetPeriods()
        val allPeriods = budgetDao.getAllBudgetPeriods().first()
        
        // Then
        assertEquals(2, allPeriods.size)
        allPeriods.forEach { period ->
            assertFalse(period.isActive)
        }
    }
    
    @Test
    fun resetBudgetPeriod() = runTest {
        // Given
        val oldPeriod = BudgetPeriod.create(2000.0)
        budgetDao.insertBudgetPeriod(oldPeriod)
        val newPeriod = BudgetPeriod.create(5000.0)
        
        // When
        val newPeriodId = budgetDao.resetBudgetPeriod(newPeriod)
        val currentPeriod = budgetDao.getCurrentBudgetPeriod().first()
        val allPeriods = budgetDao.getAllBudgetPeriods().first()
        
        // Then
        assertNotNull(currentPeriod)
        assertEquals(newPeriodId, currentPeriod!!.id)
        assertEquals(5000.0, currentPeriod.disposableAmount, 0.01)
        assertTrue(currentPeriod.isActive)
        
        // 验证旧周期被停用
        val inactivePeriods = allPeriods.filter { !it.isActive }
        assertEquals(1, inactivePeriods.size)
    }
    
    // ==================== 支出测试 ====================
    
    @Test
    fun insertAndGetExpense() = runTest {
        // Given
        val budgetPeriod = BudgetPeriod.create(3000.0)
        val periodId = budgetDao.insertBudgetPeriod(budgetPeriod)
        val expense = Expense.create(periodId, "测试支出", 100.0)!!
        
        // When
        val expenseId = budgetDao.insertExpense(expense)
        val retrieved = budgetDao.getExpenseById(expenseId)
        
        // Then
        assertNotNull(retrieved)
        assertEquals(expense.description, retrieved!!.description)
        assertEquals(expense.amount, retrieved.amount, 0.01)
        assertEquals(periodId, retrieved.budgetPeriodId)
    }
    
    @Test
    fun getExpensesForPeriod() = runTest {
        // Given
        val budgetPeriod = BudgetPeriod.create(3000.0)
        val periodId = budgetDao.insertBudgetPeriod(budgetPeriod)
        
        val expense1 = Expense.create(periodId, "支出1", 100.0)!!
        val expense2 = Expense.create(periodId, "支出2", 200.0)!!
        
        budgetDao.insertExpense(expense1)
        budgetDao.insertExpense(expense2)
        
        // When
        val expenses = budgetDao.getExpensesForPeriod(periodId).first()
        
        // Then
        assertEquals(2, expenses.size)
        // 验证按创建时间倒序排列
        assertTrue(expenses[0].createdDate >= expenses[1].createdDate)
    }
    
    @Test
    fun getCurrentPeriodExpenses() = runTest {
        // Given
        val activePeriod = BudgetPeriod.create(3000.0)
        val inactivePeriod = BudgetPeriod.create(2000.0).copy(isActive = false)
        
        val activePeriodId = budgetDao.insertBudgetPeriod(activePeriod)
        val inactivePeriodId = budgetDao.insertBudgetPeriod(inactivePeriod)
        
        val activeExpense = Expense.create(activePeriodId, "活跃支出", 100.0)!!
        val inactiveExpense = Expense.create(inactivePeriodId, "非活跃支出", 200.0)!!
        
        budgetDao.insertExpense(activeExpense)
        budgetDao.insertExpense(inactiveExpense)
        
        // When
        val currentExpenses = budgetDao.getCurrentPeriodExpenses().first()
        
        // Then
        assertEquals(1, currentExpenses.size)
        assertEquals("活跃支出", currentExpenses[0].description)
    }
    
    @Test
    fun getTotalExpensesForPeriod() = runTest {
        // Given
        val budgetPeriod = BudgetPeriod.create(3000.0)
        val periodId = budgetDao.insertBudgetPeriod(budgetPeriod)
        
        val expense1 = Expense.create(periodId, "支出1", 150.0)!!
        val expense2 = Expense.create(periodId, "支出2", 250.0)!!
        
        budgetDao.insertExpense(expense1)
        budgetDao.insertExpense(expense2)
        
        // When
        val total = budgetDao.getTotalExpensesForPeriod(periodId)
        
        // Then
        assertEquals(400.0, total, 0.01)
    }
    
    @Test
    fun getCurrentPeriodTotalExpenses() = runTest {
        // Given
        val activePeriod = BudgetPeriod.create(3000.0)
        val activePeriodId = budgetDao.insertBudgetPeriod(activePeriod)
        
        val expense1 = Expense.create(activePeriodId, "支出1", 100.0)!!
        val expense2 = Expense.create(activePeriodId, "支出2", 200.0)!!
        
        budgetDao.insertExpense(expense1)
        budgetDao.insertExpense(expense2)
        
        // When
        val total = budgetDao.getCurrentPeriodTotalExpenses().first()
        
        // Then
        assertEquals(300.0, total, 0.01)
    }
    
    @Test
    fun deleteExpense() = runTest {
        // Given
        val budgetPeriod = BudgetPeriod.create(3000.0)
        val periodId = budgetDao.insertBudgetPeriod(budgetPeriod)
        val expense = Expense.create(periodId, "测试支出", 100.0)!!
        val expenseId = budgetDao.insertExpense(expense)
        
        // When
        val expenseToDelete = expense.copy(id = expenseId)
        val deleteCount = budgetDao.deleteExpense(expenseToDelete)
        val retrieved = budgetDao.getExpenseById(expenseId)
        
        // Then
        assertEquals(1, deleteCount)
        assertNull(retrieved)
    }
    
    @Test
    fun deleteExpenseById() = runTest {
        // Given
        val budgetPeriod = BudgetPeriod.create(3000.0)
        val periodId = budgetDao.insertBudgetPeriod(budgetPeriod)
        val expense = Expense.create(periodId, "测试支出", 100.0)!!
        val expenseId = budgetDao.insertExpense(expense)
        
        // When
        val deleteCount = budgetDao.deleteExpenseById(expenseId)
        val retrieved = budgetDao.getExpenseById(expenseId)
        
        // Then
        assertEquals(1, deleteCount)
        assertNull(retrieved)
    }
    
    @Test
    fun cascadeDeleteExpensesWhenBudgetPeriodDeleted() = runTest {
        // Given
        val budgetPeriod = BudgetPeriod.create(3000.0)
        val periodId = budgetDao.insertBudgetPeriod(budgetPeriod)
        
        val expense1 = Expense.create(periodId, "支出1", 100.0)!!
        val expense2 = Expense.create(periodId, "支出2", 200.0)!!
        
        budgetDao.insertExpense(expense1)
        budgetDao.insertExpense(expense2)
        
        // When
        val periodToDelete = budgetPeriod.copy(id = periodId)
        budgetDao.deleteBudgetPeriod(periodToDelete)
        
        // Then
        val expenses = budgetDao.getExpensesForPeriod(periodId).first()
        assertTrue(expenses.isEmpty())
    }
    
    // ==================== 关系查询测试 ====================
    
    @Test
    fun getBudgetPeriodWithExpenses() = runTest {
        // Given
        val budgetPeriod = BudgetPeriod.create(3000.0)
        val periodId = budgetDao.insertBudgetPeriod(budgetPeriod)
        
        val expense1 = Expense.create(periodId, "支出1", 100.0)!!
        val expense2 = Expense.create(periodId, "支出2", 200.0)!!
        
        budgetDao.insertExpense(expense1)
        budgetDao.insertExpense(expense2)
        
        // When
        val result = budgetDao.getBudgetPeriodWithExpenses(periodId)
        
        // Then
        assertNotNull(result)
        assertEquals(3000.0, result!!.budgetPeriod.disposableAmount, 0.01)
        assertEquals(2, result.expenses.size)
        assertEquals(2700.0, result.getRemainingAmount(), 0.01) // 3000 - 100 - 200
        assertFalse(result.isOverBudget())
    }
    
    @Test
    fun getCurrentBudgetPeriodWithExpenses() = runTest {
        // Given
        val budgetPeriod = BudgetPeriod.create(1000.0)
        val periodId = budgetDao.insertBudgetPeriod(budgetPeriod)
        
        val expense = Expense.create(periodId, "大额支出", 1200.0)!!
        budgetDao.insertExpense(expense)
        
        // When
        val result = budgetDao.getCurrentBudgetPeriodWithExpenses().first()
        
        // Then
        assertNotNull(result)
        assertEquals(1000.0, result!!.budgetPeriod.disposableAmount, 0.01)
        assertEquals(1, result.expenses.size)
        assertEquals(-200.0, result.getRemainingAmount(), 0.01) // 1000 - 1200
        assertTrue(result.isOverBudget())
    }
}
