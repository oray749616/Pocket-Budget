package com.budgetapp.data.database.dao

import androidx.room.*
import com.budgetapp.data.database.entities.BudgetPeriod
import com.budgetapp.data.database.entities.Expense
import kotlinx.coroutines.flow.Flow

/**
 * BudgetDao数据访问对象
 * 
 * 定义所有与预算管理相关的数据库操作方法，包括预算周期和支出的CRUD操作。
 * 使用Flow返回响应式数据流，支持UI的实时更新。
 * 
 * Requirements:
 * - 6.1: 创建SQLite数据库和必要的表结构
 * - 6.3: 从数据库加载之前保存的数据
 */
@Dao
interface BudgetDao {
    
    // ==================== 预算周期相关操作 ====================
    
    /**
     * 获取当前活跃的预算周期
     * 使用Flow返回响应式数据，UI可以自动响应数据变化
     * 
     * @return 当前活跃的预算周期，如果没有则返回null
     */
    @Query("SELECT * FROM budget_periods WHERE is_active = 1 ORDER BY created_date DESC LIMIT 1")
    fun getCurrentBudgetPeriod(): Flow<BudgetPeriod?>
    
    /**
     * 获取当前活跃的预算周期（同步方法）
     * 用于事务操作中需要同步获取数据的场景
     * 
     * @return 当前活跃的预算周期，如果没有则返回null
     */
    @Query("SELECT * FROM budget_periods WHERE is_active = 1 ORDER BY created_date DESC LIMIT 1")
    suspend fun getCurrentBudgetPeriodSync(): BudgetPeriod?
    
    /**
     * 获取所有预算周期（按创建时间倒序）
     * 
     * @return 所有预算周期的Flow
     */
    @Query("SELECT * FROM budget_periods ORDER BY created_date DESC")
    fun getAllBudgetPeriods(): Flow<List<BudgetPeriod>>
    
    /**
     * 根据ID获取特定的预算周期
     * 
     * @param periodId 预算周期ID
     * @return 指定的预算周期，如果不存在则返回null
     */
    @Query("SELECT * FROM budget_periods WHERE id = :periodId")
    suspend fun getBudgetPeriodById(periodId: Long): BudgetPeriod?
    
    /**
     * 插入新的预算周期
     * 
     * @param period 要插入的预算周期
     * @return 插入记录的ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetPeriod(period: BudgetPeriod): Long
    
    /**
     * 更新预算周期
     * 
     * @param period 要更新的预算周期
     * @return 受影响的行数
     */
    @Update
    suspend fun updateBudgetPeriod(period: BudgetPeriod): Int
    
    /**
     * 删除预算周期
     * 由于设置了CASCADE删除，相关的支出记录也会被自动删除
     * 
     * @param period 要删除的预算周期
     * @return 受影响的行数
     */
    @Delete
    suspend fun deleteBudgetPeriod(period: BudgetPeriod): Int
    
    /**
     * 停用所有预算周期
     * 用于重置预算周期时，先停用所有现有周期
     */
    @Query("UPDATE budget_periods SET is_active = 0")
    suspend fun deactivateAllBudgetPeriods()
    
    // ==================== 支出相关操作 ====================
    
    /**
     * 获取指定预算周期的所有支出
     * 按创建时间倒序排列，最新的支出在前
     * 
     * @param periodId 预算周期ID
     * @return 支出列表的Flow
     */
    @Query("SELECT * FROM expenses WHERE budget_period_id = :periodId ORDER BY created_date DESC")
    fun getExpensesForPeriod(periodId: Long): Flow<List<Expense>>
    
    /**
     * 获取当前活跃预算周期的所有支出
     * 
     * @return 当前预算周期的支出列表Flow
     */
    @Query("""
        SELECT e.* FROM expenses e 
        INNER JOIN budget_periods bp ON e.budget_period_id = bp.id 
        WHERE bp.is_active = 1 
        ORDER BY e.created_date DESC
    """)
    fun getCurrentPeriodExpenses(): Flow<List<Expense>>
    
    /**
     * 根据ID获取特定支出
     * 
     * @param expenseId 支出ID
     * @return 指定的支出，如果不存在则返回null
     */
    @Query("SELECT * FROM expenses WHERE id = :expenseId")
    suspend fun getExpenseById(expenseId: Long): Expense?
    
