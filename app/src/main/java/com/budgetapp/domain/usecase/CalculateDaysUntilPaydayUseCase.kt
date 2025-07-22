package com.budgetapp.domain.usecase

import com.budgetapp.data.database.entities.BudgetPeriod
import com.budgetapp.domain.model.Result
import com.budgetapp.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CalculateDaysUntilPaydayUseCase
 * 
 * 计算距离发薪日剩余天数的业务逻辑用例。
 * 基于30天周期和当前日期进行计算。
 * 
 * Requirements:
 * - 4.2: 基于30天周期和当前日期进行计算
 * - 4.3: 到达发薪日时提示用户输入新的可支配金额
 */
@Singleton
class CalculateDaysUntilPaydayUseCase @Inject constructor(
    private val repository: BudgetRepository
) {
    
    /**
     * 计算指定预算周期距离发薪日的剩余天数
     * 
     * @param periodId 预算周期ID
     * @return 操作结果，包含剩余天数或错误信息
     */
    suspend fun calculateForPeriod(periodId: Long): Result<Int> {
        return try {
            val periodResult = repository.getBudgetPeriodById(periodId)
            if (periodResult.isError) {
                return Result.Error(
                    periodResult.exceptionOrNull() ?: Exception("无法获取预算周期")
                )
            }
            
            val period = periodResult.getOrNull()
                ?: return Result.Error(Exception("预算周期不存在"))
            
            val daysUntilPayday = period.getDaysUntilPayday()
            Result.Success(daysUntilPayday)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * 获取当前预算周期距离发薪日的剩余天数（响应式）
     * 
     * @return 当前预算周期剩余天数的Flow
     */
    fun getCurrentPeriodDaysUntilPayday(): Flow<Int> {
        return repository.getCurrentBudgetPeriod().map { period ->
            period?.getDaysUntilPayday() ?: 0
        }
    }
    
    /**
     * 计算发薪日倒计时详细信息
     * 
     * @param periodId 预算周期ID
     * @return 包含详细倒计时信息的结果
     */
    suspend fun calculatePaydayDetails(periodId: Long): Result<PaydayDetails> {
        return try {
            val periodResult = repository.getBudgetPeriodById(periodId)
            if (periodResult.isError) {
                return Result.Error(
                    periodResult.exceptionOrNull() ?: Exception("无法获取预算周期")
                )
            }
            
            val period = periodResult.getOrNull()
                ?: return Result.Error(Exception("预算周期不存在"))
            
            val currentTime = System.currentTimeMillis()
            val paydayTime = period.paydayDate
            val remainingMillis = paydayTime - currentTime
            
            val details = if (remainingMillis > 0) {
                val days = TimeUnit.MILLISECONDS.toDays(remainingMillis).toInt()
                val hours = TimeUnit.MILLISECONDS.toHours(remainingMillis % TimeUnit.DAYS.toMillis(1)).toInt()
                val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMillis % TimeUnit.HOURS.toMillis(1)).toInt()
                
                PaydayDetails(
                    daysUntilPayday = days,
                    hoursUntilPayday = hours,
                    minutesUntilPayday = minutes,
                    isPaydayReached = false,
                    isNearPayday = period.isNearPayday(),
                    paydayDate = paydayTime,
                    totalPeriodDays = calculateTotalPeriodDays(period),
                    elapsedDays = calculateElapsedDays(period)
                )
            } else {
                PaydayDetails(
                    daysUntilPayday = 0,
                    hoursUntilPayday = 0,
                    minutesUntilPayday = 0,
                    isPaydayReached = true,
                    isNearPayday = false,
                    paydayDate = paydayTime,
                    totalPeriodDays = calculateTotalPeriodDays(period),
                    elapsedDays = calculateTotalPeriodDays(period)
                )
            }
            
            Result.Success(details)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * 获取当前预算周期的发薪日详细信息（响应式）
     * 
     * @return 当前预算周期发薪日详细信息的Flow
     */
    fun getCurrentPeriodPaydayDetails(): Flow<PaydayDetails?> {
        return repository.getCurrentBudgetPeriod().map { period ->
            period?.let { budgetPeriod ->
                val currentTime = System.currentTimeMillis()
                val paydayTime = budgetPeriod.paydayDate
                val remainingMillis = paydayTime - currentTime
                
                if (remainingMillis > 0) {
                    val days = TimeUnit.MILLISECONDS.toDays(remainingMillis).toInt()
                    val hours = TimeUnit.MILLISECONDS.toHours(remainingMillis % TimeUnit.DAYS.toMillis(1)).toInt()
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMillis % TimeUnit.HOURS.toMillis(1)).toInt()
                    
                    PaydayDetails(
                        daysUntilPayday = days,
                        hoursUntilPayday = hours,
                        minutesUntilPayday = minutes,
                        isPaydayReached = false,
                        isNearPayday = budgetPeriod.isNearPayday(),
                        paydayDate = paydayTime,
                        totalPeriodDays = calculateTotalPeriodDays(budgetPeriod),
                        elapsedDays = calculateElapsedDays(budgetPeriod)
                    )
                } else {
                    PaydayDetails(
                        daysUntilPayday = 0,
                        hoursUntilPayday = 0,
                        minutesUntilPayday = 0,
                        isPaydayReached = true,
                        isNearPayday = false,
                        paydayDate = paydayTime,
                        totalPeriodDays = calculateTotalPeriodDays(budgetPeriod),
                        elapsedDays = calculateTotalPeriodDays(budgetPeriod)
                    )
                }
            }
        }
    }
    
    /**
     * 检查是否需要提醒用户发薪日即将到来
     * 
     * @param periodId 预算周期ID
     * @return 是否需要提醒
     */
    suspend fun shouldShowPaydayReminder(periodId: Long): Result<Boolean> {
        return try {
            val periodResult = repository.getBudgetPeriodById(periodId)
            if (periodResult.isError) {
                return Result.Error(
                    periodResult.exceptionOrNull() ?: Exception("无法获取预算周期")
                )
            }
            
            val period = periodResult.getOrNull()
                ?: return Result.Error(Exception("预算周期不存在"))
            
            val shouldRemind = period.isNearPayday() || period.isPaydayReached()
            Result.Success(shouldRemind)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * 计算预算周期的总天数
     */
    private fun calculateTotalPeriodDays(period: BudgetPeriod): Int {
        val totalMillis = period.paydayDate - period.createdDate
        return TimeUnit.MILLISECONDS.toDays(totalMillis).toInt()
    }
    
    /**
     * 计算预算周期已经过去的天数
     */
    private fun calculateElapsedDays(period: BudgetPeriod): Int {
        val currentTime = System.currentTimeMillis()
        val elapsedMillis = currentTime - period.createdDate
        return TimeUnit.MILLISECONDS.toDays(elapsedMillis).toInt()
    }
    
    /**
     * 发薪日详细信息数据类
     */
    data class PaydayDetails(
        val daysUntilPayday: Int,
        val hoursUntilPayday: Int,
        val minutesUntilPayday: Int,
        val isPaydayReached: Boolean,
        val isNearPayday: Boolean,
        val paydayDate: Long,
        val totalPeriodDays: Int,
        val elapsedDays: Int
    ) {
        /**
         * 获取进度百分比
         */
        val progressPercentage: Double
            get() = if (totalPeriodDays > 0) {
                (elapsedDays.toDouble() / totalPeriodDays) * 100
            } else {
                0.0
            }
        
        /**
         * 获取格式化的倒计时文本
         */
        fun getFormattedCountdown(): String {
            return when {
                isPaydayReached -> "发薪日已到！"
                daysUntilPayday > 0 -> "${daysUntilPayday}天"
                hoursUntilPayday > 0 -> "${hoursUntilPayday}小时"
                minutesUntilPayday > 0 -> "${minutesUntilPayday}分钟"
                else -> "即将到来"
            }
        }
        
        /**
         * 获取提醒级别
         */
        val reminderLevel: ReminderLevel
            get() = when {
                isPaydayReached -> ReminderLevel.URGENT
                daysUntilPayday <= 1 -> ReminderLevel.HIGH
                daysUntilPayday <= 3 -> ReminderLevel.MEDIUM
                daysUntilPayday <= 7 -> ReminderLevel.LOW
                else -> ReminderLevel.NONE
            }
    }
    
    /**
     * 提醒级别枚举
     */
    enum class ReminderLevel {
        NONE,    // 无需提醒
        LOW,     // 低级提醒（7天内）
        MEDIUM,  // 中级提醒（3天内）
        HIGH,    // 高级提醒（1天内）
        URGENT   // 紧急提醒（已到达）
    }
}
