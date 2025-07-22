package com.budgetapp.domain.usecase

import com.budgetapp.data.database.entities.Expense
import com.budgetapp.domain.model.Result
import com.budgetapp.domain.repository.BudgetRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DeleteExpenseUseCase
 * 
 * 处理删除支出的业务逻辑用例。
 * 支持单个删除和批量删除操作。
 * 
 * Requirements:
 * - 5.3: 提供删除选项
 * - 5.4: 从数据库中移除支出项目并更新剩余金额
 */
@Singleton
class DeleteExpenseUseCase @Inject constructor(
    private val repository: BudgetRepository
) {
    
    /**
     * 根据ID删除支出
     * 
     * @param expenseId 支出ID
     * @return 操作结果，包含受影响的行数或错误信息
     */
    suspend fun executeById(expenseId: Long): Result<DeleteExpenseResult> {
        return try {
            // 验证支出ID
            if (expenseId <= 0) {
                return Result.Error(Exception("无效的支出ID"))
            }
            
            // 获取支出信息（用于返回结果）
            val expenseResult = repository.getExpenseById(expenseId)
            if (expenseResult.isError) {
                return Result.Error(
                    expenseResult.exceptionOrNull() ?: Exception("无法获取支出信息")
                )
            }
            
            val expense = expenseResult.getOrNull()
                ?: return Result.Error(Exception("支出不存在"))
            
            // 删除支出
            val deleteResult = repository.deleteExpenseById(expenseId)
            if (deleteResult.isError) {
                return Result.Error(
                    deleteResult.exceptionOrNull() ?: Exception("删除支出失败")
                )
            }
            
            val affectedRows = deleteResult.getOrNull() ?: 0
            
            val result = DeleteExpenseResult(
                deletedExpense = expense,
                affectedRows = affectedRows,
                wasSuccessful = affectedRows > 0
            )
            
            Result.Success(result)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * 删除支出对象
     * 
     * @param expense 要删除的支出对象
     * @return 操作结果，包含受影响的行数或错误信息
     */
    suspend fun execute(expense: Expense): Result<DeleteExpenseResult> {
        return try {
            // 验证支出对象
            if (!expense.isValid()) {
                return Result.Error(Exception("无效的支出对象"))
            }
            
            // 删除支出
            val deleteResult = repository.deleteExpense(expense)
            if (deleteResult.isError) {
                return Result.Error(
                    deleteResult.exceptionOrNull() ?: Exception("删除支出失败")
                )
            }
            
            val affectedRows = deleteResult.getOrNull() ?: 0
            
            val result = DeleteExpenseResult(
                deletedExpense = expense,
                affectedRows = affectedRows,
                wasSuccessful = affectedRows > 0
            )
            
            Result.Success(result)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * 批量删除支出
     * 
     * @param expenseIds 要删除的支出ID列表
     * @return 操作结果，包含删除统计信息
     */
    suspend fun executeBatch(expenseIds: List<Long>): Result<BatchDeleteResult> {
        return try {
            if (expenseIds.isEmpty()) {
                return Result.Error(Exception("支出ID列表不能为空"))
            }
            
            // 验证所有ID
            for (id in expenseIds) {
                if (id <= 0) {
                    return Result.Error(Exception("包含无效的支出ID: $id"))
                }
            }
            
            val deletedExpenses = mutableListOf<Expense>()
            val failedIds = mutableListOf<Long>()
            var totalAffectedRows = 0
            
            // 逐个删除支出
            for (expenseId in expenseIds) {
                try {
                    // 获取支出信息
                    val expenseResult = repository.getExpenseById(expenseId)
                    if (expenseResult.isError) {
                        failedIds.add(expenseId)
                        continue
                    }
                    
                    val expense = expenseResult.getOrNull()
                    if (expense == null) {
                        failedIds.add(expenseId)
                        continue
                    }
                    
                    // 删除支出
                    val deleteResult = repository.deleteExpenseById(expenseId)
                    if (deleteResult.isError) {
                        failedIds.add(expenseId)
                        continue
                    }
                    
                    val affectedRows = deleteResult.getOrNull() ?: 0
                    if (affectedRows > 0) {
                        deletedExpenses.add(expense)
                        totalAffectedRows += affectedRows
                    } else {
                        failedIds.add(expenseId)
                    }
                } catch (e: Exception) {
                    failedIds.add(expenseId)
                }
            }
            
            val result = BatchDeleteResult(
                deletedExpenses = deletedExpenses,
                failedIds = failedIds,
                totalAffectedRows = totalAffectedRows,
                successCount = deletedExpenses.size,
                failureCount = failedIds.size
            )
            
            Result.Success(result)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * 删除指定预算周期的所有支出
     * 
     * @param budgetPeriodId 预算周期ID
     * @return 操作结果，包含删除统计信息
     */
    suspend fun deleteAllForPeriod(budgetPeriodId: Long): Result<BatchDeleteResult> {
        return try {
            if (budgetPeriodId <= 0) {
                return Result.Error(Exception("无效的预算周期ID"))
            }
            
            // 获取该周期的所有支出
            val expensesFlow = repository.getExpensesForPeriod(budgetPeriodId)
            val expenses = mutableListOf<Expense>()
            
            expensesFlow.collect { expenseList ->
                expenses.clear()
                expenses.addAll(expenseList)
            }
            
            if (expenses.isEmpty()) {
                return Result.Success(
                    BatchDeleteResult(
                        deletedExpenses = emptyList(),
                        failedIds = emptyList(),
                        totalAffectedRows = 0,
                        successCount = 0,
                        failureCount = 0
                    )
                )
            }
            
            // 批量删除
            val expenseIds = expenses.map { it.id }
            return executeBatch(expenseIds)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * 检查支出是否可以删除
     * 
     * @param expenseId 支出ID
     * @return 是否可以删除
     */
    suspend fun canDelete(expenseId: Long): Result<Boolean> {
        return try {
            if (expenseId <= 0) {
                return Result.Success(false)
            }
            
            val expenseResult = repository.getExpenseById(expenseId)
            if (expenseResult.isError) {
                return Result.Success(false)
            }
            
            val expense = expenseResult.getOrNull()
            Result.Success(expense != null && expense.isValid())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * 删除支出结果数据类
     */
    data class DeleteExpenseResult(
        val deletedExpense: Expense,
        val affectedRows: Int,
        val wasSuccessful: Boolean
    ) {
        /**
         * 获取删除成功消息
         */
        fun getSuccessMessage(): String {
            return "已删除支出：${deletedExpense.getFormattedDescription()} (¥${String.format("%.2f", deletedExpense.amount)})"
        }
    }
    
    /**
     * 批量删除结果数据类
     */
    data class BatchDeleteResult(
        val deletedExpenses: List<Expense>,
        val failedIds: List<Long>,
        val totalAffectedRows: Int,
        val successCount: Int,
        val failureCount: Int
    ) {
        /**
         * 检查是否全部成功
         */
        val isAllSuccessful: Boolean
            get() = failureCount == 0
        
        /**
         * 检查是否部分成功
         */
        val isPartiallySuccessful: Boolean
            get() = successCount > 0 && failureCount > 0
        
        /**
         * 获取删除的总金额
         */
        val totalDeletedAmount: Double
            get() = deletedExpenses.sumOf { it.amount }
        
        /**
         * 获取批量删除结果消息
         */
        fun getResultMessage(): String {
            return when {
                isAllSuccessful -> "成功删除 $successCount 项支出，总金额 ¥${String.format("%.2f", totalDeletedAmount)}"
                isPartiallySuccessful -> "成功删除 $successCount 项支出，$failureCount 项失败"
                else -> "删除失败，共 ${failedIds.size} 项支出无法删除"
            }
        }
    }
}
