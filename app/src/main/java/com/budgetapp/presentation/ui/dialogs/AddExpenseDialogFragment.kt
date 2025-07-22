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
import com.budgetapp.R
import com.budgetapp.databinding.DialogAddExpenseBinding
import com.budgetapp.presentation.viewmodel.BudgetViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * AddExpenseDialogFragment
 * 
 * 添加支出的对话框Fragment，提供支出描述和金额的输入界面。
 * 包含输入验证、错误提示和保存功能。
 * 
 * Requirements:
 * - 2.1: 支出添加的主要入口
 * - 2.2: 支出描述和金额输入
 * - 2.3: 输入验证和错误提示
 */
@AndroidEntryPoint
class AddExpenseDialogFragment : DialogFragment() {
    
    private var _binding: DialogAddExpenseBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: BudgetViewModel by activityViewModels()
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddExpenseBinding.inflate(layoutInflater)
        
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
        binding.etExpenseDescription.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.updateExpenseDescription(s?.toString() ?: "")
            }
        })
        
        binding.etExpenseAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.updateExpenseAmount(s?.toString() ?: "")
            }
        })
        
        // 设置按钮点击事件
        binding.btnCancel.setOnClickListener {
            viewModel.hideAddExpenseDialog()
            dismiss()
        }
        
        binding.btnSave.setOnClickListener {
            saveExpense()
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
            if (etExpenseDescription.text.toString() != uiState.expenseDescription) {
                etExpenseDescription.setText(uiState.expenseDescription)
                etExpenseDescription.setSelection(uiState.expenseDescription.length)
            }
            
            if (etExpenseAmount.text.toString() != uiState.expenseAmount) {
                etExpenseAmount.setText(uiState.expenseAmount)
                etExpenseAmount.setSelection(uiState.expenseAmount.length)
            }
            
            // 更新错误信息
            tilExpenseDescription.error = uiState.inputErrors["description"]
            tilExpenseAmount.error = uiState.inputErrors["amount"]
            
            // 更新保存按钮状态
            btnSave.isEnabled = uiState.canAddExpense && !uiState.isLoading
            
            // 如果对话框应该关闭
            if (!uiState.showAddExpenseDialog && isVisible) {
                dismiss()
            }
        }
    }
    
    /**
     * 保存支出
     */
    private fun saveExpense() {
        val uiState = viewModel.uiState.value
        
        if (uiState.canAddExpense) {
            try {
                val description = uiState.expenseDescription.trim()
                val amount = uiState.expenseAmount.toDouble()
                
                viewModel.addExpense(description, amount)
            } catch (e: NumberFormatException) {
                // 金额格式错误，这种情况应该已经被输入验证捕获
                binding.tilExpenseAmount.error = "请输入有效的金额"
            }
        }
    }
    
    companion object {
        const val TAG = "AddExpenseDialogFragment"
        
        /**
         * 创建新的对话框实例
         */
        fun newInstance(): AddExpenseDialogFragment {
            return AddExpenseDialogFragment()
        }
    }
}
