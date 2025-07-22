package com.budgetapp.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.budgetapp.data.database.dao.BudgetDao
import com.budgetapp.data.database.entities.BudgetPeriod
import com.budgetapp.data.database.entities.Expense

/**
 * BudgetDatabase Room数据库
 * 
 * 应用的主数据库，包含预算周期和支出两个实体。
 * 配置了数据库版本、实体类、DAO接口等。
 * 
 * Requirements:
 * - 6.1: 创建SQLite数据库和必要的表结构
 * - 6.3: 支持从数据库加载之前保存的数据
 */
@Database(
    entities = [
        BudgetPeriod::class,
        Expense::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class BudgetDatabase : RoomDatabase() {
    
    /**
     * 获取预算数据访问对象
     * 
     * @return BudgetDao实例
     */
    abstract fun budgetDao(): BudgetDao
    
    companion object {
        /**
         * 数据库名称
         */
        const val DATABASE_NAME = "budget_database"
        
        /**
         * 数据库版本
         */
        const val DATABASE_VERSION = 1
        
        /**
         * 数据库实例（单例模式）
         * 使用@Volatile确保多线程环境下的可见性
         */
        @Volatile
        private var INSTANCE: BudgetDatabase? = null
        
        /**
         * 获取数据库实例
         * 使用双重检查锁定模式确保线程安全的单例
         * 
         * @param context 应用上下文
         * @return BudgetDatabase实例
         */
        fun getDatabase(context: Context): BudgetDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BudgetDatabase::class.java,
                    DATABASE_NAME
                )
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * 获取内存数据库实例
         * 主要用于测试，数据不会持久化到磁盘
         * 
         * @param context 应用上下文
         * @return 内存数据库实例
         */
        fun getInMemoryDatabase(context: Context): BudgetDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                BudgetDatabase::class.java
            )
            .allowMainThreadQueries() // 仅用于测试
            .build()
        }
        
        /**
         * 清除数据库实例
         * 主要用于测试场景
         */
        fun clearInstance() {
            INSTANCE = null
        }
    }
}

/**
 * Room类型转换器
 * 
 * 用于处理Room不直接支持的数据类型转换
 * 目前项目中使用的都是Room原生支持的类型，所以暂时为空
 * 为将来可能的扩展预留
 */
class Converters {
    // 目前不需要任何类型转换器
    // 如果将来需要存储Date、List等复杂类型，可以在这里添加转换方法
    
    // 示例：日期转换（如果需要的话）
    // @TypeConverter
    // fun fromTimestamp(value: Long?): Date? {
    //     return value?.let { Date(it) }
    // }
    //
    // @TypeConverter
    // fun dateToTimestamp(date: Date?): Long? {
    //     return date?.time
    // }
}
