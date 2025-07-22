package com.budgetapp.presentation.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.budgetapp.databinding.FragmentBudgetMainBinding
import com.budgetapp.presentation.state.BudgetUiState
import com.budgetapp.presentation.ui.adapters.ExpenseAdapter
import com.budgetapp.presentation.ui.dialogs.AddExpenseDialogFragment
import com.budgetapp.presentation.ui.dialogs.DeleteExpenseDialogFragment
import com.budgetapp.presentation.ui.dialogs.ResetBudgetDialogFragment
import com.budgetapp.presentation.viewmodel.BudgetViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * BudgetMainFragment
 * 
 * 预算应用的主界面Fragment，显示预算信息和操作按钮。
 * 包含金额显示、发薪日倒计时、支出列表等核心功能。
 * 
 * Requirements:
 * - 1.1: 显示当前剩余可支配金额
 * - 3.1: 大号数字显示，类似计算器显示屏
 * - 3.3: 剩余金额颜色状态：正数绿色，负数红色
 * - 4.1: 显示距离发薪日的剩余天数
 * - 4.4: 颜色提醒：少于7天显示橙色
 * - 5.1: 显示支出列表
 * - 5.5: 空列表状态的提示信息
 */
@AndroidEntryPoint
class BudgetMainFragment : Fragment() {
    
