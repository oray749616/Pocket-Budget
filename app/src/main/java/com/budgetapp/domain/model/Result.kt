package com.budgetapp.domain.model

/**
 * Result包装类
 * 
 * 用于封装操作结果，提供统一的成功和失败状态处理。
 * 支持链式调用和函数式编程风格的错误处理。
 * 
 * Requirements:
 * - 6.4: 显示错误信息并提供重试选项
 */
sealed class Result<out T> {
    
    /**
     * 成功状态
     * 
     * @param data 成功时返回的数据
     */
    data class Success<T>(val data: T) : Result<T>()
    
    /**
     * 失败状态
     * 
     * @param exception 失败时的异常信息
     */
    data class Error(val exception: Throwable) : Result<Nothing>()
    
    /**
     * 加载状态
     * 用于表示操作正在进行中
     */
    object Loading : Result<Nothing>()
    
    /**
     * 检查是否为成功状态
     * 
     * @return true 如果是成功状态
     */
    val isSuccess: Boolean
        get() = this is Success
    
    /**
     * 检查是否为失败状态
     * 
     * @return true 如果是失败状态
     */
    val isError: Boolean
        get() = this is Error
    
    /**
     * 检查是否为加载状态
     * 
     * @return true 如果是加载状态
     */
    val isLoading: Boolean
        get() = this is Loading
    
    /**
     * 获取成功时的数据
     * 
     * @return 成功时的数据，失败时返回null
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }
    
    /**
     * 获取失败时的异常
     * 
     * @return 失败时的异常，成功时返回null
     */
    fun exceptionOrNull(): Throwable? = when (this) {
        is Error -> exception
        else -> null
    }
}

/**
 * Result扩展函数：成功时执行操作
 * 
 * @param action 成功时要执行的操作
 * @return 原始Result对象，支持链式调用
 */
inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) {
        action(data)
    }
    return this
}

/**
 * Result扩展函数：失败时执行操作
 * 
 * @param action 失败时要执行的操作
 * @return 原始Result对象，支持链式调用
 */
inline fun <T> Result<T>.onError(action: (Throwable) -> Unit): Result<T> {
    if (this is Result.Error) {
        action(exception)
    }
    return this
}

/**
 * Result扩展函数：加载时执行操作
 * 
 * @param action 加载时要执行的操作
 * @return 原始Result对象，支持链式调用
 */
inline fun <T> Result<T>.onLoading(action: () -> Unit): Result<T> {
    if (this is Result.Loading) {
        action()
    }
    return this
}

/**
 * Result扩展函数：映射成功数据
 * 
 * @param transform 数据转换函数
 * @return 转换后的Result对象
 */
inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Error -> this
    is Result.Loading -> this
}

/**
 * Result扩展函数：平铺映射
 * 
 * @param transform 返回Result的转换函数
 * @return 转换后的Result对象
 */
inline fun <T, R> Result<T>.flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
    is Result.Success -> transform(data)
    is Result.Error -> this
    is Result.Loading -> this
}

/**
 * Result扩展函数：获取数据或默认值
 * 
 * @param defaultValue 失败时的默认值
 * @return 成功时的数据或默认值
 */
fun <T> Result<T>.getOrDefault(defaultValue: T): T = when (this) {
    is Result.Success -> data
    else -> defaultValue
}

/**
 * Result扩展函数：获取数据或执行函数获取默认值
 * 
 * @param defaultValue 失败时执行的函数
 * @return 成功时的数据或函数执行结果
 */
inline fun <T> Result<T>.getOrElse(defaultValue: (Throwable?) -> T): T = when (this) {
    is Result.Success -> data
    is Result.Error -> defaultValue(exception)
    is Result.Loading -> defaultValue(null)
}

/**
 * 创建成功的Result
 * 
 * @param data 成功数据
 * @return Success Result
 */
fun <T> resultOf(data: T): Result<T> = Result.Success(data)

/**
 * 创建失败的Result
 * 
 * @param exception 异常信息
 * @return Error Result
 */
fun <T> errorOf(exception: Throwable): Result<T> = Result.Error(exception)

/**
 * 创建失败的Result
 * 
 * @param message 错误消息
 * @return Error Result
 */
fun <T> errorOf(message: String): Result<T> = Result.Error(Exception(message))

/**
 * 创建加载中的Result
 * 
 * @return Loading Result
 */
fun <T> loadingResult(): Result<T> = Result.Loading

/**
 * 安全执行操作并返回Result
 * 
 * @param block 要执行的操作
 * @return 操作结果的Result包装
 */
inline fun <T> safeCall(block: () -> T): Result<T> = try {
    Result.Success(block())
} catch (e: Exception) {
    Result.Error(e)
}

/**
 * 安全执行挂起操作并返回Result
 * 
 * @param block 要执行的挂起操作
 * @return 操作结果的Result包装
 */
suspend inline fun <T> safeSuspendCall(crossinline block: suspend () -> T): Result<T> = try {
    Result.Success(block())
} catch (e: Exception) {
    Result.Error(e)
}
