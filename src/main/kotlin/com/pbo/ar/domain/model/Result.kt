package com.pbo.ar.domain.model

/**
 * Sealed class Result - Wrapper untuk hasil operasi.
 * 
 * Abstraksi: Menyembunyikan detail success/error handling
 * Polimorfisme: Success dan Error memiliki behavior berbeda
 * 
 * @author PBO Project Team
 * @since 1.0.0
 */
sealed class Result<out T> {
    data class Success<T>(val data: T, val message: String = "Success") : Result<T>()
    data class Error(val message: String, val code: Int = 400) : Result<Nothing>()
    
    fun isSuccess(): Boolean = this is Success
    fun isError(): Boolean = this is Error
    
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }
    
    fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data), message)
        is Error -> this
    }
    
    companion object {
        fun <T> success(data: T, message: String = "Success"): Result<T> = Success(data, message)
        fun error(message: String, code: Int = 400): Result<Nothing> = Error(message, code)
    }
}
