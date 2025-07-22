package com.budgetapp.data.repository

import com.budgetapp.data.database.dao.BudgetDao
import com.budgetapp.data.database.dao.BudgetPeriodWithExpenses
import com.budgetapp.data.database.entities.BudgetPeriod
import com.budgetapp.data.database.entities.Expense
import com.budgetapp.data.di.IoDispatcher
import com.budgetapp.domain.model.Result
import com.budgetapp.domain.model.safeSuspendCall
import com.budgetapp.domain.repository.BudgetRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BudgetRepositoryImpl实现类
 * 
 * 实现BudgetRepository接口，提供具体的数据访问逻辑。
 * 集成Room DAO，实现所有数据库操作，并添加错误处理和异常转换逻辑。
 * 
 * Requirements:
 * - 6.2: 立即将数据保存到本地SQLite数据库
 * - 6.4: 显示错误信息并提供重试选项
 */
@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : BudgetRepository {
    
    // ==================== 预算周期相关操作 ====================
    
    override fun getCurrentBudgetPeriod(): Flow<BudgetPeriod?> {
        return budgetDao.getCurrentBudgetPeriod()
    }
    
    override fun getAllBudgetPeriods(): Flow<List<BudgetPeriod>> {
        return budgetDao.getAllBudgetPeriods()
    }
    
    override suspend fun getBudgetPeriodById(periodId: Long): Result<BudgetPeriod?> = 
        withContext(ioDispatcher) {
            safeSuspendCall {
                budgetDao.getBudgetPeriodById(periodId)
            }
        }
    
    override suspend fun createBudgetPeriod(
        disposableAmount: Double,
        paydayDate: Long
    ): Result<Long> = withContext(ioDispatcher) {
        // 验证输入数据
        validateBudgetPeriodData(disposableAmount, paydayDate).let { validation ->
            if (validation is Result.Error) return@withContext validation
        }
        
        safeSuspendCall {
            val newPeriod = BudgetPeriod(
                disposableAmount = disposableAmount,
                paydayDate = paydayDate,
                createdDate = System.currentTimeMillis(),
                isActive = true
            )
            
            // 先停用所有现有预算周期
            budgetDao.deactivateAllBudgetPeriods()
            // 插入新的预算周期
            budgetDao.insertBudgetPeriod(newPeriod)
        }
    }
    
    override suspend fun updateBudgetPeriod(period: BudgetPeriod): Result<Int> = 
        withContext(ioDispatcher) {
            safeSuspendCall {
                budgetDao.updateBudgetPeriod(period)
            }
        }
    
    override suspend fun deleteBudgetPeriod(period: BudgetPeriod): Result<Int> = 
        withContext(ioDispatcher) {
            safeSuspendCall {
                budgetDao.deleteBudgetPeriod(period)
            }
        }
    
    override suspend fun resetBudgetPeriod(
        disposableAmount: Double,
        paydayDate: Long
    ): Result<Long> = withContext(ioDispatcher) {
        // 验证输入数据
        validateBudgetPeriodData(disposableAmount, paydayDate).let { validation ->
            if (validation is Result.Error) return@withContext validation
        }
        
        safeSuspendCall {
            val newPeriod = BudgetPeriod(
                disposableAmount = disposableAmount,
                paydayDate = paydayDate,
                createdDate = System.currentTimeMillis(),
                isActive = true
            )
            budgetDao.resetBudgetPeriod(newPeriod)
        }
    }
    
    // ==================== 支出相关操作 ====================
    
    override fun getExpensesForPeriod(periodId: Long): Flow<List<Expense>> {
        return budgetDao.getExpensesForPeriod(periodId)
    }
    
    override fun getCurrentPeriodExpenses(): Flow<List<Expense>> {
        return budgetDao.getCurrentPeriodExpenses()
    }
    
    override suspend fun getExpenseById(expenseId: Long): Result<Expense?> = 
        withContext(ioDispatcher) {
            safeSuspendCall {
                budgetDao.getExpenseById(expenseId)
            }
        }
    
    override suspend fun addExpense(
        description: String,
        amount: Double,
        budgetPeriodId: Long?
    ): Result<Long> = withContext(ioDispatcher) {
        // 验证输入数据
        validateExpenseData(description, amount).let { validation ->
            if (validation is Result.Error) return@withContext validation
        }
        
        safeSuspendCall {
            // 如果没有指定预算周期ID，使用当前活跃周期
            val periodId = budgetPeriodId ?: run {
                val currentPeriod = budgetDao.getCurrentBudgetPeriodSync()
                    ?: throw IllegalStateException("没有活跃的预算周期，请先创建预算周期")
                currentPeriod.id
            }
            
            val expense = Expense(
                budgetPeriodId = periodId,
                description = description.trim(),
                amount = amount,
                createdDate = System.currentTimeMillis()
            )
            
            budgetDao.insertExpense(expense)
        }
    }
    
    override suspend fun updateExpense(expense: Expense): Result<Int> = 
        withContext(ioDispatcher) {
            // 验证支出数据
            validateExpenseData(expense.description, expense.amount).let { validation ->
                if (validation is Result.Error) return@withContext validation
            }
            
            safeSuspendCall {
                budgetDao.updateExpense(expense)
            }
        }
    
    override suspend fun deleteExpense(expense: Expense): Result<Int> = 
        withContext(ioDispatcher) {
            safeSuspendCall {
                budgetDao.deleteExpense(expense)
            }
        }
    
    override suspend fun deleteExpenseById(expenseId: Long): Result<Int> = 
        withContext(ioDispatcher) {
            safeSuspendCall {
                budgetDao.deleteExpenseById(expenseId)
            }
        }
    
    // ==================== 计算相关操作 ====================
    
    override suspend fun getTotalExpensesForPeriod(periodId: Long): Result<Double> = 
        withContext(ioDispatcher) {
            safeSuspendCall {
                budgetDao.getTotalExpensesForPeriod(periodId)
            }
        }
    
    override fun getCurrentPeriodTotalExpenses(): Flow<Double> {
        return budgetDao.getCurrentPeriodTotalExpenses()
    }
    
    override suspend fun calculateRemainingAmount(periodId: Long): Result<Double> = 
        withContext(ioDispatcher) {
            safeSuspendCall {
                val period = budgetDao.getBudgetPeriodById(periodId)
                    ?: throw IllegalArgumentException("预算周期不存在: $periodId")
                
                val totalExpenses = budgetDao.getTotalExpensesForPeriod(periodId)
                period.disposableAmount - totalExpenses
            }
        }
    
    override fun getCurrentPeriodRemainingAmount(): Flow<Double> {
        return combine(
            getCurrentBudgetPeriod(),
            getCurrentPeriodTotalExpenses()
        ) { period, totalExpenses ->
            period?.disposableAmount?.minus(totalExpenses) ?: 0.0
        }
    }
    
    // ==================== 组合数据操作 ====================
    
    override suspend fun getBudgetPeriodWithExpenses(periodId: Long): Result<BudgetPeriodWithExpenses?> = 
        withContext(ioDispatcher) {
            safeSuspendCall {
                budgetDao.getBudgetPeriodWithExpenses(periodId)
            }
        }
    
    override fun getCurrentBudgetPeriodWithExpenses(): Flow<BudgetPeriodWithExpenses?> {
        return budgetDao.getCurrentBudgetPeriodWithExpenses()
    }
    
    // ==================== 验证相关操作 ====================
    
    override fun validateExpenseData(description: String, amount: Double): Result<Unit> {
        return when {
            description.isBlank() -> Result.Error(
                IllegalArgumentException("支出描述不能为空")
            )
            description.length > 50 -> Result.Error(
                IllegalArgumentException("支出描述不能超过50个字符")
            )
            amount <= 0 -> Result.Error(
                IllegalArgumentException("支出金额必须大于0")
            )
            amount > 999999.99 -> Result.Error(
                IllegalArgumentException("支出金额不能超过999,999.99")
            )
            else -> Result.Success(Unit)
        }
    }
    
    override fun validateBudgetPeriodData(disposableAmount: Double, paydayDate: Long): Result<Unit> {
        val currentTime = System.currentTimeMillis()
        return when {
            disposableAmount < 0 -> Result.Error(
                IllegalArgumentException("可支配金额不能为负数")
            )
            disposableAmount > 9999999.99 -> Result.Error(
                IllegalArgumentException("可支配金额不能超过9,999,999.99")
            )
            paydayDate <= currentTime -> Result.Error(
                IllegalArgumentException("发薪日必须是未来的日期")
            )
            paydayDate > currentTime + (365L * 24 * 60 * 60 * 1000) -> Result.Error(
                IllegalArgumentException("发薪日不能超过一年后")
            )
            else -> Result.Success(Unit)
        }
    }
    
    override suspend fun hasActiveBudgetPeriod(): Result<Boolean> = 
        withContext(ioDispatcher) {
            safeSuspendCall {
                budgetDao.getCurrentBudgetPeriodSync() != null
            }
        }
}
