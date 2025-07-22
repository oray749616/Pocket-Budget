package com.budgetapp.data.di

import android.content.Context
import androidx.room.Room
import com.budgetapp.data.database.BudgetDatabase
import com.budgetapp.data.database.dao.BudgetDao
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

    /**
     * 提供BudgetDatabase实例
     * 使用Room构建器创建数据库实例，配置数据库名称和迁移策略
     *
     * @param context 应用上下文
     * @return BudgetDatabase实例
     */
    @Provides
    @Singleton
    fun provideBudgetDatabase(@ApplicationContext context: Context): BudgetDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            BudgetDatabase::class.java,
            BudgetDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration() // 开发阶段使用，生产环境需要提供迁移策略
        .build()
    }

    /**
     * 提供BudgetDao实例
     * 从数据库实例中获取DAO接口
     *
     * @param database BudgetDatabase实例
     * @return BudgetDao实例
     */
    @Provides
    fun provideBudgetDao(database: BudgetDatabase): BudgetDao {
        return database.budgetDao()
    }
}
