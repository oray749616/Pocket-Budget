package com.budgetapp.data.database.entities

import org.junit.Assert.*
import org.junit.Test
import org.junit.Before

/**
 * Expense实体类的单元测试
 * 
 * 验证Expense实体的数据完整性、业务逻辑和边界情况
 */
class ExpenseTest {
    
    private lateinit var validExpense: Expense
    private val testBudgetPeriodId = 1L
    private val testDescription = "测试支出"
    private val testAmount = 100.0
    private val currentTime = System.currentTimeMillis()
    
    @Before
    fun setUp() {
        validExpense = Expense(
            id = 1L,
            budgetPeriodId = testBudgetPeriodId,
            description = testDescription,
            amount = testAmount,
            createdDate = currentTime
        )
    }
    
    @Test
    fun `创建Expense时应该包含所有必需字段`() {
        // Given & When
        val expense = Expense(
            budgetPeriodId = 2L,
            description = "购买食物",
            amount = 50.0,
            createdDate = currentTime
        )
        
        // Then
        assertEquals(0L, expense.id) // 默认ID为0
        assertEquals(2L, expense.budgetPeriodId)
        assertEquals("购买食物", expense.description)
        assertEquals(50.0, expense.amount, 0.01)
        assertEquals(currentTime, expense.createdDate)
    }
    
    @Test
    fun `isValid应该在数据有效时返回true`() {
        // Given
        val validExpense = Expense(
            budgetPeriodId = 1L,
            description = "有效支出",
            amount = 200.0,
            createdDate = currentTime
        )
        
        // When & Then
        assertTrue(validExpense.isValid())
    }
    
    @Test
    fun `isValid应该在描述为空时返回false`() {
        // Given
        val invalidExpense = Expense(
            budgetPeriodId = 1L,
            description = "", // 空描述
            amount = 100.0,
            createdDate = currentTime
        )
        
        // When & Then
        assertFalse(invalidExpense.isValid())
    }
    
    @Test
    fun `isValid应该在描述只包含空格时返回false`() {
        // Given
        val invalidExpense = Expense(
            budgetPeriodId = 1L,
            description = "   ", // 只有空格
            amount = 100.0,
            createdDate = currentTime
        )
        
        // When & Then
        assertFalse(invalidExpense.isValid())
    }
    
    @Test
    fun `isValid应该在金额为0或负数时返回false`() {
        // Given
        val zeroAmountExpense = Expense(
            budgetPeriodId = 1L,
            description = "测试",
            amount = 0.0, // 零金额
            createdDate = currentTime
        )
        
        val negativeAmountExpense = Expense(
            budgetPeriodId = 1L,
            description = "测试",
            amount = -50.0, // 负金额
            createdDate = currentTime
        )
        
        // When & Then
        assertFalse(zeroAmountExpense.isValid())
        assertFalse(negativeAmountExpense.isValid())
    }
    
    @Test
    fun `isValid应该在budgetPeriodId无效时返回false`() {
        // Given
        val invalidExpense = Expense(
            budgetPeriodId = 0L, // 无效的预算周期ID
            description = "测试",
            amount = 100.0,
            createdDate = currentTime
        )
        
        // When & Then
        assertFalse(invalidExpense.isValid())
    }
    
    @Test
    fun `isLargeExpense应该正确识别大额支出`() {
        // Given
        val largeExpense = Expense(
            budgetPeriodId = 1L,
            description = "大额支出",
            amount = 1500.0,
            createdDate = currentTime
        )
        
        val smallExpense = Expense(
            budgetPeriodId = 1L,
            description = "小额支出",
            amount = 50.0,
            createdDate = currentTime
        )
        
        // When & Then
        assertTrue(largeExpense.isLargeExpense())
        assertFalse(smallExpense.isLargeExpense())
    }
    
    @Test
    fun `isLargeExpense应该支持自定义阈值`() {
        // Given
        val expense = Expense(
            budgetPeriodId = 1L,
            description = "测试支出",
            amount = 500.0,
            createdDate = currentTime
        )
        
        // When & Then
        assertTrue(expense.isLargeExpense(400.0)) // 自定义阈值400
        assertFalse(expense.isLargeExpense(600.0)) // 自定义阈值600
    }
    
    @Test
    fun `getFormattedDescription应该正确格式化长描述`() {
        // Given
        val longDescription = "这是一个非常非常非常非常非常非常非常非常非常非常长的支出描述"
        val expense = Expense(
            budgetPeriodId = 1L,
            description = longDescription,
            amount = 100.0,
            createdDate = currentTime
        )
        
        // When
        val formatted = expense.getFormattedDescription(20)
        
        // Then
        assertEquals(20, formatted.length)
        assertTrue(formatted.endsWith("..."))
    }
    
