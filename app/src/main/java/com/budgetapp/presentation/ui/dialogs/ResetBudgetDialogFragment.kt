package com.budgetapp.presentation.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.budgetapp.databinding.DialogResetBudgetBinding
import com.budgetapp.presentation.viewmodel.BudgetViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * ResetBudgetDialogFragment
 * 
 * 重置预算周期的对话框Fragment，提供新的可支配金额输入界面。
 * 包含输入验证、错误提示和重置确认功能。
 * 
 * Requirements:
 * - 8.1: 预算周期重置功能
 * - 8.2: 重置确认对话框
 * - 8.3: 清除当前支出记录
 * - 8.4: 输入新的可支配金额
 */
@AndroidEntryPoint
class ResetBudgetDialogFragment : DialogFragment() {
    
    private var _binding: DialogResetBudgetBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: BudgetViewModel by activityViewModels()
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogResetBudgetBinding.inflate(layoutInflater)
        
        setupUI()
        observeViewModel()
        
        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    /**
     * 设置UI组件
     */
    private fun setupUI() {
        // 设置文本变化监听器
        binding.etNewDisposableAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.updateDisposableAmount(s?.toString() ?: "")
            }
        })
        
        // 设置按钮点击事件
        binding.btnCancel.setOnClickListener {
            viewModel.hideResetBudgetDialog()
            dismiss()
        }
        
        binding.btnReset.setOnClickListener {
            resetBudget()
        }
    }
    
    /**
     * 观察ViewModel状态
     */
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    updateUI(uiState)
                }
            }
        }
    }
    
    /**
     * 更新UI状态
     */
    private fun updateUI(uiState: com.budgetapp.presentation.state.BudgetUiState) {
        binding.apply {
            // 更新输入内容
            if (etNewDisposableAmount.text.toString() != uiState.disposableAmountInput) {
                etNewDisposableAmount.setText(uiState.disposableAmountInput)
                etNewDisposableAmount.setSelection(uiState.disposableAmountInput.length)
            }
            
            // 更新错误信息
            tilNewDisposableAmount.error = uiState.inputErrors["disposableAmount"]
            
            // 更新重置按钮状态
            val canReset = uiState.disposableAmountInput.isNotBlank() && 
                          !uiState.inputErrors.containsKey("disposableAmount") &&
                          !uiState.isLoading
            btnReset.isEnabled = canReset
            
            // 如果对话框应该关闭
            if (!uiState.showResetBudgetDialog && isVisible) {
                dismiss()
            }
        }
    }
    
    /**
     * 重置预算
     */
    private fun resetBudget() {
        val uiState = viewModel.uiState.value
        
        if (uiState.disposableAmountInput.isNotBlank() && 
            !uiState.inputErrors.containsKey("disposableAmount")) {
            try {
                val newAmount = uiState.disposableAmountInput.toDouble()
                viewModel.createBudgetPeriod(newAmount)
            } catch (e: NumberFormatException) {
                // 金额格式错误，这种情况应该已经被输入验证捕获
                binding.tilNewDisposableAmount.error = "请输入有效的金额"
            }
        }
    }
    
    companion object {
        const val TAG = "ResetBudgetDialogFragment"
        
        /**
         * 创建新的对话框实例
         */
        fun newInstance(): ResetBudgetDialogFragment {
            return ResetBudgetDialogFragment()
        }
    }
}
