package com.budgetapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetapp.domain.repository.BudgetRepository
import com.budgetapp.presentation.state.BudgetUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BudgetViewModel
 * 
 * 预算应用的主要ViewModel，管理UI状态和业务逻辑。
 * 使用MVVM架构模式，通过StateFlow管理状态。
 * 
 * Requirements:
 * - 3.1: 管理剩余可支配金额的显示
 * - 3.4: 支持响应式UI更新
 * - 1.2: 处理可支配金额输入
 * - 2.2: 处理支出添加
 * - 2.4: 处理支出删除
 * - 5.3: 管理支出列表显示
 */
@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "BudgetViewModel"
    }
    
    // ==================== 状态管理 ====================
    
    /**
     * 私有的可变UI状态
     */
    private val _uiState = MutableStateFlow(BudgetUiState())
    
    /**
     * 公开的只读UI状态
     * UI层通过此StateFlow观察状态变化
     */
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()
    
    // ==================== 初始化 ====================
    
    init {
        // 启动时加载初始数据
        loadInitialData()
        
        // 监听数据变化并更新UI状态
        observeDataChanges()
    }
    
    // ==================== 数据加载 ====================
    
    /**
     * 加载初始数据
     */
    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                
                // 检查是否有活跃的预算周期
                val hasActivePeriod = budgetRepository.hasActiveBudgetPeriod()
                if (hasActivePeriod.isError) {
                    handleError(hasActivePeriod.exceptionOrNull(), "检查预算周期失败")
                    return@launch
                }
                
                _uiState.update { it.copy(isLoading = false) }
                
            } catch (e: Exception) {
                handleError(e, "加载初始数据失败")
            }
        }
    }
    
    /**
     * 监听数据变化
     */
    private fun observeDataChanges() {
        viewModelScope.launch {
            // 监听当前预算周期变化
            budgetRepository.getCurrentBudgetPeriod()
                .catch { e -> handleError(e, "监听预算周期失败") }
                .collect { budgetPeriod ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            currentBudgetPeriod = budgetPeriod
                        )
                    }
                }
        }
        
        viewModelScope.launch {
            // 监听当前周期支出变化
            budgetRepository.getCurrentPeriodExpenses()
                .catch { e -> handleError(e, "监听支出数据失败") }
                .collect { expenses ->
                    val totalExpenses = expenses.sumOf { it.amount }
                    _uiState.update { currentState ->
                        currentState.copy(
                            expenses = expenses,
                            totalExpenses = totalExpenses
                        )
                    }
                }
        }
        
        viewModelScope.launch {
            // 监听剩余金额变化
            budgetRepository.getCurrentPeriodRemainingAmount()
                .catch { e -> handleError(e, "计算剩余金额失败") }
                .collect { remainingAmount ->
                    val isOverBudget = remainingAmount < 0
                    val overspendAmount = if (isOverBudget) -remainingAmount else 0.0
                    
                    _uiState.update { currentState ->
                        currentState.copy(
                            remainingAmount = remainingAmount,
                            isOverBudget = isOverBudget,
                            overspendAmount = overspendAmount
                        )
                    }
                }
        }
    }
    
    // ==================== 预算周期管理 ====================
    
    /**
     * 显示重置预算对话框
     */
    fun showResetBudgetDialog() {
        _uiState.update { it.copy(showResetBudgetDialog = true) }
    }
    
    /**
     * 隐藏重置预算对话框
     */
    fun hideResetBudgetDialog() {
        _uiState.update { 
            it.copy(
                showResetBudgetDialog = false,
                disposableAmountInput = "",
                inputErrors = it.inputErrors - "disposableAmount"
            ) 
        }
    }
    
    /**
     * 更新可支配金额输入
     * 
     * @param amount 可支配金额字符串
     */
    fun updateDisposableAmount(amount: String) {
        _uiState.update { currentState ->
            val errors = currentState.inputErrors.toMutableMap()
            
            // 验证可支配金额输入
            try {
                if (amount.isBlank()) {
                    errors["disposableAmount"] = "可支配金额不能为空"
                } else {
                    val amountValue = amount.toDouble()
                    if (amountValue < 0) {
                        errors["disposableAmount"] = "可支配金额不能为负数"
                    } else if (amountValue > 9999999.99) {
                        errors["disposableAmount"] = "可支配金额不能超过9,999,999.99"
                    } else {
                        errors.remove("disposableAmount")
                    }
                }
            } catch (e: NumberFormatException) {
                errors["disposableAmount"] = "请输入有效的金额"
            }
            
            currentState.copy(
                disposableAmountInput = amount,
                inputErrors = errors
            )
        }
    }
    
    /**
     * 创建新的预算周期
     *
     * @param disposableAmount 可支配金额
     */
    fun createBudgetPeriod(disposableAmount: Double) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }

                val result = budgetRepository.createBudgetPeriod(
                    disposableAmount = disposableAmount,
                    paydayDate = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000) // 30天后
                )

                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showResetBudgetDialog = false,
                            disposableAmountInput = "",
                            successMessage = "预算周期创建成功"
                        )
                    }

                    // 重新加载数据
                    loadInitialData()
                } else {
                    val error = result.exceptionOrNull()
                    handleError(error ?: Exception("创建预算周期失败"), "创建预算周期失败")
                }

            } catch (e: Exception) {
                handleError(e, "创建预算周期失败")
            }
        }
    }
    
    // ==================== 支出管理 ====================
    
    /**
     * 显示添加支出对话框
     */
    fun showAddExpenseDialog() {
        _uiState.update { it.copy(showAddExpenseDialog = true) }
    }
    
    /**
     * 隐藏添加支出对话框
     */
    fun hideAddExpenseDialog() {
        _uiState.update { 
            it.copy(
                showAddExpenseDialog = false,
                expenseDescription = "",
                expenseAmount = "",
                inputErrors = it.inputErrors - "description" - "amount"
            ) 
        }
    }
    
    /**
     * 更新支出描述输入
     * 
     * @param description 支出描述
     */
    fun updateExpenseDescription(description: String) {
        _uiState.update { currentState ->
            val errors = currentState.inputErrors.toMutableMap()
            
            // 验证描述输入
            if (description.isBlank()) {
                errors["description"] = "支出描述不能为空"
            } else if (description.length > 100) {
                errors["description"] = "支出描述不能超过100个字符"
            } else {
                errors.remove("description")
            }
            
            currentState.copy(
                expenseDescription = description,
                inputErrors = errors
            )
        }
    }
    
    /**
     * 更新支出金额输入
     * 
     * @param amount 支出金额字符串
     */
    fun updateExpenseAmount(amount: String) {
        _uiState.update { currentState ->
            val errors = currentState.inputErrors.toMutableMap()
            
            // 验证金额输入
            try {
                if (amount.isBlank()) {
                    errors["amount"] = "支出金额不能为空"
                } else {
                    val amountValue = amount.toDouble()
                    if (amountValue <= 0) {
                        errors["amount"] = "支出金额必须大于0"
                    } else if (amountValue > 999999.99) {
                        errors["amount"] = "支出金额不能超过999,999.99"
                    } else {
                        errors.remove("amount")
                    }
                }
            } catch (e: NumberFormatException) {
                errors["amount"] = "请输入有效的金额"
            }
            
            currentState.copy(
                expenseAmount = amount,
                inputErrors = errors
            )
        }
    }

    /**
     * 添加支出
     *
     * @param description 支出描述
     * @param amount 支出金额
     */
    fun addExpense(description: String, amount: Double) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }

                val result = budgetRepository.addExpense(description, amount)

                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showAddExpenseDialog = false,
                            expenseDescription = "",
                            expenseAmount = "",
                            inputErrors = emptyMap(),
                            successMessage = "支出添加成功"
                        )
                    }
                } else {
                    val error = result.exceptionOrNull()
                    handleError(error ?: Exception("添加支出失败"), "添加支出失败")
                }

            } catch (e: Exception) {
                handleError(e, "添加支出失败")
            }
        }
    }
    
    /**
     * 显示删除确认对话框
     * 
     * @param expenseId 要删除的支出ID
     */
    fun showDeleteConfirmDialog(expenseId: Long) {
        _uiState.update { 
            it.copy(
                showDeleteConfirmDialog = true,
                expenseToDelete = expenseId
            ) 
        }
    }
    
    /**
     * 隐藏删除确认对话框
     */
    fun hideDeleteConfirmDialog() {
        _uiState.update { 
            it.copy(
                showDeleteConfirmDialog = false,
                expenseToDelete = null
            ) 
        }
    }
    
    /**
     * 删除支出
     * 
     * @param expenseId 要删除的支出ID
     */
    fun deleteExpense(expenseId: Long) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                
                val result = budgetRepository.deleteExpenseById(expenseId)
                
                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showDeleteConfirmDialog = false,
                            expenseToDelete = null,
                            successMessage = "支出删除成功"
                        )
                    }
                } else {
                    val error = result.exceptionOrNull()
                    handleError(error ?: Exception("删除支出失败"), "删除支出失败")
                }
                
            } catch (e: Exception) {
                handleError(e, "删除支出失败")
            }
        }
    }
    
    // ==================== 刷新和重试 ====================
    
    /**
     * 刷新数据
     */
    fun refreshData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                loadInitialData()
            } catch (e: Exception) {
                handleError(e, "刷新数据失败")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    /**
     * 重试操作
     */
    fun retryOperation() {
        refreshData()
    }
    
    // ==================== 消息处理 ====================
    
    /**
     * 清除错误消息
     */
    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    /**
     * 清除成功消息
     */
    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
    
    /**
     * 清除所有消息
     */
    fun clearAllMessages() {
        _uiState.update { 
            it.copy(
                errorMessage = null,
                successMessage = null
            ) 
        }
    }
    
    // ==================== 错误处理 ====================
    
    /**
     * 处理错误
     * 
     * @param error 错误对象
     * @param message 错误消息前缀
     */
    private fun handleError(error: Throwable?, message: String) {
        val errorMessage = if (error != null) {
            "$message: ${error.message}"
        } else {
            message
        }
        
        _uiState.update { 
            it.copy(
                isLoading = false,
                errorMessage = errorMessage
            ) 
        }
    }
}
