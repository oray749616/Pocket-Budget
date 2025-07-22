package com.budgetapp.data.database.converters

import androidx.room.TypeConverter
import java.util.Date

/**
 * Room数据库类型转换器
 * 
 * 提供自定义数据类型与Room支持的基本类型之间的转换方法
 * 使用@TypeConverter注解标记转换方法
 * 
 * Requirements:
 * - 6.1: 支持数据库中复杂数据类型的存储和读取
 */
class Converters {
    
    /**
     * 将时间戳转换为Date对象
     * 
     * @param value 时间戳（毫秒），可能为null
     * @return Date对象，如果输入为null则返回null
     */
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }
    
    /**
     * 将Date对象转换为时间戳
     * 
     * @param date Date对象，可能为null
     * @return 时间戳（毫秒），如果输入为null则返回null
     */
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
    
    /**
     * 将逗号分隔的字符串转换为字符串列表
     * 用于存储标签或分类等列表数据
     * 
     * @param value 逗号分隔的字符串，可能为null
     * @return 字符串列表，如果输入为null或空则返回空列表
     */
    @TypeConverter
    fun fromString(value: String?): List<String> {
        return if (value.isNullOrBlank()) {
            emptyList()
        } else {
            value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
    }
    
    /**
     * 将字符串列表转换为逗号分隔的字符串
     * 
     * @param list 字符串列表，可能为null
     * @return 逗号分隔的字符串，如果输入为null或空则返回空字符串
     */
    @TypeConverter
    fun fromListString(list: List<String>?): String {
        return list?.joinToString(",") ?: ""
    }
    
    /**
     * 将布尔值转换为整数
     * Room数据库中布尔值存储为整数：1表示true，0表示false
     * 
     * @param value 布尔值，可能为null
     * @return 整数值：1表示true，0表示false，null输入返回null
     */
    @TypeConverter
    fun fromBoolean(value: Boolean?): Int? {
        return when (value) {
            true -> 1
            false -> 0
            null -> null
        }
    }
    
    /**
     * 将整数转换为布尔值
     * 
     * @param value 整数值，可能为null
     * @return 布尔值：1表示true，0表示false，其他值表示false，null输入返回null
     */
    @TypeConverter
    fun toBoolean(value: Int?): Boolean? {
        return when (value) {
            1 -> true
            0 -> false
            null -> null
            else -> false
        }
    }
}