    /**
     * 计算指定预算周期的支出总额
     * 
     * @param periodId 预算周期ID
     * @return 支出总额
     */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE budget_period_id = :periodId")
    suspend fun getTotalExpensesForPeriod(periodId: Long): Double
    
    /**
     * 计算当前活跃预算周期的支出总额
     * 
     * @return 当前预算周期的支出总额Flow
     */
    @Query("""
        SELECT COALESCE(SUM(e.amount), 0) FROM expenses e 
        INNER JOIN budget_periods bp ON e.budget_period_id = bp.id 
        WHERE bp.is_active = 1
    """)
    fun getCurrentPeriodTotalExpenses(): Flow<Double>
    
    /**
     * 插入新支出
     * 
     * @param expense 要插入的支出
     * @return 插入记录的ID
     */
    @Insert
    suspend fun insertExpense(expense: Expense): Long
    
    /**
     * 批量插入支出
     * 
     * @param expenses 要插入的支出列表
     * @return 插入记录的ID列表
     */
    @Insert
    suspend fun insertExpenses(expenses: List<Expense>): List<Long>
    
    /**
     * 更新支出
     * 
     * @param expense 要更新的支出
     * @return 受影响的行数
     */
    @Update
    suspend fun updateExpense(expense: Expense): Int
    
    /**
     * 删除支出
     * 
     * @param expense 要删除的支出
     * @return 受影响的行数
     */
    @Delete
    suspend fun deleteExpense(expense: Expense): Int
    
    /**
     * 根据ID删除支出
     * 
     * @param expenseId 支出ID
     * @return 受影响的行数
     */
    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteExpenseById(expenseId: Long): Int
    
    /**
     * 删除指定预算周期的所有支出
     * 
     * @param periodId 预算周期ID
     * @return 受影响的行数
     */
    @Query("DELETE FROM expenses WHERE budget_period_id = :periodId")
    suspend fun deleteExpensesForPeriod(periodId: Long): Int
    
    // ==================== 事务操作 ====================
    
    /**
     * 重置预算周期
     * 停用当前预算周期并创建新的预算周期
     * 使用事务确保操作的原子性
     * 
     * @param newPeriod 新的预算周期
     * @return 新预算周期的ID
     */
    @Transaction
    suspend fun resetBudgetPeriod(newPeriod: BudgetPeriod): Long {
        // 停用所有现有预算周期
        deactivateAllBudgetPeriods()
        // 插入新的预算周期
        return insertBudgetPeriod(newPeriod)
    }
    
    /**
     * 获取预算周期及其所有支出
     * 使用事务确保数据一致性
     * 
     * @param periodId 预算周期ID
     * @return 预算周期和支出的组合数据
     */
    @Transaction
    @Query("SELECT * FROM budget_periods WHERE id = :periodId")
    suspend fun getBudgetPeriodWithExpenses(periodId: Long): BudgetPeriodWithExpenses?
    
    /**
     * 获取当前预算周期及其所有支出
     * 
     * @return 当前预算周期和支出的组合数据Flow
     */
    @Transaction
    @Query("SELECT * FROM budget_periods WHERE is_active = 1 ORDER BY created_date DESC LIMIT 1")
    fun getCurrentBudgetPeriodWithExpenses(): Flow<BudgetPeriodWithExpenses?>
}

/**
 * 预算周期与支出的关系数据类
 * 用于一次性获取预算周期及其相关支出
 */
data class BudgetPeriodWithExpenses(
    @Embedded val budgetPeriod: BudgetPeriod,
    @Relation(
        parentColumn = "id",
        entityColumn = "budget_period_id"
    )
    val expenses: List<Expense>
) {
    /**
     * 计算剩余可支配金额
     * 
     * @return 剩余金额
     */
    fun getRemainingAmount(): Double {
        val totalExpenses = expenses.sumOf { it.amount }
        return budgetPeriod.disposableAmount - totalExpenses
    }
    
    /**
     * 检查是否超支
     * 
     * @return true 如果超支
     */
    fun isOverBudget(): Boolean {
        return getRemainingAmount() < 0
    }
}
