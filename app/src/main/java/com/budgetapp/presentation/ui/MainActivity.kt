package com.budgetapp.presentation.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.budgetapp.R
import com.budgetapp.databinding.ActivityMainBinding
import com.budgetapp.presentation.state.BudgetEvent
import com.budgetapp.presentation.state.BudgetUiState
import com.budgetapp.presentation.ui.fragments.BudgetMainFragment
import com.budgetapp.presentation.viewmodel.BudgetViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 简洁资金管理应用的主Activity
 *
 * 使用Hilt进行依赖注入，Material Design 3主题
 * 负责管理应用的主要UI导航和生命周期
 *
 * Requirements:
 * - 7.1: 配置Material Design 3主题
 * - 7.2: 创建主布局文件，使用ConstraintLayout作为根布局
 * - 7.4: 显示加载状态和用户反馈
 * - 7.5: 处理错误信息显示
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: BudgetViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 启用Edge-to-Edge显示
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 设置View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化UI组件
        setupUI()

        // 观察ViewModel事件
        observeViewModelEvents()

        // 加载主Fragment
        if (savedInstanceState == null) {
            loadMainFragment()
        }
    }

    /**
     * 初始化UI组件和导航
     */
    private fun setupUI() {
        // 设置工具栏
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = getString(R.string.app_name)
            setDisplayShowTitleEnabled(true)
        }

        // 配置工具栏样式
        binding.toolbar.apply {
            setTitleTextColor(getColor(R.color.md_theme_on_surface))
            elevation = 4f
        }
    }

    /**
     * 观察ViewModel状态
     */
    private fun observeViewModelEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    handleUiStateChange(state)
                }
            }
        }
    }

    /**
     * 处理UI状态变化
     *
     * @param state UI状态
     */
    private fun handleUiStateChange(state: BudgetUiState) {
        // 处理错误消息
        state.errorMessage?.let { message ->
            showErrorSnackbar(message)
            viewModel.clearErrorMessage()
        }

        // 处理成功消息
        state.successMessage?.let { message ->
            showSuccessSnackbar(message)
            viewModel.clearSuccessMessage()
        }

        // 处理超支警告
        if (state.isOverBudget && state.overspendAmount > 0) {
            showOverspendWarning(state.overspendAmount)
        }
    }

    /**
     * 显示错误Snackbar
     *
     * @param message 错误消息
     */
    private fun showErrorSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(getColor(R.color.md_theme_error_container))
            .setTextColor(getColor(R.color.md_theme_on_error_container))
            .setAction("关闭") { /* 关闭Snackbar */ }
            .setActionTextColor(getColor(R.color.md_theme_on_error_container))
            .show()
    }

    /**
     * 显示成功Snackbar
     *
     * @param message 成功消息
     */
    private fun showSuccessSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            .setBackgroundTint(getColor(R.color.positive_amount))
            .setTextColor(getColor(R.color.white))
            .show()
    }

    /**
     * 显示超支警告
     *
     * @param overspendAmount 超支金额
     */
    private fun showOverspendWarning(overspendAmount: Double) {
        val message = "注意：本次支出将导致超支 ¥%.2f".format(overspendAmount)
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(getColor(R.color.warning_amount))
            .setTextColor(getColor(R.color.white))
            .setAction("知道了") { /* 关闭Snackbar */ }
            .setActionTextColor(getColor(R.color.white))
            .show()
    }

    /**
     * 加载主Fragment
     */
    private fun loadMainFragment() {
        val fragment = BudgetMainFragment()
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.nav_host_fragment, fragment)
        }
    }

    /**
     * 替换Fragment
     *
     * @param fragment 要显示的Fragment
     * @param addToBackStack 是否添加到返回栈
     */
    fun replaceFragment(fragment: Fragment, addToBackStack: Boolean = false) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.nav_host_fragment, fragment)
            if (addToBackStack) {
                addToBackStack(null)
            }
        }
    }

    /**
     * 创建选项菜单
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    /**
     * 处理选项菜单点击
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_reset_budget -> {
                viewModel.showResetBudgetDialog()
                true
            }
            R.id.action_settings -> {
                // TODO: 实现设置页面
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
