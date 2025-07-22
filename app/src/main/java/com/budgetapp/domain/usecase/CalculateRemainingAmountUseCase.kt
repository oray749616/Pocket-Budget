package com.budgetapp.domain.usecase

import com.budgetapp.data.database.entities.BudgetPeriod
import com.budgetapp.data.database.entities.Expense
import com.budgetapp.domain.model.Result
import com.budgetapp.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CalculateRemainingAmountUseCase
 * 
 * 计算剩余可支配金额的业务逻辑用例。
 * 实现计算逻辑：可支配金额 - 所有支出总和
 * 
 * Requirements:
 * - 3.2: 添加或删除支出项目时立即重新计算并显示更新后的剩余金额
 * - 3.5: 计算公式为：可支配金额 - 所有计划支出的总和
 */
@Singleton
class CalculateRemainingAmountUseCase @Inject constructor(
    private val repository: BudgetRepository
) {
    
    /**
     * 计算指定预算周期的剩余可支配金额
     * 
     * @param periodId 预算周期ID
     * @return 操作结果，包含剩余金额或错误信息
     */
    suspend fun calculateForPeriod(periodId: Long): Result<Double> {
        return try {
            // 获取预算周期
            val periodResult = repository.getBudgetPeriodById(periodId)
            if (periodResult.isError) {
                return Result.Error(
                    periodResult.exceptionOrNull() ?: Exception("无法获取预算周期")
                )
            }
            
            val period = periodResult.getOrNull()
                ?: return Result.Error(Exception("预算周期不存在"))
            
            // 获取支出总额
            val totalExpensesResult = repository.getTotalExpensesForPeriod(periodId)
            if (totalExpensesResult.isError) {
                return Result.Error(
                    totalExpensesResult.exceptionOrNull() ?: Exception("无法计算支出总额")
                )
            }
            
            val totalExpenses = totalExpensesResult.getOrNull() ?: 0.0
            
            // 计算剩余金额
            val remainingAmount = period.disposableAmount - totalExpenses
            
            Result.Success(remainingAmount)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * 获取当前预算周期的剩余可支配金额（响应式）
     * 
     * @return 当前预算周期剩余金额的Flow
     */
    fun getCurrentPeriodRemainingAmount(): Flow<Double> {
        return repository.getCurrentPeriodRemainingAmount()
    }
    
    /**
     * 计算剩余金额并返回详细信息
     * 
     * @param periodId 预算周期ID
     * @return 包含详细计算信息的结果
     */
    suspend fun calculateWithDetails(periodId: Long): Result<RemainingAmountDetails> {
        return try {
            // 获取预算周期
            val periodResult = repository.getBudgetPeriodById(periodId)
            if (periodResult.isError) {
                return Result.Error(
                    periodResult.exceptionOrNull() ?: Exception("无法获取预算周期")
                )
            }
            
            val period = periodResult.getOrNull()
                ?: return Result.Error(Exception("预算周期不存在"))
            
            // 获取支出列表
            val expensesResult = repository.getExpensesForPeriod(periodId)
            val expenses = mutableListOf<Expense>()
            
            expensesResult.collect { expenseList ->
                expenses.clear()
                expenses.addAll(expenseList)
            }
            
            // 计算总支出
            val totalExpenses = expenses.sumOf { it.amount }
            
            // 计算剩余金额
            val remainingAmount = period.disposableAmount - totalExpenses
            
            // 创建详细信息
            val details = RemainingAmountDetails(
                disposableAmount = period.disposableAmount,
                totalExpenses = totalExpenses,
                remainingAmount = remainingAmount,
                expenseCount = expenses.size,
                isOverspent = remainingAmount < 0,
                overspentAmount = if (remainingAmount < 0) -remainingAmount else 0.0
            )
            
            Result.Success(details)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * 获取当前预算周期的剩余金额详细信息（响应式）
     * 
     * @return 当前预算周期剩余金额详细信息的Flow
     */
    fun getCurrentPeriodRemainingAmountDetails(): Flow<RemainingAmountDetails?> {
        return combine(
            repository.getCurrentBudgetPeriod(),
            repository.getCurrentPeriodExpenses()
        ) { period, expenses ->
            period?.let { budgetPeriod ->
                val totalExpenses = expenses.sumOf { it.amount }
                val remainingAmount = budgetPeriod.disposableAmount - totalExpenses
                
                RemainingAmountDetails(
                    disposableAmount = budgetPeriod.disposableAmount,
                    totalExpenses = totalExpenses,
                    remainingAmount = remainingAmount,
                    expenseCount = expenses.size,
                    isOverspent = remainingAmount < 0,
                    overspentAmount = if (remainingAmount < 0) -remainingAmount else 0.0
                )
            }
        }
    }
    
    /**
     * 检查指定金额是否会导致超支
     * 
     * @param periodId 预算周期ID
     * @param additionalAmount 要添加的金额
     * @return 是否会导致超支
     */
    suspend fun wouldCauseOverspending(
        periodId: Long, 
        additionalAmount: Double
    ): Result<Boolean> {
        return try {
            val remainingResult = calculateForPeriod(periodId)
            if (remainingResult.isError) {
                return Result.Error(
                    remainingResult.exceptionOrNull() ?: Exception("无法计算剩余金额")
                )
            }
            
            val remainingAmount = remainingResult.getOrNull() ?: 0.0
            val wouldOverspend = (remainingAmount - additionalAmount) < 0
            
            Result.Success(wouldOverspend)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * 剩余金额详细信息数据类
     */
    data class RemainingAmountDetails(
        val disposableAmount: Double,
        val totalExpenses: Double,
        val remainingAmount: Double,
        val expenseCount: Int,
        val isOverspent: Boolean,
        val overspentAmount: Double
    ) {
        /**
         * 获取支出百分比
         */
        val expensePercentage: Double
            get() = if (disposableAmount > 0) {
                (totalExpenses / disposableAmount) * 100
            } else {
                0.0
            }
        
        /**
         * 检查是否接近超支（剩余金额少于10%）
         */
        val isNearOverspending: Boolean
            get() = !isOverspent && (remainingAmount / disposableAmount) < 0.1
    }
}
