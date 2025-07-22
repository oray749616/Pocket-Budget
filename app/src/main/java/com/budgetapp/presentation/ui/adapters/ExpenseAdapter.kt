package com.budgetapp.presentation.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.budgetapp.data.database.entities.Expense
import com.budgetapp.databinding.ItemExpenseBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * ExpenseAdapter
 * 
 * 支出列表的RecyclerView适配器，使用ListAdapter和DiffUtil实现高效的列表更新。
 * 支持支出项目的显示、点击和长按删除功能。
 * 
 * Requirements:
 * - 5.1: 显示支出列表
 * - 5.2: 显示支出描述、金额和添加日期
 * - 5.3: 支持支出删除功能
 * - 5.5: 空列表状态的提示信息
 */
class ExpenseAdapter(
    private val onExpenseClick: (Expense) -> Unit = {},
    private val onExpenseLongClick: (Expense) -> Unit = {}
) : ListAdapter<Expense, ExpenseAdapter.ExpenseViewHolder>(ExpenseDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ItemExpenseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ExpenseViewHolder(binding, onExpenseClick, onExpenseLongClick)
    }
    
    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    /**
     * ExpenseViewHolder
     * 
     * 支出项目的ViewHolder，负责绑定数据和处理用户交互。
     */
    class ExpenseViewHolder(
        private val binding: ItemExpenseBinding,
        private val onExpenseClick: (Expense) -> Unit,
        private val onExpenseLongClick: (Expense) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private var currentExpense: Expense? = null
        private var isDeleteButtonVisible = false
        
        init {
            // 设置点击事件
            binding.root.setOnClickListener {
                currentExpense?.let { expense ->
                    if (isDeleteButtonVisible) {
                        hideDeleteButton()
                    } else {
                        onExpenseClick(expense)
                    }
                }
            }
            
            // 设置长按事件
            binding.root.setOnLongClickListener {
                currentExpense?.let { expense ->
                    if (!isDeleteButtonVisible) {
                        showDeleteButton()
                        onExpenseLongClick(expense)
                    }
                }
                true
            }
            
            // 设置删除按钮点击事件
            binding.btnDeleteExpense.setOnClickListener {
                currentExpense?.let { expense ->
                    onExpenseLongClick(expense)
                }
            }
        }
        
        /**
         * 绑定支出数据
         * 
         * @param expense 支出对象
         */
        fun bind(expense: Expense) {
            currentExpense = expense
            
            binding.apply {
                // 设置支出描述
                tvExpenseDescription.text = expense.description
                
                // 设置支出金额
                val amountText = "¥%.2f".format(expense.amount)
                tvExpenseAmount.text = amountText
                
                // 设置支出日期
                val dateText = formatExpenseDate(expense.createdDate)
                tvExpenseDate.text = dateText
                
                // 重置删除按钮状态
                hideDeleteButton()
            }
        }
        
        /**
         * 显示删除按钮
         */
        private fun showDeleteButton() {
            isDeleteButtonVisible = true
            binding.btnDeleteExpense.visibility = View.VISIBLE
        }
        
        /**
         * 隐藏删除按钮
         */
        fun hideDeleteButton() {
            isDeleteButtonVisible = false
            binding.btnDeleteExpense.visibility = View.GONE
        }
        
        /**
         * 格式化支出日期显示
         * 
         * @param timestamp 时间戳
         * @return 格式化的日期字符串
         */
        private fun formatExpenseDate(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            
            return when {
                // 今天
                diff < 24 * 60 * 60 * 1000 -> {
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    "今天 ${timeFormat.format(Date(timestamp))}"
                }
                // 昨天
                diff < 2 * 24 * 60 * 60 * 1000 -> {
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    "昨天 ${timeFormat.format(Date(timestamp))}"
                }
                // 本周内
                diff < 7 * 24 * 60 * 60 * 1000 -> {
                    val dayFormat = SimpleDateFormat("EEEE HH:mm", Locale.getDefault())
                    dayFormat.format(Date(timestamp))
                }
                // 更早
                else -> {
                    val dateFormat = SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault())
                    dateFormat.format(Date(timestamp))
                }
            }
        }
    }
    
    /**
     * ExpenseDiffCallback
     * 
     * 支出列表的DiffUtil回调，用于高效计算列表变化。
     */
    class ExpenseDiffCallback : DiffUtil.ItemCallback<Expense>() {
        
        override fun areItemsTheSame(oldItem: Expense, newItem: Expense): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Expense, newItem: Expense): Boolean {
            return oldItem == newItem
        }
    }
    
    /**
     * 隐藏所有删除按钮
     * 当用户点击其他地方时调用
     */
    fun hideAllDeleteButtons() {
        for (i in 0 until itemCount) {
            val viewHolder = recyclerView?.findViewHolderForAdapterPosition(i) as? ExpenseViewHolder
            viewHolder?.hideDeleteButton()
        }
    }
    
    private var recyclerView: RecyclerView? = null
    
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }
    
    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView = null
    }
}
