package com.pbo.ar.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Sealed class InvoiceStatus - Status faktur dengan state machine.
 * 
 * Polimorfisme: Setiap status memiliki behavior berbeda
 * Domain Invariant: Transisi status harus valid
 * 
 * @author PBO Project Team
 * @since 1.0.0
 */
sealed class InvoiceStatus(val value: String) {
    data object DRAFT : InvoiceStatus("DRAFT")
    data object SENT : InvoiceStatus("SENT")
    data object PARTIAL_PAID : InvoiceStatus("PARTIAL_PAID")
    data object PAID : InvoiceStatus("PAID")
    data object OVERDUE : InvoiceStatus("OVERDUE")
    data object CANCELLED : InvoiceStatus("CANCELLED")
    
    /**
     * Validasi transisi status yang diperbolehkan.
     */
    fun canTransitionTo(newStatus: InvoiceStatus): Boolean {
        return when (this) {
            DRAFT -> newStatus in listOf(SENT, CANCELLED)
            SENT -> newStatus in listOf(PARTIAL_PAID, PAID, OVERDUE, CANCELLED)
            PARTIAL_PAID -> newStatus in listOf(PAID, OVERDUE)
            OVERDUE -> newStatus in listOf(PARTIAL_PAID, PAID)
            PAID -> false // Final state
            CANCELLED -> false // Final state
        }
    }
    
    fun toDisplayString(): String = when (this) {
        DRAFT -> "Draft"
        SENT -> "Terkirim"
        PARTIAL_PAID -> "Dibayar Sebagian"
        PAID -> "Lunas"
        OVERDUE -> "Jatuh Tempo"
        CANCELLED -> "Dibatalkan"
    }
    
    companion object {
        fun fromString(value: String): InvoiceStatus = when (value.uppercase()) {
            "DRAFT" -> DRAFT
            "SENT" -> SENT
            "PARTIAL_PAID" -> PARTIAL_PAID
            "PAID" -> PAID
            "OVERDUE" -> OVERDUE
            "CANCELLED" -> CANCELLED
            else -> throw IllegalArgumentException("Unknown status: $value")
        }
    }
}

/**
 * Invoice Entity - Faktur penjualan.
 * 
 * Domain Invariant:
 * - Total = Subtotal + Tax
 * - Balance Due = Total - Paid >= 0
 * - Due Date >= Invoice Date
 * 
 * @author PBO Project Team
 * @since 1.0.0
 */
data class Invoice(
    val invoiceId: Int = 0,
    val invoiceNumber: String,
    val customerId: Int,
    val invoiceDate: LocalDate = LocalDate.now(),
    val dueDate: LocalDate,
    val status: InvoiceStatus = InvoiceStatus.DRAFT,
    val subtotal: Double = 0.0,
    val taxRate: Double = 11.0, // PPN 11%
    val taxAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val notes: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    init {
        require(paidAmount >= 0) { "Paid amount tidak boleh negatif" }
        require(paidAmount <= totalAmount || totalAmount == 0.0) { "Paid amount tidak boleh melebihi total" }
    }
    
    /**
     * Hitung sisa tagihan.
     */
    fun getBalanceDue(): Double = (totalAmount - paidAmount).coerceAtLeast(0.0)
    
    /**
     * Cek apakah faktur sudah jatuh tempo.
     */
    fun isOverdue(): Boolean = 
        LocalDate.now().isAfter(dueDate) && 
        status !in listOf(InvoiceStatus.PAID, InvoiceStatus.CANCELLED)
    
    /**
     * Cek apakah faktur bisa menerima pembayaran.
     */
    fun canBePaid(): Boolean = 
        status in listOf(InvoiceStatus.SENT, InvoiceStatus.PARTIAL_PAID, InvoiceStatus.OVERDUE) &&
        getBalanceDue() > 0
    
    /**
     * Validasi data invoice.
     */
    fun isValid(): Boolean {
        return invoiceNumber.isNotBlank() &&
               customerId > 0 &&
               dueDate >= invoiceDate
    }
    
    /**
     * Kalkulasi ulang totals berdasarkan line items.
     */
    fun recalculateTotals(lineItems: List<InvoiceLineItem>): Invoice {
        val newSubtotal = lineItems.sumOf { it.amount }
        val newTaxAmount = newSubtotal * (taxRate / 100)
        val newTotal = newSubtotal + newTaxAmount
        
        return copy(
            subtotal = newSubtotal,
            taxAmount = newTaxAmount,
            totalAmount = newTotal
        )
    }
    
    /**
     * Update status berdasarkan pembayaran.
     */
    fun updateStatusAfterPayment(newPaidAmount: Double): Invoice {
        val newStatus = when {
            newPaidAmount >= totalAmount -> InvoiceStatus.PAID
            newPaidAmount > 0 -> InvoiceStatus.PARTIAL_PAID
            else -> status
        }
        return copy(paidAmount = newPaidAmount, status = newStatus)
    }
    
    companion object {
        fun generateNumber(sequence: Int): String {
            val year = LocalDate.now().year
            return "INV/$year/${String.format("%05d", sequence)}"
        }
    }
}

/**
 * InvoiceLineItem - Detail item dalam faktur.
 * 
 * Domain Invariant: Amount = Quantity * UnitPrice
 * 
 * @author PBO Project Team
 * @since 1.0.0
 */
data class InvoiceLineItem(
    val lineItemId: Int = 0,
    val invoiceId: Int,
    val description: String,
    val quantity: Int = 1,
    val unitPrice: Double,
    val amount: Double = 0.0
) {
    init {
        require(quantity > 0) { "Quantity harus lebih dari 0" }
        require(unitPrice >= 0) { "Unit price tidak boleh negatif" }
    }
    
    /**
     * Hitung amount dari quantity dan unit price.
     */
    fun calculateAmount(): InvoiceLineItem {
        return copy(amount = quantity * unitPrice)
    }
    
    /**
     * Validasi line item.
     */
    fun isValid(): Boolean {
        return description.isNotBlank() &&
               quantity > 0 &&
               unitPrice >= 0
    }
}
