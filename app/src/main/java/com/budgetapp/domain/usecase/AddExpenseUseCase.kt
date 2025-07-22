package com.budgetapp.domain.usecase

import com.budgetapp.data.database.entities.Expense
import com.budgetapp.domain.model.Result
import com.budgetapp.domain.repository.BudgetRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AddExpenseUseCase
 * 
 * 处理添加支出的业务逻辑用例。
 * 包含输入验证：金额必须为正数，描述不能为空。
 * 
 * Requirements:
 * - 2.2: 验证支出输入的完整性和有效性
 * - 2.3: 将支出项目保存到SQLite数据库
 */
@Singleton
class AddExpenseUseCase @Inject constructor(
    private val repository: BudgetRepository
) {
    
    /**
     * 添加新支出到指定预算周期
     * 
     * @param description 支出描述
     * @param amount 支出金额
     * @param budgetPeriodId 预算周期ID，如果为null则使用当前活跃周期
     * @return 操作结果，包含新支出ID或错误信息
     */
    suspend fun execute(
        description: String,
        amount: Double,
        budgetPeriodId: Long? = null
    ): Result<Long> {
        return try {
            // 验证输入数据
            val validationResult = validateInput(description, amount)
            if (validationResult.isError) {
                return validationResult as Result<Long>
            }
            
            // 如果没有指定预算周期ID，检查是否存在活跃的预算周期
            if (budgetPeriodId == null) {
                val hasActiveResult = repository.hasActiveBudgetPeriod()
                if (hasActiveResult.isError) {
                    return Result.Error(
                        hasActiveResult.exceptionOrNull() ?: Exception("无法检查活跃预算周期")
                    )
                }
                
                val hasActive = hasActiveResult.getOrNull() ?: false
                if (!hasActive) {
                    return Result.Error(Exception("没有活跃的预算周期，请先创建预算周期"))
                }
            }
            
            // 添加支出
            val result = repository.addExpense(
                description = description.trim(),
                amount = amount,
                budgetPeriodId = budgetPeriodId
            )
            
            result
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * 添加支出并检查是否会导致超支
     * 
     * @param description 支出描述
     * @param amount 支出金额
     * @param budgetPeriodId 预算周期ID，如果为null则使用当前活跃周期
     * @return 添加结果，包含超支警告信息
     */
    suspend fun executeWithOverspendingCheck(
        description: String,
        amount: Double,
        budgetPeriodId: Long? = null
    ): Result<AddExpenseResult> {
        return try {
            // 验证输入数据
            val validationResult = validateInput(description, amount)
            if (validationResult.isError) {
                return Result.Error(
                    validationResult.exceptionOrNull() ?: Exception("输入验证失败")
                )
            }
            
            // 获取当前预算周期（如果没有指定）
            val targetPeriodId = budgetPeriodId ?: run {
                val hasActiveResult = repository.hasActiveBudgetPeriod()
                if (hasActiveResult.isError || hasActiveResult.getOrNull() != true) {
                    return Result.Error(Exception("没有活跃的预算周期，请先创建预算周期"))
                }
                
                // 获取当前活跃周期的ID
                val currentPeriod = repository.getCurrentBudgetPeriod()
                var periodId: Long? = null
                currentPeriod.collect { period ->
                    periodId = period?.id
                }
                periodId ?: return Result.Error(Exception("无法获取当前预算周期ID"))
            }
            
            // 检查是否会导致超支
            val remainingResult = repository.calculateRemainingAmount(targetPeriodId)
            if (remainingResult.isError) {
                return Result.Error(
                    remainingResult.exceptionOrNull() ?: Exception("无法计算剩余金额")
                )
            }
            
            val remainingAmount = remainingResult.getOrNull() ?: 0.0
            val willOverspend = (remainingAmount - amount) < 0
            val overspendAmount = if (willOverspend) amount - remainingAmount else 0.0
            
            // 添加支出
            val addResult = repository.addExpense(
                description = description.trim(),
                amount = amount,
                budgetPeriodId = targetPeriodId
            )
            
            if (addResult.isError) {
                return Result.Error(
                    addResult.exceptionOrNull() ?: Exception("添加支出失败")
                )
            }
            
            val expenseId = addResult.getOrNull() ?: 0L
            
            val result = AddExpenseResult(
                expenseId = expenseId,
                willOverspend = willOverspend,
                overspendAmount = overspendAmount,
                remainingAmountAfter = remainingAmount - amount
            )
            
            Result.Success(result)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * 批量添加支出
     * 
     * @param expenses 支出列表
     * @param budgetPeriodId 预算周期ID，如果为null则使用当前活跃周期
     * @return 操作结果，包含成功添加的支出ID列表
     */
    suspend fun executeBatch(
        expenses: List<ExpenseInput>,
        budgetPeriodId: Long? = null
    ): Result<List<Long>> {
        return try {
            if (expenses.isEmpty()) {
                return Result.Error(Exception("支出列表不能为空"))
            }
            
            // 验证所有输入
            for (expense in expenses) {
                val validationResult = validateInput(expense.description, expense.amount)
                if (validationResult.isError) {
                    return Result.Error(
                        Exception("支出 '${expense.description}' 验证失败: ${validationResult.exceptionOrNull()?.message}")
                    )
                }
            }
            
            // 检查是否存在活跃的预算周期
            if (budgetPeriodId == null) {
                val hasActiveResult = repository.hasActiveBudgetPeriod()
                if (hasActiveResult.isError || hasActiveResult.getOrNull() != true) {
                    return Result.Error(Exception("没有活跃的预算周期，请先创建预算周期"))
                }
            }
            
            val addedIds = mutableListOf<Long>()
            
            // 逐个添加支出
            for (expense in expenses) {
                val result = repository.addExpense(
                    description = expense.description.trim(),
                    amount = expense.amount,
                    budgetPeriodId = budgetPeriodId
                )
                
                if (result.isError) {
                    return Result.Error(
                        Exception("添加支出 '${expense.description}' 失败: ${result.exceptionOrNull()?.message}")
                    )
                }
                
                val expenseId = result.getOrNull() ?: 0L
                addedIds.add(expenseId)
            }
            
            Result.Success(addedIds)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * 验证支出输入数据
     * 
     * @param description 支出描述
     * @param amount 支出金额
     * @return 验证结果
     */
    private fun validateInput(description: String, amount: Double): Result<Unit> {
        // 使用Repository的验证方法
        return repository.validateExpenseData(description, amount)
    }
    
    /**
     * 支出输入数据类
     */
    data class ExpenseInput(
        val description: String,
        val amount: Double
    )
    
    /**
     * 添加支出结果数据类
     */
    data class AddExpenseResult(
        val expenseId: Long,
        val willOverspend: Boolean,
        val overspendAmount: Double,
        val remainingAmountAfter: Double
    ) {
        /**
         * 获取超支警告消息
         */
        fun getOverspendWarning(): String? {
            return if (willOverspend) {
                "警告：此支出将导致超支 ¥${String.format("%.2f", overspendAmount)}"
            } else {
                null
            }
        }
        
        /**
         * 检查是否接近超支
         */
        val isNearOverspending: Boolean
            get() = !willOverspend && remainingAmountAfter < 100.0 // 剩余金额少于100元
    }
}