    private var _binding: FragmentBudgetMainBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BudgetViewModel by activityViewModels()
    private lateinit var expenseAdapter: ExpenseAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetMainBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        observeViewModel()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    /**
     * 设置UI组件
     */
    private fun setupUI() {
        // 初始化支出适配器
        expenseAdapter = ExpenseAdapter(
            onExpenseClick = { expense ->
                // 点击支出项目，可以显示详情或编辑
                // TODO: 在后续版本中实现支出详情功能
            },
            onExpenseLongClick = { expense ->
                // 长按支出项目，显示删除确认对话框
                viewModel.showDeleteConfirmDialog(expense.id)
            }
        )

        // 设置RecyclerView
        binding.recyclerViewExpenses.adapter = expenseAdapter

        // 设置添加支出按钮点击事件
        binding.fabAddExpense.setOnClickListener {
            viewModel.showAddExpenseDialog()
        }

        // 设置设置预算按钮点击事件
        binding.btnSetupBudget.setOnClickListener {
            viewModel.showResetBudgetDialog()
        }

        // 设置刷新按钮点击事件
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshData()
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
     *
     * @param uiState UI状态
     */
    private fun updateUI(uiState: BudgetUiState) {
        // 更新加载状态
        binding.swipeRefreshLayout.isRefreshing = uiState.isLoading

        if (uiState.showEmptyState) {
            // 显示空状态
            showEmptyState()
        } else if (uiState.hasActiveBudgetPeriod) {
            // 显示预算数据
            showBudgetData(uiState)
        }

        // 处理对话框显示
        handleDialogs(uiState)
    }

    /**
     * 处理对话框显示
     *
     * @param uiState UI状态
     */
    private fun handleDialogs(uiState: BudgetUiState) {
        // 显示添加支出对话框
        if (uiState.showAddExpenseDialog) {
            val existingDialog = parentFragmentManager.findFragmentByTag(AddExpenseDialogFragment.TAG)
            if (existingDialog == null) {
                val dialog = AddExpenseDialogFragment.newInstance()
                dialog.show(parentFragmentManager, AddExpenseDialogFragment.TAG)
            }
        }

        // 显示删除确认对话框
        if (uiState.showDeleteConfirmDialog) {
            val existingDialog = parentFragmentManager.findFragmentByTag(DeleteExpenseDialogFragment.TAG)
            if (existingDialog == null) {
                val dialog = DeleteExpenseDialogFragment.newInstance()
                dialog.show(parentFragmentManager, DeleteExpenseDialogFragment.TAG)
            }
        }

        // 显示重置预算对话框
        if (uiState.showResetBudgetDialog) {
            val existingDialog = parentFragmentManager.findFragmentByTag(ResetBudgetDialogFragment.TAG)
            if (existingDialog == null) {
                val dialog = ResetBudgetDialogFragment.newInstance()
                dialog.show(parentFragmentManager, ResetBudgetDialogFragment.TAG)
            }
        }
    }
    
    /**
     * 显示空状态
     */
    private fun showEmptyState() {
        binding.apply {
            // 隐藏主要内容
            layoutBudgetContent.visibility = View.GONE
            fabAddExpense.visibility = View.GONE
            
            // 显示空状态
            layoutEmptyState.visibility = View.VISIBLE
        }
    }
    
    /**
     * 显示预算数据
     * 
     * @param uiState UI状态
     */
    private fun showBudgetData(uiState: BudgetUiState) {
        binding.apply {
            // 显示主要内容
            layoutBudgetContent.visibility = View.VISIBLE
            fabAddExpense.visibility = View.VISIBLE
            
            // 隐藏空状态
            layoutEmptyState.visibility = View.GONE
            
            // 更新剩余金额显示
            updateRemainingAmountDisplay(uiState)
            
            // 更新发薪日倒计时
            updatePaydayCountdown(uiState)
            
            // 更新支出列表
            updateExpensesList(uiState)
        }
    }
    
    /**
     * 更新剩余金额显示
     * 
     * @param uiState UI状态
     */
    private fun updateRemainingAmountDisplay(uiState: BudgetUiState) {
        binding.apply {
            // 格式化金额显示
            val amountText = "¥%.2f".format(uiState.remainingAmount)
            tvRemainingAmount.text = amountText
            
            // 设置金额颜色
            val colorRes = when {
                uiState.isOverBudget -> com.budgetapp.R.color.negative_amount
                uiState.isNearPayday && uiState.remainingAmount < 100 -> com.budgetapp.R.color.warning_amount
                else -> com.budgetapp.R.color.positive_amount
            }
            tvRemainingAmount.setTextColor(requireContext().getColor(colorRes))
            
            // 显示可支配金额
            uiState.currentBudgetPeriod?.let { period ->
                val disposableText = "可支配金额：¥%.2f".format(period.disposableAmount)
                tvDisposableAmount.text = disposableText
            }
            
            // 显示支出总额
            val totalExpensesText = "已支出：¥%.2f".format(uiState.totalExpenses)
            tvTotalExpenses.text = totalExpensesText
            
            // 显示超支提示
            if (uiState.isOverBudget) {
                tvOverspendWarning.visibility = View.VISIBLE
                val overspendText = "超支：¥%.2f".format(uiState.overspendAmount)
                tvOverspendWarning.text = overspendText
            } else {
                tvOverspendWarning.visibility = View.GONE
            }
        }
    }
    
    /**
     * 更新发薪日倒计时
     * 
     * @param uiState UI状态
     */
    private fun updatePaydayCountdown(uiState: BudgetUiState) {
        binding.apply {
            val daysText = "${uiState.daysUntilPayday}天"
            tvDaysUntilPayday.text = daysText
            
            // 设置倒计时颜色
            val colorRes = if (uiState.isNearPayday) {
                com.budgetapp.R.color.warning_amount
            } else {
                com.budgetapp.R.color.md_theme_on_surface
            }
            tvDaysUntilPayday.setTextColor(requireContext().getColor(colorRes))
            
            // 更新进度条
            progressBudgetPeriod.progress = (uiState.budgetPeriodProgress * 100).toInt()
        }
    }
    
    /**
     * 更新支出列表
     *
     * @param uiState UI状态
     */
    private fun updateExpensesList(uiState: BudgetUiState) {
        binding.apply {
            if (uiState.showExpenseEmptyState) {
                // 显示支出空状态
                layoutExpenseEmpty.visibility = View.VISIBLE
                recyclerViewExpenses.visibility = View.GONE
            } else {
                // 显示支出列表
                layoutExpenseEmpty.visibility = View.GONE
                recyclerViewExpenses.visibility = View.VISIBLE

                // 更新适配器数据
                expenseAdapter.submitList(uiState.expenses)
            }
        }
    }
}
