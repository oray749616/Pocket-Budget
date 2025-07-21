package com.budgetapp.data.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt数据库依赖注入模块
 * 
 * 提供数据库相关的依赖项，包括Room数据库实例和DAO接口
 * 使用Singleton作用域确保数据库实例的唯一性
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    // TODO: 在任务2中实现具体的数据库提供者
    // 包括BudgetDatabase、BudgetDao等的提供方法
    
    /**
     * 提供应用上下文
     * 用于数据库初始化等需要Context的场景
     */
    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context {
        return context
    }
}