    @Test
    fun `getFormattedDescription应该保持短描述不变`() {
        // Given
        val shortDescription = "短描述"
        val expense = Expense(
            budgetPeriodId = 1L,
            description = shortDescription,
            amount = 100.0,
            createdDate = currentTime
        )
        
        // When
        val formatted = expense.getFormattedDescription()
        
        // Then
        assertEquals(shortDescription, formatted)
    }
    
    @Test
    fun `getAgeInDays应该正确计算支出年龄`() {
        // Given
        val threeDaysAgo = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000L)
        val expense = Expense(
            budgetPeriodId = 1L,
            description = "三天前的支出",
            amount = 100.0,
            createdDate = threeDaysAgo
        )
        
        // When
        val age = expense.getAgeInDays()
        
        // Then
        assertTrue("支出年龄应该在2-3天之间", age in 2..3)
    }
    
    @Test
    fun `isCreatedToday应该正确识别今日创建的支出`() {
        // Given
        val todayExpense = Expense(
            budgetPeriodId = 1L,
            description = "今日支出",
            amount = 100.0,
            createdDate = System.currentTimeMillis()
        )
        
        val yesterdayExpense = Expense(
            budgetPeriodId = 1L,
            description = "昨日支出",
            amount = 100.0,
            createdDate = System.currentTimeMillis() - (25 * 60 * 60 * 1000L) // 25小时前
        )
        
        // When & Then
        assertTrue(todayExpense.isCreatedToday())
        assertFalse(yesterdayExpense.isCreatedToday())
    }
    
    @Test
    fun `create工厂方法应该创建有效的Expense`() {
        // Given
        val budgetPeriodId = 1L
        val description = "测试支出"
        val amount = 150.0
        
        // When
        val expense = Expense.create(budgetPeriodId, description, amount)
        
        // Then
        assertNotNull(expense)
        assertEquals(budgetPeriodId, expense!!.budgetPeriodId)
        assertEquals(description, expense.description)
        assertEquals(amount, expense.amount, 0.01)
        assertTrue(expense.isValid())
    }
    
    @Test
    fun `create工厂方法应该在参数无效时返回null`() {
        // When & Then
        assertNull(Expense.create(0L, "测试", 100.0)) // 无效budgetPeriodId
        assertNull(Expense.create(1L, "", 100.0)) // 空描述
        assertNull(Expense.create(1L, "   ", 100.0)) // 空白描述
        assertNull(Expense.create(1L, "测试", 0.0)) // 零金额
        assertNull(Expense.create(1L, "测试", -50.0)) // 负金额
    }
    
    @Test
    fun `create工厂方法应该自动修剪描述中的空格`() {
        // Given
        val description = "  测试支出  "
        
        // When
        val expense = Expense.create(1L, description, 100.0)
        
        // Then
        assertNotNull(expense)
        assertEquals("测试支出", expense!!.description)
    }
    
    @Test
    fun `isValidDescription应该正确验证描述`() {
        // When & Then
        assertTrue(Expense.isValidDescription("有效描述"))
        assertFalse(Expense.isValidDescription(""))
        assertFalse(Expense.isValidDescription("   "))
        
        // 测试长度限制
        val longDescription = "a".repeat(Expense.MAX_DESCRIPTION_LENGTH + 1)
        assertFalse(Expense.isValidDescription(longDescription))
        
        val maxLengthDescription = "a".repeat(Expense.MAX_DESCRIPTION_LENGTH)
        assertTrue(Expense.isValidDescription(maxLengthDescription))
    }
    
    @Test
    fun `isValidAmount应该正确验证金额`() {
        // When & Then
        assertTrue(Expense.isValidAmount(100.0))
        assertTrue(Expense.isValidAmount(0.01))
        assertFalse(Expense.isValidAmount(0.0))
        assertFalse(Expense.isValidAmount(-50.0))
        assertFalse(Expense.isValidAmount(Double.POSITIVE_INFINITY))
        assertFalse(Expense.isValidAmount(Double.NaN))
    }
    
    @Test
    fun `常量值应该正确定义`() {
        // Then
        assertEquals(50, Expense.MAX_DESCRIPTION_LENGTH)
        assertEquals(1000.0, Expense.LARGE_EXPENSE_THRESHOLD, 0.01)
        assertEquals(1, Expense.MIN_DESCRIPTION_LENGTH)
    }
}
