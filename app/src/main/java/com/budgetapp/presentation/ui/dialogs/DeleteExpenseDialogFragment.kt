package com.budgetapp.presentation.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.budgetapp.R
import com.budgetapp.presentation.viewmodel.BudgetViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

/**
 * DeleteExpenseDialogFragment
 * 
 * 删除支出确认对话框，提供删除确认功能。
 * 显示要删除的支出信息并要求用户确认。
 * 
 * Requirements:
 * - 5.3: 支出删除功能
 * - 5.4: 删除确认对话框
 */
@AndroidEntryPoint
class DeleteExpenseDialogFragment : DialogFragment() {
    
    private val viewModel: BudgetViewModel by activityViewModels()
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val uiState = viewModel.uiState.value
        val expenseToDelete = uiState.expenseToDelete
        
        // 查找要删除的支出
        val expense = uiState.expenses.find { it.id == expenseToDelete }
        val expenseDescription = expense?.description ?: "此支出"
        val expenseAmount = expense?.amount ?: 0.0
        
        val message = getString(
            R.string.delete_expense_message_detailed,
            expenseDescription,
            expenseAmount
        )
        
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirm_delete)
            .setMessage(message)
            .setPositiveButton(R.string.delete) { _, _ ->
                expenseToDelete?.let { id ->
                    viewModel.deleteExpense(id)
                }
                viewModel.hideDeleteConfirmDialog()
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                viewModel.hideDeleteConfirmDialog()
            }
            .create()
    }
    
    companion object {
        const val TAG = "DeleteExpenseDialogFragment"
        
        /**
         * 创建新的对话框实例
         */
        fun newInstance(): DeleteExpenseDialogFragment {
            return DeleteExpenseDialogFragment()
        }
    }
}
