package com.budgetapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * 简洁资金管理应用的Application类
 * 
 * 使用Hilt进行依赖注入的入口点
 * 负责初始化全局应用状态和依赖注入容器
 */
@HiltAndroidApp
class BudgetApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // 应用初始化逻辑
        // Hilt会自动处理依赖注入的初始化
    }
}
