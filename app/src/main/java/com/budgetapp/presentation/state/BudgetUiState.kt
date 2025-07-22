package com.budgetapp.presentation.state

import com.budgetapp.data.database.entities.BudgetPeriod
import com.budgetapp.data.database.entities.Expense

/**
 * BudgetUiState
 * 
 * 预算应用的UI状态数据类，包含所有界面显示所需的状态信息。
 * 使用不可变数据类确保状态管理的安全性和可预测性。
 * 
 * Requirements:
 * - 3.1: 显示当前剩余可支配金额
 * - 3.4: 支持响应式UI更新
 * - 4.1: 显示距离发薪日的剩余天数
 * - 5.1: 显示支出列表
 * - 7.4: 显示加载状态
 * - 7.5: 显示错误信息和用户反馈
 */
data class BudgetUiState(
    // ==================== 加载和错误状态 ====================
    
    /**
     * 是否正在加载数据
     * 用于显示加载指示器
     */
    val isLoading: Boolean = false,
    
    /**
     * 错误信息
     * 当操作失败时显示给用户的错误消息
     */
    val errorMessage: String? = null,
    
    /**
     * 成功消息
     * 当操作成功时显示给用户的反馈消息
     */
    val successMessage: String? = null,
    
    // ==================== 核心数据状态 ====================
    
    /**
     * 当前预算周期
     * 包含可支配金额、创建日期、发薪日等信息
     */
    val currentBudgetPeriod: BudgetPeriod? = null,
    
    /**
     * 当前周期的支出列表
     * 按创建时间倒序排列
     */
    val expenses: List<Expense> = emptyList(),
    
    /**
     * 剩余可支配金额
     * 计算公式：可支配金额 - 所有支出总和
     */
    val remainingAmount: Double = 0.0,
    
    /**
     * 距离发薪日的剩余天数
     * 基于30天周期计算
     */
    val daysUntilPayday: Int = 0,
    
    // ==================== 计算状态 ====================
    
    /**
     * 是否超支
     * 当剩余金额为负数时为true
     */
    val isOverBudget: Boolean = false,
    
    /**
     * 超支金额
     * 当超支时显示超出的金额
     */
    val overspendAmount: Double = 0.0,
    
    /**
     * 支出总额
     * 当前周期所有支出的总和
     */
    val totalExpenses: Double = 0.0,
    
    // ==================== UI交互状态 ====================
    
    /**
     * 是否显示添加支出对话框
     */
    val showAddExpenseDialog: Boolean = false,
    
    /**
     * 是否显示删除确认对话框
     */
    val showDeleteConfirmDialog: Boolean = false,
    
    /**
     * 待删除的支出ID
     * 用于删除确认对话框
     */
    val expenseToDelete: Long? = null,
    
    /**
     * 是否显示重置预算周期对话框
     */
    val showResetBudgetDialog: Boolean = false,
    
    /**
     * 是否显示超支警告
     */
    val showOverspendWarning: Boolean = false,
    
    // ==================== 输入状态 ====================
    
    /**
     * 支出描述输入
     */
    val expenseDescription: String = "",
    
    /**
     * 支出金额输入
     */
    val expenseAmount: String = "",
    
    /**
     * 可支配金额输入
     */
    val disposableAmountInput: String = "",
    
    /**
     * 输入验证错误
     */
    val inputErrors: Map<String, String> = emptyMap()
) {
    
    // ==================== 计算属性 ====================
    
    /**
     * 是否有活跃的预算周期
     */
    val hasActiveBudgetPeriod: Boolean
        get() = currentBudgetPeriod != null && currentBudgetPeriod.isActive
    
    /**
     * 是否有支出记录
     */
    val hasExpenses: Boolean
        get() = expenses.isNotEmpty()
    
    /**
     * 预算周期进度百分比
     * 基于已过天数计算
     */
    val budgetPeriodProgress: Float
        get() {
            val period = currentBudgetPeriod ?: return 0f
            val totalDays = 30 // 固定30天周期
            val remainingDays = daysUntilPayday
            val passedDays = totalDays - remainingDays
            return (passedDays.toFloat() / totalDays).coerceIn(0f, 1f)
        }
    
    /**
     * 支出使用百分比
     * 基于已使用金额计算
     */
    val expenseUsagePercentage: Float
        get() {
            val period = currentBudgetPeriod ?: return 0f
            if (period.disposableAmount <= 0) return 0f
            return (totalExpenses / period.disposableAmount).toFloat().coerceIn(0f, 1f)
        }
    
    /**
     * 是否接近发薪日
     * 少于7天时返回true
     */
    val isNearPayday: Boolean
        get() = daysUntilPayday in 1..7
    
    /**
     * 是否有输入错误
     */
    val hasInputErrors: Boolean
        get() = inputErrors.isNotEmpty()
    
    /**
     * 是否可以添加支出
     * 需要有活跃的预算周期且输入有效
     */
    val canAddExpense: Boolean
        get() = hasActiveBudgetPeriod && 
                expenseDescription.isNotBlank() && 
                expenseAmount.isNotBlank() && 
                !hasInputErrors
    
    /**
     * 是否显示空状态
     * 当没有预算周期或支出时显示
     */
    val showEmptyState: Boolean
        get() = !isLoading && !hasActiveBudgetPeriod
    
    /**
     * 是否显示支出空状态
     * 当有预算周期但没有支出时显示
     */
    val showExpenseEmptyState: Boolean
        get() = hasActiveBudgetPeriod && !hasExpenses && !isLoading
}

/**
 * BudgetEvent
 * 
 * 预算应用的一次性事件，用于处理导航、提示等不需要保存在状态中的事件。
 */
sealed class BudgetEvent {
    /**
     * 支出添加成功事件
     */
    object ExpenseAdded : BudgetEvent()
    
    /**
     * 支出删除成功事件
     */
    data class ExpenseDeleted(val expenseDescription: String) : BudgetEvent()
    
    /**
     * 预算周期重置成功事件
     */
    object BudgetPeriodReset : BudgetEvent()
    
    /**
     * 显示错误消息事件
     */
    data class ShowError(val message: String) : BudgetEvent()
    
    /**
     * 显示成功消息事件
     */
    data class ShowSuccess(val message: String) : BudgetEvent()
    
    /**
     * 显示超支警告事件
     */
    data class ShowOverspendWarning(val overspendAmount: Double) : BudgetEvent()
    
    /**
     * 导航到设置页面事件
     */
    object NavigateToSettings : BudgetEvent()
}
