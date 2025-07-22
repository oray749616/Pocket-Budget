package com.budgetapp.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Expense实体类
 * 
 * 表示用户的一笔计划支出，包含支出描述、金额、创建日期等信息。
 * 每个支出都关联到一个特定的预算周期，当预算周期被删除时，相关支出也会被级联删除。
 * 
 * Requirements:
 * - 2.2: 验证支出输入的完整性和有效性
 * - 5.2: 支持显示支出项目的描述、金额和添加日期
 * - 6.2: 立即将支出数据保存到本地SQLite数据库
 */
@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = BudgetPeriod::class,
            parentColumns = ["id"],
            childColumns = ["budget_period_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["budget_period_id"]),
        Index(value = ["created_date"])
    ]
)
data class Expense(
    /**
     * 主键ID，自动生成
     */
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /**
     * 关联的预算周期ID
     * 外键关联到BudgetPeriod表的id字段
     */
    @ColumnInfo(name = "budget_period_id")
    val budgetPeriodId: Long,
    
    /**
     * 支出描述
     * 用户输入的支出项目描述，不能为空
     */
    @ColumnInfo(name = "description")
    val description: String,
    
    /**
     * 支出金额
     * 支出的具体金额，必须为正数
     */
    @ColumnInfo(name = "amount")
    val amount: Double,
    
    /**
     * 创建日期
     * 支出记录创建的时间戳（毫秒）
     */
    @ColumnInfo(name = "created_date")
    val createdDate: Long = System.currentTimeMillis()
) {
    
    /**
     * 验证支出数据的完整性和有效性
     * 
     * @return true 如果数据有效，false 如果数据无效
     */
    fun isValid(): Boolean {
        return description.isNotBlank() && 
               amount > 0 && 
               budgetPeriodId > 0 &&
               createdDate > 0
    }
    
    /**
     * 检查是否为大额支出
     * 
     * @param threshold 大额支出的阈值，默认1000元
     * @return true 如果是大额支出
     */
    fun isLargeExpense(threshold: Double = LARGE_EXPENSE_THRESHOLD): Boolean {
        return amount >= threshold
    }
    
    /**
     * 获取格式化的支出描述
     * 限制描述长度，避免UI显示问题
     * 
     * @param maxLength 最大长度，默认50个字符
     * @return 格式化后的描述
     */
    fun getFormattedDescription(maxLength: Int = MAX_DESCRIPTION_LENGTH): String {
        return if (description.length <= maxLength) {
            description
        } else {
            description.take(maxLength - 3) + "..."
        }
    }
    
    /**
     * 获取支出的年龄（天数）
     * 
     * @return 从创建到现在的天数
     */
    fun getAgeInDays(): Int {
        val currentTime = System.currentTimeMillis()
        val ageInMillis = currentTime - createdDate
        return (ageInMillis / (24 * 60 * 60 * 1000)).toInt()
    }
    
    /**
     * 检查是否为今日创建的支出
     * 
     * @return true 如果是今日创建
     */
    fun isCreatedToday(): Boolean {
        val currentTime = System.currentTimeMillis()
        val oneDayInMillis = 24 * 60 * 60 * 1000L
        val todayStart = currentTime - (currentTime % oneDayInMillis)
        return createdDate >= todayStart
    }
    
    companion object {
        /**
         * 最大支出描述长度
         */
        const val MAX_DESCRIPTION_LENGTH = 50
        
        /**
         * 大额支出阈值
         */
        const val LARGE_EXPENSE_THRESHOLD = 1000.0
        
        /**
         * 支出描述的最小长度
         */
        const val MIN_DESCRIPTION_LENGTH = 1
        
        /**
         * 创建新的支出记录
         * 
         * @param budgetPeriodId 关联的预算周期ID
         * @param description 支出描述
         * @param amount 支出金额
         * @return 新的Expense实例，如果参数无效则返回null
         */
        fun create(
            budgetPeriodId: Long,
            description: String,
            amount: Double
        ): Expense? {
            // 验证输入参数
            if (budgetPeriodId <= 0 || 
                description.isBlank() || 
                amount <= 0) {
                return null
            }
            
            return Expense(
                budgetPeriodId = budgetPeriodId,
                description = description.trim(),
                amount = amount,
                createdDate = System.currentTimeMillis()
            )
        }
        
        /**
         * 验证支出描述是否有效
         * 
         * @param description 待验证的描述
         * @return true 如果描述有效
         */
        fun isValidDescription(description: String): Boolean {
            return description.isNotBlank() && 
                   description.trim().length >= MIN_DESCRIPTION_LENGTH &&
                   description.length <= MAX_DESCRIPTION_LENGTH
        }
        
        /**
         * 验证支出金额是否有效
         * 
         * @param amount 待验证的金额
         * @return true 如果金额有效
         */
        fun isValidAmount(amount: Double): Boolean {
            return amount > 0 && amount.isFinite()
        }
    }
}
