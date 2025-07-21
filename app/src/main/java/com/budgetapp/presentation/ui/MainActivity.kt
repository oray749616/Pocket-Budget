package com.budgetapp.presentation.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import com.budgetapp.R

/**
 * 简洁资金管理应用的主Activity
 * 
 * 使用Hilt进行依赖注入
 * 负责管理应用的主要UI导航和生命周期
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // 初始化UI组件
        setupUI()
    }
    
    /**
     * 初始化UI组件和导航
     */
    private fun setupUI() {
        // TODO: 在后续任务中实现UI初始化逻辑
        // 包括Fragment导航、工具栏设置等
    }
}
