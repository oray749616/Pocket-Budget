package com.budgetapp.data.di

import com.budgetapp.data.repository.BudgetRepositoryImpl
import com.budgetapp.domain.repository.BudgetRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Repository依赖注入模块
 * 
 * 提供Repository相关的依赖项，包括Repository接口的实现绑定
 * 和协程调度器的配置
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    /**
     * 绑定BudgetRepository接口到其实现类
     * 
     * @param budgetRepositoryImpl BudgetRepository的实现类
     * @return BudgetRepository接口
     */
    @Binds
    @Singleton
    abstract fun bindBudgetRepository(
        budgetRepositoryImpl: BudgetRepositoryImpl
    ): BudgetRepository
    
    companion object {
        /**
         * 提供IO调度器
         * 用于数据库操作和网络请求
         * 
         * @return IO调度器
         */
        @Provides
        @IoDispatcher
        fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
        
        /**
         * 提供Main调度器
         * 用于UI更新操作
         * 
         * @return Main调度器
         */
        @Provides
        @MainDispatcher
        fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
        
        /**
         * 提供Default调度器
         * 用于CPU密集型操作
         * 
         * @return Default调度器
         */
        @Provides
        @DefaultDispatcher
        fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
    }
}

/**
 * IO调度器限定符
 * 用于标识IO操作的协程调度器
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/**
 * Main调度器限定符
 * 用于标识主线程操作的协程调度器
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

/**
 * Default调度器限定符
 * 用于标识CPU密集型操作的协程调度器
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher
