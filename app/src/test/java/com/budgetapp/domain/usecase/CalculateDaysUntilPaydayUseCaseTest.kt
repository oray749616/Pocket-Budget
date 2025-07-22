package com.budgetapp.domain.usecase

import com.budgetapp.data.database.entities.BudgetPeriod
import com.budgetapp.domain.model.Result
import com.budgetapp.domain.repository.BudgetRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * CalculateDaysUntilPaydayUseCase单元测试
 * 
 * 测试发薪日倒计时Use Case的各种场景，包括边界情况。
 */
class CalculateDaysUntilPaydayUseCaseTest {

    private lateinit var repository: BudgetRepository
    private lateinit var useCase: CalculateDaysUntilPaydayUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = CalculateDaysUntilPaydayUseCase(repository)
    }
    
    @Test
    fun `calculateForPeriod should return correct days when period exists`() = runTest {
        // Given
        val periodId = 1L
        val currentTime = System.currentTimeMillis()
        val paydayTime = currentTime + TimeUnit.DAYS.toMillis(15) // 15天后
        val expectedDays = 15
        
        val period = BudgetPeriod(
            id = periodId,
            disposableAmount = 1000.0,
            createdDate = currentTime - TimeUnit.DAYS.toMillis(15), // 15天前创建
            paydayDate = paydayTime
        )
        
        coEvery { repository.getBudgetPeriodById(periodId) } returns Result.Success(period)
        
        // When
        val result = useCase.calculateForPeriod(periodId)
        
        // Then
        assertTrue(result.isSuccess)
        // 允许1天的误差，因为时间计算可能有微小差异
        val actualDays = result.getOrNull()!!
        assertTrue(actualDays >= expectedDays - 1 && actualDays <= expectedDays + 1)
    }
    
    @Test
    fun `calculateForPeriod should return 0 when payday has passed`() = runTest {
        // Given
        val periodId = 1L
        val currentTime = System.currentTimeMillis()
        val paydayTime = currentTime - TimeUnit.DAYS.toMillis(5) // 5天前
        
        val period = BudgetPeriod(
            id = periodId,
            disposableAmount = 1000.0,
            createdDate = currentTime - TimeUnit.DAYS.toMillis(35), // 35天前创建
            paydayDate = paydayTime
        )
        
        coEvery { repository.getBudgetPeriodById(periodId) } returns Result.Success(period)
        
        // When
        val result = useCase.calculateForPeriod(periodId)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
    }
    
    @Test
    fun `calculateForPeriod should return error when period does not exist`() = runTest {
        // Given
        val periodId = 1L
        
        coEvery { repository.getBudgetPeriodById(periodId) } returns Result.Success(null)
        
        // When
        val result = useCase.calculateForPeriod(periodId)
        
        // Then
        assertTrue(result.isError)
        assertEquals("预算周期不存在", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `calculatePaydayDetails should return correct details for future payday`() = runTest {
        // Given
        val periodId = 1L
        val currentTime = System.currentTimeMillis()
        val paydayTime = currentTime + TimeUnit.DAYS.toMillis(10) + TimeUnit.HOURS.toMillis(5) // 10天5小时后
        
        val period = BudgetPeriod(
            id = periodId,
            disposableAmount = 1000.0,
            createdDate = currentTime - TimeUnit.DAYS.toMillis(20), // 20天前创建
            paydayDate = paydayTime
        )
        
        coEvery { repository.getBudgetPeriodById(periodId) } returns Result.Success(period)
        
        // When
        val result = useCase.calculatePaydayDetails(periodId)
        
        // Then
        assertTrue(result.isSuccess)
        val details = result.getOrNull()!!
        
        // 允许1天的误差
        assertTrue(details.daysUntilPayday >= 9 && details.daysUntilPayday <= 11)
        assertFalse(details.isPaydayReached)
        assertEquals(paydayTime, details.paydayDate)
        assertTrue(details.totalPeriodDays > 0)
        assertTrue(details.elapsedDays > 0)
    }
    
    @Test
    fun `calculatePaydayDetails should return correct details for reached payday`() = runTest {
        // Given
        val periodId = 1L
        val currentTime = System.currentTimeMillis()
        val paydayTime = currentTime - TimeUnit.HOURS.toMillis(2) // 2小时前
        
        val period = BudgetPeriod(
            id = periodId,
            disposableAmount = 1000.0,
            createdDate = currentTime - TimeUnit.DAYS.toMillis(30), // 30天前创建
            paydayDate = paydayTime
        )
        
        coEvery { repository.getBudgetPeriodById(periodId) } returns Result.Success(period)
        
        // When
        val result = useCase.calculatePaydayDetails(periodId)
        
        // Then
        assertTrue(result.isSuccess)
        val details = result.getOrNull()!!
        
        assertEquals(0, details.daysUntilPayday)
        assertEquals(0, details.hoursUntilPayday)
        assertEquals(0, details.minutesUntilPayday)
        assertTrue(details.isPaydayReached)
        assertFalse(details.isNearPayday)
    }
    
    @Test
    fun `shouldShowPaydayReminder should return true when near payday`() = runTest {
        // Given
        val periodId = 1L
        val currentTime = System.currentTimeMillis()
        val paydayTime = currentTime + TimeUnit.DAYS.toMillis(3) // 3天后（少于7天）
        
        val period = BudgetPeriod(
            id = periodId,
            disposableAmount = 1000.0,
            createdDate = currentTime - TimeUnit.DAYS.toMillis(27), // 27天前创建
            paydayDate = paydayTime
        )
        
        coEvery { repository.getBudgetPeriodById(periodId) } returns Result.Success(period)
        
        // When
        val result = useCase.shouldShowPaydayReminder(periodId)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
    }
    
    @Test
    fun `shouldShowPaydayReminder should return false when not near payday`() = runTest {
        // Given
        val periodId = 1L
        val currentTime = System.currentTimeMillis()
        val paydayTime = currentTime + TimeUnit.DAYS.toMillis(15) // 15天后
        
        val period = BudgetPeriod(
            id = periodId,
            disposableAmount = 1000.0,
            createdDate = currentTime - TimeUnit.DAYS.toMillis(15), // 15天前创建
            paydayDate = paydayTime
        )
        
        coEvery { repository.getBudgetPeriodById(periodId) } returns Result.Success(period)
        
        // When
        val result = useCase.shouldShowPaydayReminder(periodId)
        
        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() == true)
    }
    
    @Test
    fun `PaydayDetails should calculate progress percentage correctly`() {
        // Given
        val details = CalculateDaysUntilPaydayUseCase.PaydayDetails(
            daysUntilPayday = 10,
            hoursUntilPayday = 5,
            minutesUntilPayday = 30,
            isPaydayReached = false,
            isNearPayday = false,
            paydayDate = System.currentTimeMillis(),
            totalPeriodDays = 30,
            elapsedDays = 20
        )
        
        // When
        val percentage = details.progressPercentage
        
        // Then
        assertEquals(66.67, percentage, 0.1)
    }
    
    @Test
    fun `PaydayDetails should format countdown correctly`() {
        // Given
        val detailsDays = CalculateDaysUntilPaydayUseCase.PaydayDetails(
            daysUntilPayday = 5,
            hoursUntilPayday = 0,
            minutesUntilPayday = 0,
            isPaydayReached = false,
            isNearPayday = true,
            paydayDate = System.currentTimeMillis(),
            totalPeriodDays = 30,
            elapsedDays = 25
        )
        
        val detailsHours = CalculateDaysUntilPaydayUseCase.PaydayDetails(
            daysUntilPayday = 0,
            hoursUntilPayday = 8,
            minutesUntilPayday = 0,
            isPaydayReached = false,
            isNearPayday = true,
            paydayDate = System.currentTimeMillis(),
            totalPeriodDays = 30,
            elapsedDays = 30
        )
        
        val detailsReached = CalculateDaysUntilPaydayUseCase.PaydayDetails(
            daysUntilPayday = 0,
            hoursUntilPayday = 0,
            minutesUntilPayday = 0,
            isPaydayReached = true,
            isNearPayday = false,
            paydayDate = System.currentTimeMillis(),
            totalPeriodDays = 30,
            elapsedDays = 30
        )
        
        // When & Then
        assertEquals("5天", detailsDays.getFormattedCountdown())
        assertEquals("8小时", detailsHours.getFormattedCountdown())
        assertEquals("发薪日已到！", detailsReached.getFormattedCountdown())
    }
    
    @Test
    fun `PaydayDetails should determine reminder level correctly`() {
        // Given
        val urgentDetails = CalculateDaysUntilPaydayUseCase.PaydayDetails(
            daysUntilPayday = 0, hoursUntilPayday = 0, minutesUntilPayday = 0,
            isPaydayReached = true, isNearPayday = false,
            paydayDate = System.currentTimeMillis(), totalPeriodDays = 30, elapsedDays = 30
        )
        
        val highDetails = CalculateDaysUntilPaydayUseCase.PaydayDetails(
            daysUntilPayday = 1, hoursUntilPayday = 0, minutesUntilPayday = 0,
            isPaydayReached = false, isNearPayday = true,
            paydayDate = System.currentTimeMillis(), totalPeriodDays = 30, elapsedDays = 29
        )
        
        val noneDetails = CalculateDaysUntilPaydayUseCase.PaydayDetails(
            daysUntilPayday = 15, hoursUntilPayday = 0, minutesUntilPayday = 0,
            isPaydayReached = false, isNearPayday = false,
            paydayDate = System.currentTimeMillis(), totalPeriodDays = 30, elapsedDays = 15
        )
        
        // When & Then
        assertEquals(CalculateDaysUntilPaydayUseCase.ReminderLevel.URGENT, urgentDetails.reminderLevel)
        assertEquals(CalculateDaysUntilPaydayUseCase.ReminderLevel.HIGH, highDetails.reminderLevel)
        assertEquals(CalculateDaysUntilPaydayUseCase.ReminderLevel.NONE, noneDetails.reminderLevel)
    }
}
