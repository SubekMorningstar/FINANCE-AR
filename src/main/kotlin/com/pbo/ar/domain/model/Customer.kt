package com.pbo.ar.domain.model

import java.time.LocalDateTime

/**
 * Customer Entity - Merepresentasikan pelanggan dalam sistem AR.
 * 
 * Enkapsulasi: Properti privat dengan validasi pada akses
 * Domain Invariant: Credit limit tidak boleh negatif
 * 
 * @author PBO Project Team
 * @since 1.0.0
 */
data class Customer(
    val customerId: Int = 0,
    val code: String,
    val name: String,
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val creditLimit: Double = 0.0,
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    init {
        require(creditLimit >= 0) { "Credit limit tidak boleh negatif" }
    }
    
    /**
     * Validasi data customer.
     */
    fun isValid(): Boolean {
        return code.isNotBlank() && 
               name.isNotBlank() && 
               creditLimit >= 0
    }
    
    /**
     * Cek apakah customer memiliki kredit tersedia.
     * @param amount Jumlah kredit yang dibutuhkan
     * @param currentBalance Saldo piutang saat ini
     */
    fun hasAvailableCredit(amount: Double, currentBalance: Double): Boolean {
        return (currentBalance + amount) <= creditLimit
    }
    
    /**
     * Format tampilan nama customer.
     */
    fun getDisplayName(): String = "[$code] $name"
    
    companion object {
        /**
         * Generate customer code otomatis.
         */
        fun generateCode(sequence: Int): String {
            return "CUST${String.format("%05d", sequence)}"
        }
    }
}
