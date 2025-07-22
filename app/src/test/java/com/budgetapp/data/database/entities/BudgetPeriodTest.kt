package com.budgetapp.data.database.entities

import org.junit.Assert.*
import org.junit.Test
import org.junit.Before

/**
 * BudgetPeriod实体类的单元测试
 * 
 * 验证BudgetPeriod实体的数据完整性、业务逻辑和边界情况
 */
class BudgetPeriodTest {
    
    private lateinit var validBudgetPeriod: BudgetPeriod
    private val testDisposableAmount = 5000.0
    private val currentTime = System.currentTimeMillis()
    private val futureTime = currentTime + (30 * 24 * 60 * 60 * 1000L) // 30天后
    
    @Before
    fun setUp() {
        validBudgetPeriod = BudgetPeriod(
            id = 1L,
            disposableAmount = testDisposableAmount,
            createdDate = currentTime,
            paydayDate = futureTime,
            isActive = true
        )
    }
    
    @Test
    fun `创建BudgetPeriod时应该包含所有必需字段`() {
        // Given & When
        val budgetPeriod = BudgetPeriod(
            disposableAmount = 3000.0,
            createdDate = currentTime,
            paydayDate = futureTime,
            isActive = true
        )
        
        // Then
        assertEquals(0L, budgetPeriod.id) // 默认ID为0
        assertEquals(3000.0, budgetPeriod.disposableAmount, 0.01)
        assertEquals(currentTime, budgetPeriod.createdDate)
        assertEquals(futureTime, budgetPeriod.paydayDate)
        assertTrue(budgetPeriod.isActive)
    }
    
    @Test
    fun `isValid应该在数据有效时返回true`() {
        // Given
        val validPeriod = BudgetPeriod(
            disposableAmount = 1000.0,
            createdDate = currentTime,
            paydayDate = currentTime + 1000L, // 发薪日在创建日期之后
            isActive = true
        )
        
        // When & Then
        assertTrue(validPeriod.isValid())
    }
    
    @Test
    fun `isValid应该在可支配金额为负数时返回false`() {
        // Given
        val invalidPeriod = BudgetPeriod(
            disposableAmount = -100.0, // 负数金额
            createdDate = currentTime,
            paydayDate = futureTime,
            isActive = true
        )
        
        // When & Then
        assertFalse(invalidPeriod.isValid())
    }
    
    @Test
    fun `isValid应该在发薪日早于创建日期时返回false`() {
        // Given
        val invalidPeriod = BudgetPeriod(
            disposableAmount = 1000.0,
            createdDate = currentTime,
            paydayDate = currentTime - 1000L, // 发薪日在创建日期之前
            isActive = true
        )
        
        // When & Then
        assertFalse(invalidPeriod.isValid())
    }
    
    @Test
    fun `isValid应该在可支配金额为0时返回true`() {
        // Given
        val zeroPeriod = BudgetPeriod(
            disposableAmount = 0.0,
            createdDate = currentTime,
            paydayDate = futureTime,
            isActive = true
        )
        
        // When & Then
        assertTrue(zeroPeriod.isValid())
    }
    
    @Test
    fun `getDaysUntilPayday应该正确计算剩余天数`() {
        // Given
        val oneDayInMillis = 24 * 60 * 60 * 1000L
        val threeDaysLater = System.currentTimeMillis() + (3 * oneDayInMillis)
        val period = BudgetPeriod(
            disposableAmount = 1000.0,
            createdDate = System.currentTimeMillis(),
            paydayDate = threeDaysLater,
            isActive = true
        )
        
        // When
        val daysUntilPayday = period.getDaysUntilPayday()
        
        // Then
        assertTrue("剩余天数应该在2-3天之间", daysUntilPayday in 2..3)
    }
    
    @Test
    fun `getDaysUntilPayday应该在发薪日已过时返回0`() {
        // Given
        val pastTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000L) // 1天前
        val period = BudgetPeriod(
            disposableAmount = 1000.0,
            createdDate = pastTime - 1000L,
            paydayDate = pastTime,
            isActive = true
        )
        
        // When
        val daysUntilPayday = period.getDaysUntilPayday()
        
        // Then
        assertEquals(0, daysUntilPayday)
    }
    
    @Test
    fun `isNearPayday应该在距离发薪日少于7天时返回true`() {
        // Given
        val sixDaysLater = System.currentTimeMillis() + (6 * 24 * 60 * 60 * 1000L)
        val period = BudgetPeriod(
            disposableAmount = 1000.0,
            createdDate = System.currentTimeMillis(),
            paydayDate = sixDaysLater,
            isActive = true
        )
        
        // When & Then
        assertTrue(period.isNearPayday())
    }
    
    @Test
    fun `isNearPayday应该在距离发薪日超过7天时返回false`() {
        // Given
        val tenDaysLater = System.currentTimeMillis() + (10 * 24 * 60 * 60 * 1000L)
        val period = BudgetPeriod(
            disposableAmount = 1000.0,
            createdDate = System.currentTimeMillis(),
            paydayDate = tenDaysLater,
            isActive = true
        )
        
        // When & Then
        assertFalse(period.isNearPayday())
    }
    
    @Test
    fun `isPaydayReached应该在发薪日已到达时返回true`() {
        // Given
        val pastTime = System.currentTimeMillis() - 1000L // 1秒前
        val period = BudgetPeriod(
            disposableAmount = 1000.0,
            createdDate = pastTime - 1000L,
            paydayDate = pastTime,
            isActive = true
        )
        
        // When & Then
        assertTrue(period.isPaydayReached())
    }
    
    @Test
    fun `isPaydayReached应该在发薪日未到达时返回false`() {
        // Given
        val futureTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000L) // 1天后
        val period = BudgetPeriod(
            disposableAmount = 1000.0,
            createdDate = System.currentTimeMillis(),
            paydayDate = futureTime,
            isActive = true
        )
        
        // When & Then
        assertFalse(period.isPaydayReached())
    }
    
    @Test
    fun `create工厂方法应该创建有效的BudgetPeriod`() {
        // Given
        val amount = 2500.0
        
        // When
        val period = BudgetPeriod.create(amount)
        
        // Then
        assertEquals(amount, period.disposableAmount, 0.01)
        assertTrue(period.isActive)
        assertTrue(period.isValid())
        assertTrue(period.paydayDate > period.createdDate)
    }
    
    @Test
    fun `create工厂方法应该支持自定义周期天数`() {
        // Given
        val amount = 1500.0
        val customDays = 15
        
        // When
        val period = BudgetPeriod.create(amount, customDays)
        
        // Then
        val expectedPaydayTime = period.createdDate + (customDays * 24 * 60 * 60 * 1000L)
        val timeDifference = Math.abs(period.paydayDate - expectedPaydayTime)
        assertTrue("发薪日时间应该正确计算", timeDifference < 1000L) // 允许1秒误差
    }
    
    @Test
    fun `常量值应该正确定义`() {
        // Then
        assertEquals(30, BudgetPeriod.DEFAULT_PERIOD_DAYS)
        assertEquals(7, BudgetPeriod.PAYDAY_REMINDER_THRESHOLD_DAYS)
    }
}
