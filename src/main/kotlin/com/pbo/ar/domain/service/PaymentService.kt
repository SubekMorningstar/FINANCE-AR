package com.pbo.ar.domain.service

import com.pbo.ar.data.repository.PaymentRepository
import com.pbo.ar.data.repository.AllocationRepository
import com.pbo.ar.data.repository.InvoiceRepository
import com.pbo.ar.data.repository.CustomerRepository
import com.pbo.ar.domain.model.*
import java.time.LocalDate

/**
 * PaymentService - Business logic untuk Payment.
 * 
 * Enkapsulasi: Mengelola pembayaran dan alokasi
 * Domain Invariant: Alokasi tidak boleh melebihi balance
 * 
 * @author PBO Project Team
 * @since 1.0.0
 */
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val allocationRepository: AllocationRepository,
    private val invoiceRepository: InvoiceRepository,
    private val customerRepository: CustomerRepository,
    private val invoiceService: InvoiceService
) {
    
    /**
     * Buat pembayaran baru.
     */
    fun createPayment(
        customerId: Int,
        amount: Double,
        paymentMethod: PaymentMethod,
        paymentDate: LocalDate = LocalDate.now(),
        reference: String = "",
        notes: String = ""
    ): Result<Payment> {
        // Validasi customer
        customerRepository.findById(customerId)
            ?: return Result.error("Customer tidak ditemukan", 404)
        
        if (amount <= 0) {
            return Result.error("Jumlah pembayaran harus lebih dari 0")
        }
        
        val paymentNumber = Payment.generateNumber(paymentRepository.getNextSequence())
        
        val payment = Payment(
            paymentNumber = paymentNumber,
            customerId = customerId,
            paymentDate = paymentDate,
            amount = amount,
            paymentMethod = paymentMethod,
            reference = reference.trim(),
            notes = notes.trim()
        )
        
        return try {
            val created = paymentRepository.create(payment)
            Result.success(created, "Pembayaran berhasil dibuat")
        } catch (e: Exception) {
            Result.error("Gagal membuat pembayaran: ${e.message}")
        }
    }
    
    /**
     * Alokasikan pembayaran ke invoice.
     */
    fun allocatePayment(
        paymentId: Int,
        invoiceId: Int,
        amount: Double
    ): Result<PaymentAllocation> {
        val payment = paymentRepository.findById(paymentId)
            ?: return Result.error("Pembayaran tidak ditemukan", 404)
        
        val invoice = invoiceRepository.findById(invoiceId)
            ?: return Result.error("Invoice tidak ditemukan", 404)
        
        // Validasi customer
        if (payment.customerId != invoice.customerId) {
            return Result.error("Pembayaran dan invoice harus dari customer yang sama")
        }
        
        // Cek apakah invoice bisa menerima pembayaran
        if (!invoice.canBePaid()) {
            return Result.error("Invoice tidak dapat menerima pembayaran (status: ${invoice.status.toDisplayString()})")
        }
        
        // Cek sisa yang belum dialokasikan
        val allocatedAmount = allocationRepository.getAllocatedAmount(paymentId)
        val unallocated = payment.amount - allocatedAmount
        
        if (amount > unallocated) {
            return Result.error("Jumlah melebihi sisa pembayaran (tersedia: Rp $unallocated)")
        }
        
        // Cek sisa tagihan invoice
        val invoiceBalance = invoice.getBalanceDue()
        if (amount > invoiceBalance) {
            return Result.error("Jumlah melebihi sisa tagihan invoice (tersedia: Rp $invoiceBalance)")
        }
        
        val allocation = PaymentAllocation(
            paymentId = paymentId,
            invoiceId = invoiceId,
            amount = amount
        )
        
        return try {
            val created = allocationRepository.create(allocation)
            
            // Update invoice paid amount
            val newPaidAmount = allocationRepository.getInvoicePaidAmount(invoiceId)
            invoiceService.updateInvoicePaidAmount(invoiceId, newPaidAmount)
            
            Result.success(created, "Alokasi berhasil")
        } catch (e: Exception) {
            Result.error("Gagal mengalokasikan: ${e.message}")
        }
    }
    
    /**
     * Auto-allocate payment ke invoice outstanding (FIFO).
     */
    fun autoAllocatePayment(paymentId: Int): Result<List<PaymentAllocation>> {
        val payment = paymentRepository.findById(paymentId)
            ?: return Result.error("Pembayaran tidak ditemukan", 404)
        
        val allocatedAmount = allocationRepository.getAllocatedAmount(paymentId)
        var remaining = payment.amount - allocatedAmount
        
        if (remaining <= 0) {
            return Result.error("Pembayaran sudah dialokasikan seluruhnya")
        }
        
        // Get unpaid invoices sorted by due date
        val invoices = invoiceRepository.findUnpaidByCustomer(payment.customerId)
        val allocations = mutableListOf<PaymentAllocation>()
        
        for (invoice in invoices) {
            if (remaining <= 0) break
            
            val balance = invoice.getBalanceDue()
            val allocAmount = minOf(remaining, balance)
            
            if (allocAmount > 0) {
                val result = allocatePayment(paymentId, invoice.invoiceId, allocAmount)
                if (result is Result.Success) {
                    allocations.add(result.data)
                    remaining -= allocAmount
                }
            }
        }
        
        return if (allocations.isNotEmpty()) {
            Result.success(allocations, "Auto-alokasi berhasil ke ${allocations.size} invoice")
        } else {
            Result.error("Tidak ada invoice yang dapat dialokasi")
        }
    }
    
    /**
     * Ambil pembayaran dengan alokasi.
     */
    fun getPaymentById(id: Int): Payment? = paymentRepository.findById(id)
    
    fun getAllocations(paymentId: Int): List<PaymentAllocation> = allocationRepository.findByPayment(paymentId)
    
    fun getAllPayments(): List<Payment> = paymentRepository.findAll()
    
    fun getPaymentsByCustomer(customerId: Int): List<Payment> = paymentRepository.findByCustomer(customerId)
    
    /**
     * Get unallocated amount dari payment.
     */
    fun getUnallocatedAmount(paymentId: Int): Double {
        val payment = paymentRepository.findById(paymentId) ?: return 0.0
        val allocated = allocationRepository.getAllocatedAmount(paymentId)
        return payment.amount - allocated
    }
}
