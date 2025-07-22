package com.budgetapp.presentation.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.budgetapp.R
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * BudgetMainIntegrationTest
 * 
 * 预算应用主界面的集成测试，测试主要用户流程。
 * 使用Espresso测试UI交互和数据持久化。
 * 
 * Requirements:
 * - 1.1-1.5: 测试金额输入和显示功能
 * - 2.1-2.5: 测试支出添加和删除功能
 * - 5.1-5.5: 测试支出列表显示功能
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class BudgetMainIntegrationTest {
    
    @get:Rule
    val hiltRule = HiltAndroidRule(this)
    
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)
    
    @Before
    fun setup() {
        hiltRule.inject()
    }
    
    @Test
    fun testCompleteUserFlow_SetupBudgetAndAddExpenses() {
        // 1. 验证初始状态 - 应该显示空状态
        onView(withId(R.id.layout_empty_state))
            .check(matches(isDisplayed()))
        
        onView(withText(R.string.no_budget_period))
            .check(matches(isDisplayed()))
        
        // 2. 点击设置预算按钮
        onView(withId(R.id.btn_setup_budget))
            .perform(click())
        
        // 3. 在重置预算对话框中输入可支配金额
        onView(withId(R.id.et_new_disposable_amount))
            .perform(typeText("1000.00"))
        
        // 4. 确认创建预算周期
        onView(withId(R.id.btn_reset))
            .perform(click())
        
        // 5. 验证预算数据显示
        onView(withId(R.id.layout_budget_content))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.tv_remaining_amount))
            .check(matches(withText("¥1000.00")))
        
        onView(withId(R.id.tv_disposable_amount))
            .check(matches(withText("可支配金额：¥1000.00")))
        
        // 6. 点击添加支出按钮
        onView(withId(R.id.fab_add_expense))
            .perform(click())
        
        // 7. 在添加支出对话框中输入数据
        onView(withId(R.id.et_expense_description))
            .perform(typeText("Groceries"))
        
        onView(withId(R.id.et_expense_amount))
            .perform(typeText("150.00"))
        
        // 8. 保存支出
        onView(withId(R.id.btn_save))
            .perform(click())
        
        // 9. 验证支出添加成功
        onView(withId(R.id.tv_remaining_amount))
            .check(matches(withText("¥850.00")))
        
        onView(withId(R.id.tv_total_expenses))
            .check(matches(withText("¥150.00")))
        
        // 10. 验证支出列表显示
        onView(withId(R.id.recycler_view_expenses))
            .check(matches(isDisplayed()))
        
        onView(withText("Groceries"))
            .check(matches(isDisplayed()))
        
        onView(withText("¥150.00"))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testAddMultipleExpenses() {
        // 设置预算周期
        setupBudgetPeriod("1000.00")
        
        // 添加第一笔支出
        addExpense("Coffee", "25.00")
        
        // 验证第一笔支出
        onView(withId(R.id.tv_remaining_amount))
            .check(matches(withText("¥975.00")))
        
        // 添加第二笔支出
        addExpense("Lunch", "45.00")
        
        // 验证总计
        onView(withId(R.id.tv_remaining_amount))
            .check(matches(withText("¥930.00")))
        
        onView(withId(R.id.tv_total_expenses))
            .check(matches(withText("¥70.00")))
        
        // 验证两笔支出都在列表中
        onView(withText("Coffee"))
            .check(matches(isDisplayed()))
        
        onView(withText("Lunch"))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testOverspendingWarning() {
        // 设置较小的预算
        setupBudgetPeriod("100.00")
        
        // 添加会导致超支的支出
        addExpense("Expensive item", "150.00")
        
        // 验证超支状态
        onView(withId(R.id.tv_remaining_amount))
            .check(matches(withText("¥-50.00")))
        
        onView(withId(R.id.tv_overspend_warning))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.tv_overspend_warning))
            .check(matches(withText("超支：¥50.00")))
    }
    
    @Test
    fun testInputValidation() {
        setupBudgetPeriod("1000.00")
        
        // 点击添加支出按钮
        onView(withId(R.id.fab_add_expense))
            .perform(click())
        
        // 尝试保存空的支出描述
        onView(withId(R.id.et_expense_amount))
            .perform(typeText("100.00"))
        
        onView(withId(R.id.btn_save))
            .perform(click())
        
        // 验证错误提示
        onView(withText("支出描述不能为空"))
            .check(matches(isDisplayed()))
        
        // 输入描述但清空金额
        onView(withId(R.id.et_expense_description))
            .perform(typeText("Test"))
        
        onView(withId(R.id.et_expense_amount))
            .perform(clearText())
        
        onView(withId(R.id.btn_save))
            .perform(click())
        
        // 验证金额错误提示
        onView(withText("支出金额不能为空"))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testExpenseDelete() {
        setupBudgetPeriod("1000.00")
        addExpense("Test expense", "100.00")
        
        // 长按支出项目
        onView(withText("Test expense"))
            .perform(longClick())
        
        // 确认删除
        onView(withText(R.string.delete))
            .perform(click())
        
        // 验证支出被删除
        onView(withId(R.id.tv_remaining_amount))
            .check(matches(withText("¥1000.00")))
        
        onView(withId(R.id.tv_total_expenses))
            .check(matches(withText("¥0.00")))
        
        // 验证空状态显示
        onView(withId(R.id.layout_expense_empty))
            .check(matches(isDisplayed()))
    }
    
    // Helper methods
    
    private fun setupBudgetPeriod(amount: String) {
        onView(withId(R.id.btn_setup_budget))
            .perform(click())
        
        onView(withId(R.id.et_new_disposable_amount))
            .perform(typeText(amount))
        
        onView(withId(R.id.btn_reset))
            .perform(click())
    }
    
    private fun addExpense(description: String, amount: String) {
        onView(withId(R.id.fab_add_expense))
            .perform(click())
        
        onView(withId(R.id.et_expense_description))
            .perform(typeText(description))
        
        onView(withId(R.id.et_expense_amount))
            .perform(typeText(amount))
        
        onView(withId(R.id.btn_save))
            .perform(click())
    }
}
