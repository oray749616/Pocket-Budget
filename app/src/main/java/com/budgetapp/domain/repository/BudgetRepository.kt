package com.budgetapp.domain.repository

import com.budgetapp.data.database.dao.BudgetPeriodWithExpenses
import com.budgetapp.data.database.entities.BudgetPeriod
import com.budgetapp.data.database.entities.Expense
import com.budgetapp.domain.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * BudgetRepository接口
 * 
 * 定义预算管理应用的所有数据操作抽象方法。
 * 使用Flow返回类型支持响应式数据流，确保UI能实时更新。
 * 使用Result包装类处理操作结果和错误状态。
 * 
 * Requirements:
 * - 6.3: 从数据库加载之前保存的数据
 * - 6.4: 显示错误信息并提供重试选项
 */
interface BudgetRepository {
    
    // ==================== 预算周期相关操作 ====================
    
    /**
     * 获取当前活跃的预算周期
     * 
     * @return 当前活跃预算周期的Flow，如果没有则返回null
     */
    fun getCurrentBudgetPeriod(): Flow<BudgetPeriod?>
    
    /**
     * 获取所有预算周期
     * 
     * @return 所有预算周期的Flow，按创建时间倒序
     */
    fun getAllBudgetPeriods(): Flow<List<BudgetPeriod>>
    
    /**
     * 根据ID获取特定预算周期
     * 
     * @param periodId 预算周期ID
     * @return 操作结果，包含预算周期数据或错误信息
     */
    suspend fun getBudgetPeriodById(periodId: Long): Result<BudgetPeriod?>
    
    /**
     * 创建新的预算周期
     * 
     * @param disposableAmount 可支配金额
     * @param paydayDate 发薪日时间戳
     * @return 操作结果，包含新创建的预算周期ID或错误信息
     */
    suspend fun createBudgetPeriod(
        disposableAmount: Double,
        paydayDate: Long
    ): Result<Long>
    
    /**
     * 更新预算周期
     * 
     * @param period 要更新的预算周期
     * @return 操作结果，包含受影响的行数或错误信息
     */
    suspend fun updateBudgetPeriod(period: BudgetPeriod): Result<Int>
    
    /**
     * 删除预算周期
     * 
     * @param period 要删除的预算周期
     * @return 操作结果，包含受影响的行数或错误信息
     */
    suspend fun deleteBudgetPeriod(period: BudgetPeriod): Result<Int>
    
    /**
     * 重置预算周期
     * 停用当前预算周期并创建新的预算周期
     * 
     * @param disposableAmount 新的可支配金额
     * @param paydayDate 新的发薪日时间戳
     * @return 操作结果，包含新预算周期ID或错误信息
     */
    suspend fun resetBudgetPeriod(
        disposableAmount: Double,
        paydayDate: Long
    ): Result<Long>
    
    // ==================== 支出相关操作 ====================
    
    /**
     * 获取指定预算周期的所有支出
     * 
     * @param periodId 预算周期ID
     * @return 支出列表的Flow，按创建时间倒序
     */
    fun getExpensesForPeriod(periodId: Long): Flow<List<Expense>>
    
    /**
     * 获取当前预算周期的所有支出
     * 
     * @return 当前预算周期支出列表的Flow，按创建时间倒序
     */
    fun getCurrentPeriodExpenses(): Flow<List<Expense>>
    
    /**
     * 根据ID获取特定支出
     * 
     * @param expenseId 支出ID
     * @return 操作结果，包含支出数据或错误信息
     */
    suspend fun getExpenseById(expenseId: Long): Result<Expense?>
    
    /**
     * 添加新支出
     * 
     * @param description 支出描述
     * @param amount 支出金额
     * @param budgetPeriodId 所属预算周期ID，如果为null则使用当前活跃周期
     * @return 操作结果，包含新支出ID或错误信息
     */
    suspend fun addExpense(
        description: String,
        amount: Double,
        budgetPeriodId: Long? = null
    ): Result<Long>
    
    /**
     * 更新支出
     * 
     * @param expense 要更新的支出
     * @return 操作结果，包含受影响的行数或错误信息
     */
    suspend fun updateExpense(expense: Expense): Result<Int>
    
    /**
     * 删除支出
     * 
     * @param expense 要删除的支出
     * @return 操作结果，包含受影响的行数或错误信息
     */
    suspend fun deleteExpense(expense: Expense): Result<Int>
    
    /**
     * 根据ID删除支出
     * 
     * @param expenseId 支出ID
     * @return 操作结果，包含受影响的行数或错误信息
     */
    suspend fun deleteExpenseById(expenseId: Long): Result<Int>
    
    // ==================== 计算相关操作 ====================
    
    /**
     * 获取指定预算周期的支出总额
     * 
     * @param periodId 预算周期ID
     * @return 操作结果，包含支出总额或错误信息
     */
    suspend fun getTotalExpensesForPeriod(periodId: Long): Result<Double>
    
    /**
     * 获取当前预算周期的支出总额
     * 
     * @return 当前预算周期支出总额的Flow
     */
    fun getCurrentPeriodTotalExpenses(): Flow<Double>
    
    /**
     * 计算指定预算周期的剩余可支配金额
     * 
     * @param periodId 预算周期ID
     * @return 操作结果，包含剩余金额或错误信息
     */
    suspend fun calculateRemainingAmount(periodId: Long): Result<Double>
    
    /**
     * 获取当前预算周期的剩余可支配金额
     * 
     * @return 当前预算周期剩余金额的Flow
     */
    fun getCurrentPeriodRemainingAmount(): Flow<Double>
    
    // ==================== 组合数据操作 ====================
    
    /**
     * 获取预算周期及其所有支出
     * 
     * @param periodId 预算周期ID
     * @return 操作结果，包含预算周期和支出的组合数据或错误信息
     */
    suspend fun getBudgetPeriodWithExpenses(periodId: Long): Result<BudgetPeriodWithExpenses?>
    
    /**
     * 获取当前预算周期及其所有支出
     * 
     * @return 当前预算周期和支出组合数据的Flow
     */
    fun getCurrentBudgetPeriodWithExpenses(): Flow<BudgetPeriodWithExpenses?>
    
    // ==================== 验证相关操作 ====================
    
    /**
     * 验证支出数据的有效性
     * 
     * @param description 支出描述
     * @param amount 支出金额
     * @return 验证结果，成功返回Unit，失败返回错误信息
     */
    fun validateExpenseData(description: String, amount: Double): Result<Unit>
    
    /**
     * 验证预算周期数据的有效性
     * 
     * @param disposableAmount 可支配金额
     * @param paydayDate 发薪日时间戳
     * @return 验证结果，成功返回Unit，失败返回错误信息
     */
    fun validateBudgetPeriodData(disposableAmount: Double, paydayDate: Long): Result<Unit>
    
    /**
     * 检查是否存在活跃的预算周期
     * 
     * @return 操作结果，包含是否存在活跃周期的布尔值或错误信息
     */
    suspend fun hasActiveBudgetPeriod(): Result<Boolean>
}
