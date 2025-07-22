package com.budgetapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetapp.domain.usecase.AddExpenseUseCase
import com.budgetapp.domain.usecase.CalculateDaysUntilPaydayUseCase
import com.budgetapp.domain.usecase.CalculateRemainingAmountUseCase
import com.budgetapp.domain.usecase.DeleteExpenseUseCase
import com.budgetapp.domain.repository.BudgetRepository
import com.budgetapp.presentation.state.BudgetEvent
import com.budgetapp.presentation.state.BudgetUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BudgetViewModel
 * 
 * 预算应用的主要ViewModel，管理UI状态和业务逻辑。
 * 使用MVVM架构模式，通过StateFlow和SharedFlow管理状态和事件。
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
    private val budgetRepository: BudgetRepository,
    private val calculateRemainingAmountUseCase: CalculateRemainingAmountUseCase,
    private val calculateDaysUntilPaydayUseCase: CalculateDaysUntilPaydayUseCase,
    private val addExpenseUseCase: AddExpenseUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase
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
    
    /**
     * 一次性事件流
     * 用于处理导航、提示等不需要保存在状态中的事件
     */
    private val _events = MutableSharedFlow<BudgetEvent>()
    val events: SharedFlow<BudgetEvent> = _events.asSharedFlow()
    
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
                
                // 加载当前预算周期和支出数据
                loadBudgetData()
                
            } catch (e: Exception) {
                handleError(e, "加载数据失败")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    /**
     * 加载预算数据
     */
    private suspend fun loadBudgetData() {
        try {
            // 这里会在后续实现具体的数据加载逻辑
            // 当前只是基础结构
            
        } catch (e: Exception) {
            handleError(e, "加载预算数据失败")
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
                            currentBudgetPeriod = budgetPeriod,
                            showEmptyState = budgetPeriod == null
                        )
                    }
                }
        }
        
        viewModelScope.launch {
            // 监听当前周期支出变化
            budgetRepository.getExpensesForCurrentPeriod()
                .catch { e -> handleError(e, "监听支出数据失败") }
                .collect { expenses ->
                    val totalExpenses = expenses.sumOf { it.amount }
                    _uiState.update { currentState ->
                        currentState.copy(
                            expenses = expenses,
                            totalExpenses = totalExpenses,
                            hasExpenses = expenses.isNotEmpty()
                        )
                    }
                }
        }
        
        viewModelScope.launch {
            // 监听剩余金额变化
            calculateRemainingAmountUseCase()
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
        
        viewModelScope.launch {
            // 监听发薪日倒计时变化
            calculateDaysUntilPaydayUseCase()
                .catch { e -> handleError(e, "计算发薪日倒计时失败") }
                .collect { daysUntilPayday ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            daysUntilPayday = daysUntilPayday,
                            isNearPayday = daysUntilPayday in 1..7
                        )
                    }
                }
        }
    }
    
    // ==================== 错误处理 ====================
    
    /**
     * 处理错误
     * 
     * @param error 异常对象
     * @param defaultMessage 默认错误消息
     */
    private fun handleError(error: Throwable, defaultMessage: String = "操作失败") {
        val errorMessage = error.message ?: defaultMessage
        
        _uiState.update { currentState ->
            currentState.copy(
                isLoading = false,
                errorMessage = errorMessage
            )
        }
        
        // 发送错误事件
        viewModelScope.launch {
            _events.emit(BudgetEvent.ShowError(errorMessage))
        }
    }
    
    /**
     * 清除错误消息
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    /**
     * 清除成功消息
     */
    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }
    
    // ==================== UI交互方法 ====================
    
    /**
     * 显示添加支出对话框
     */
    fun showAddExpenseDialog() {
        _uiState.update { 
            it.copy(
                showAddExpenseDialog = true,
                expenseDescription = "",
                expenseAmount = "",
                inputErrors = emptyMap()
            ) 
        }
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
                inputErrors = emptyMap()
            ) 
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
     * 显示重置预算周期对话框
     */
    fun showResetBudgetDialog() {
        _uiState.update { 
            it.copy(
                showResetBudgetDialog = true,
                disposableAmountInput = ""
            ) 
        }
    }
    
    /**
     * 隐藏重置预算周期对话框
     */
    fun hideResetBudgetDialog() {
        _uiState.update { 
            it.copy(
                showResetBudgetDialog = false,
                disposableAmountInput = ""
            ) 
        }
    }
    
    // ==================== 输入处理方法 ====================
    
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
            } else if (description.length > 50) {
                errors["description"] = "支出描述不能超过50个字符"
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

    // ==================== 业务逻辑方法 ====================

    /**
     * 创建新的预算周期
     *
     * @param disposableAmount 可支配金额
     */
    fun createBudgetPeriod(disposableAmount: Double) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }

                val result = budgetRepository.createNewBudgetPeriod(disposableAmount)

                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showResetBudgetDialog = false,
                            disposableAmountInput = "",
                            successMessage = "预算周期创建成功"
                        )
                    }

                    _events.emit(BudgetEvent.BudgetPeriodReset)
                    _events.emit(BudgetEvent.ShowSuccess("预算周期创建成功"))

                    // 重新加载数据
                    loadBudgetData()
                } else {
                    val error = result.exceptionOrNull()
                    handleError(error ?: Exception("创建预算周期失败"), "创建预算周期失败")
                }

            } catch (e: Exception) {
                handleError(e, "创建预算周期失败")
            }
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

                // 使用带超支检查的添加方法
                val result = addExpenseUseCase.executeWithOverspendingCheck(description, amount)

                if (result.isSuccess) {
                    val addResult = result.getOrNull()!!

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

                    _events.emit(BudgetEvent.ExpenseAdded)
                    _events.emit(BudgetEvent.ShowSuccess("支出添加成功"))

                    // 如果会导致超支，显示警告
                    if (addResult.willOverspend) {
                        _events.emit(BudgetEvent.ShowOverspendWarning(addResult.overspendAmount))
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
     * 删除支出
     *
     * @param expenseId 支出ID
     */
    fun deleteExpense(expenseId: Long) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }

                val result = deleteExpenseUseCase.executeById(expenseId)

                if (result.isSuccess) {
                    val deleteResult = result.getOrNull()!!

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showDeleteConfirmDialog = false,
                            expenseToDelete = null,
                            successMessage = "支出删除成功"
                        )
                    }

                    val expenseDescription = deleteResult.deletedExpense.description
                    _events.emit(BudgetEvent.ExpenseDeleted(expenseDescription))
                    _events.emit(BudgetEvent.ShowSuccess("支出删除成功"))

                } else {
                    val error = result.exceptionOrNull()
                    handleError(error ?: Exception("删除支出失败"), "删除支出失败")
                }

            } catch (e: Exception) {
                handleError(e, "删除支出失败")
            }
        }
    }

    /**
     * 重置预算周期
     *
     * @param newDisposableAmount 新的可支配金额
     */
    fun resetBudgetPeriod(newDisposableAmount: Double) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }

                // 先重置当前周期
                val resetResult = budgetRepository.resetBudgetPeriod()

                if (resetResult.isSuccess) {
                    // 创建新的预算周期
                    createBudgetPeriod(newDisposableAmount)
                } else {
                    val error = resetResult.exceptionOrNull()
                    handleError(error ?: Exception("重置预算周期失败"), "重置预算周期失败")
                }

            } catch (e: Exception) {
                handleError(e, "重置预算周期失败")
            }
        }
    }

    /**
     * 刷新数据
     */
    fun refreshData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                loadBudgetData()
            } catch (e: Exception) {
                handleError(e, "刷新数据失败")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
