package com.budgetapp.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * BudgetPeriod实体类
 * 
 * 表示一个预算周期，包含用户的可支配金额、创建日期、发薪日期等信息。
 * 每个预算周期默认为30天，用户可以在发薪日重新设置新的可支配金额。
 * 
 * Requirements:
 * - 1.2: 保存用户输入的可支配金额到SQLite数据库
 * - 4.2: 支持基于30天周期和当前日期的计算
 * - 6.2: 立即将数据保存到本地SQLite数据库
 */
@Entity(
    tableName = "budget_periods",
    indices = [Index(value = ["created_date"])]
)
data class BudgetPeriod(
    /**
     * 主键ID，自动生成
     */
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /**
     * 可支配金额
     * 用户在每个预算周期开始时输入的总可支配金额
     */
    @ColumnInfo(name = "disposable_amount")
    val disposableAmount: Double,
    
    /**
     * 创建日期
     * 预算周期创建的时间戳（毫秒）
     */
    @ColumnInfo(name = "created_date")
    val createdDate: Long = System.currentTimeMillis(),
    
    /**
     * 发薪日期
     * 下一个发薪日的时间戳（毫秒），默认为创建日期后30天
     */
    @ColumnInfo(name = "payday_date") 
    val paydayDate: Long,
    
    /**
     * 是否为活跃状态
     * 标识当前预算周期是否为活跃状态，同时只能有一个活跃的预算周期
     */
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true
) {
    
    /**
     * 验证预算周期数据的完整性
     * 
     * @return true 如果数据有效，false 如果数据无效
     */
    fun isValid(): Boolean {
        return disposableAmount >= 0 && paydayDate > createdDate
    }
    
    /**
     * 计算距离发薪日的剩余天数
     * 
     * @return 剩余天数，如果已过发薪日则返回0
     */
    fun getDaysUntilPayday(): Int {
        val currentTime = System.currentTimeMillis()
        val remainingMillis = paydayDate - currentTime
        return if (remainingMillis > 0) {
            (remainingMillis / (24 * 60 * 60 * 1000)).toInt()
        } else {
            0
        }
    }
    
    /**
     * 检查是否接近发薪日（少于7天）
     * 
     * @return true 如果距离发薪日少于7天
     */
    fun isNearPayday(): Boolean {
        return getDaysUntilPayday() < 7
    }
    
    /**
     * 检查是否已到达或超过发薪日
     * 
     * @return true 如果已到达发薪日
     */
    fun isPaydayReached(): Boolean {
        return System.currentTimeMillis() >= paydayDate
    }
    
    companion object {
        /**
         * 默认预算周期天数
         */
        const val DEFAULT_PERIOD_DAYS = 30
        
        /**
         * 发薪日提醒阈值（天数）
         */
        const val PAYDAY_REMINDER_THRESHOLD_DAYS = 7
        
        /**
         * 创建新的预算周期
         * 
         * @param disposableAmount 可支配金额
         * @param periodDays 预算周期天数，默认30天
         * @return 新的BudgetPeriod实例
         */
        fun create(
            disposableAmount: Double,
            periodDays: Int = DEFAULT_PERIOD_DAYS
        ): BudgetPeriod {
            val currentTime = System.currentTimeMillis()
            val paydayTime = currentTime + (periodDays * 24 * 60 * 60 * 1000L)
            
            return BudgetPeriod(
                disposableAmount = disposableAmount,
                createdDate = currentTime,
                paydayDate = paydayTime,
                isActive = true
            )
        }
    }
}
