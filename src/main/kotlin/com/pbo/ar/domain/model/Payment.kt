package com.pbo.ar.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Sealed class PaymentMethod - Metode pembayaran.
 * 
 * Polimorfisme: Setiap method memiliki representasi berbeda
 * 
 * @author PBO Project Team
 * @since 1.0.0
 */
sealed class PaymentMethod(val value: String) {
    data object CASH : PaymentMethod("CASH")
    data object BANK_TRANSFER : PaymentMethod("BANK_TRANSFER")
    data object CREDIT_CARD : PaymentMethod("CREDIT_CARD")
    data object CHECK : PaymentMethod("CHECK")
    data object OTHER : PaymentMethod("OTHER")
    
    fun toDisplayString(): String = when (this) {
        CASH -> "Tunai"
        BANK_TRANSFER -> "Transfer Bank"
        CREDIT_CARD -> "Kartu Kredit"
        CHECK -> "Cek/Giro"
        OTHER -> "Lainnya"
    }
    
    companion object {
        fun fromString(value: String): PaymentMethod = when (value.uppercase()) {
            "CASH" -> CASH
            "BANK_TRANSFER" -> BANK_TRANSFER
            "CREDIT_CARD" -> CREDIT_CARD
            "CHECK" -> CHECK
            "OTHER" -> OTHER
            else -> OTHER
        }
    }
}

/**
 * Payment Entity - Pembayaran dari pelanggan.
 * 
 * Domain Invariant: Amount harus positif
 * 
 * @author PBO Project Team
 * @since 1.0.0
 */
data class Payment(
    val paymentId: Int = 0,
    val paymentNumber: String,
    val customerId: Int,
    val paymentDate: LocalDate = LocalDate.now(),
    val amount: Double,
    val paymentMethod: PaymentMethod = PaymentMethod.BANK_TRANSFER,
    val reference: String = "",
    val notes: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    init {
        require(amount > 0) { "Payment amount harus lebih dari 0" }
    }
    
    /**
     * Validasi payment.
     */
    fun isValid(): Boolean {
        return paymentNumber.isNotBlank() &&
               customerId > 0 &&
               amount > 0
    }
    
    companion object {
        fun generateNumber(sequence: Int): String {
            val year = LocalDate.now().year
            return "PAY/$year/${String.format("%05d", sequence)}"
        }
    }
}

/**
 * PaymentAllocation - Alokasi pembayaran ke faktur.
 * 
 * Domain Invariant: 
 * - Amount tidak boleh melebihi payment unallocated
 * - Amount tidak boleh melebihi invoice balance
 * 
 * @author PBO Project Team
 * @since 1.0.0
 */
data class PaymentAllocation(
    val allocationId: Int = 0,
    val paymentId: Int,
    val invoiceId: Int,
    val amount: Double,
    val allocatedAt: LocalDateTime = LocalDateTime.now()
) {
    init {
        require(amount > 0) { "Allocation amount harus lebih dari 0" }
    }
    
    /**
     * Validasi allocation.
     */
    fun isValid(): Boolean {
        return paymentId > 0 &&
               invoiceId > 0 &&
               amount > 0
    }
}
