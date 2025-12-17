package com.pbo.ar.domain.service

import com.pbo.ar.data.repository.InvoiceRepository
import com.pbo.ar.data.repository.LineItemRepository
import com.pbo.ar.data.repository.CustomerRepository
import com.pbo.ar.domain.model.*
import java.time.LocalDate

/**
 * InvoiceService - Business logic untuk Invoice.
 * 
 * Enkapsulasi: Mengelola lifecycle invoice
 * Domain Invariant: Total harus sama dengan sum line items + tax
 * 
 * @author PBO Project Team
 * @since 1.0.0
 */
class InvoiceService(
    private val invoiceRepository: InvoiceRepository,
    private val lineItemRepository: LineItemRepository,
    private val customerRepository: CustomerRepository
) {
    
    /**
     * Buat invoice baru (draft).
     */
    fun createInvoice(
        customerId: Int,
        dueDate: LocalDate,
        taxRate: Double = 11.0,
        notes: String = ""
    ): Result<Invoice> {
        // Validasi customer
        val customer = customerRepository.findById(customerId)
            ?: return Result.error("Customer tidak ditemukan", 404)
        
        // Validasi due date
        if (dueDate.isBefore(LocalDate.now())) {
            return Result.error("Due date tidak boleh di masa lalu")
        }
        
        val invoiceNumber = Invoice.generateNumber(invoiceRepository.getNextSequence())
        
        val invoice = Invoice(
            invoiceNumber = invoiceNumber,
            customerId = customerId,
            invoiceDate = LocalDate.now(),
            dueDate = dueDate,
            taxRate = taxRate,
            notes = notes.trim()
        )
        
        return try {
            val created = invoiceRepository.create(invoice)
            Result.success(created, "Invoice berhasil dibuat")
        } catch (e: Exception) {
            Result.error("Gagal membuat invoice: ${e.message}")
        }
    }
    
    /**
     * Tambah line item ke invoice.
     */
    fun addLineItem(
        invoiceId: Int,
        description: String,
        quantity: Int,
        unitPrice: Double
    ): Result<InvoiceLineItem> {
        val invoice = invoiceRepository.findById(invoiceId)
            ?: return Result.error("Invoice tidak ditemukan", 404)
        
        // Hanya draft yang bisa diedit
        if (invoice.status != InvoiceStatus.DRAFT) {
            return Result.error("Invoice sudah dikirim, tidak dapat diedit")
        }
        
        if (description.isBlank()) {
            return Result.error("Deskripsi tidak boleh kosong")
        }
        
        if (quantity <= 0) {
            return Result.error("Quantity harus lebih dari 0")
        }
        
        if (unitPrice < 0) {
            return Result.error("Harga tidak boleh negatif")
        }
        
        val item = InvoiceLineItem(
            invoiceId = invoiceId,
            description = description.trim(),
            quantity = quantity,
            unitPrice = unitPrice
        ).calculateAmount()
        
        return try {
            val created = lineItemRepository.create(item)
            
            // Recalculate invoice totals
            recalculateInvoiceTotals(invoiceId)
            
            Result.success(created, "Item berhasil ditambahkan")
        } catch (e: Exception) {
            Result.error("Gagal menambah item: ${e.message}")
        }
    }
    
    /**
     * Hapus line item dari invoice.
     */
    fun removeLineItem(invoiceId: Int, lineItemId: Int): Result<Boolean> {
        val invoice = invoiceRepository.findById(invoiceId)
            ?: return Result.error("Invoice tidak ditemukan", 404)
        
        if (invoice.status != InvoiceStatus.DRAFT) {
            return Result.error("Invoice sudah dikirim, tidak dapat diedit")
        }
        
        return if (lineItemRepository.delete(lineItemId)) {
            recalculateInvoiceTotals(invoiceId)
            Result.success(true, "Item berhasil dihapus")
        } else {
            Result.error("Item tidak ditemukan", 404)
        }
    }
    
    /**
     * Kirim invoice ke customer.
     */
    fun sendInvoice(invoiceId: Int): Result<Invoice> {
        val invoice = invoiceRepository.findById(invoiceId)
            ?: return Result.error("Invoice tidak ditemukan", 404)
        
        if (!invoice.status.canTransitionTo(InvoiceStatus.SENT)) {
            return Result.error("Invoice tidak dapat dikirim dari status ${invoice.status.toDisplayString()}")
        }
        
        // Cek apakah ada line items
        val items = lineItemRepository.findByInvoice(invoiceId)
        if (items.isEmpty()) {
            return Result.error("Invoice harus memiliki minimal 1 item")
        }
        
        return if (invoiceRepository.updateStatus(invoiceId, InvoiceStatus.SENT)) {
            val updated = invoiceRepository.findById(invoiceId)!!
            Result.success(updated, "Invoice berhasil dikirim")
        } else {
            Result.error("Gagal mengirim invoice")
        }
    }
    
    /**
     * Batalkan invoice.
     */
    fun cancelInvoice(invoiceId: Int): Result<Invoice> {
        val invoice = invoiceRepository.findById(invoiceId)
            ?: return Result.error("Invoice tidak ditemukan", 404)
        
        if (!invoice.status.canTransitionTo(InvoiceStatus.CANCELLED)) {
            return Result.error("Invoice tidak dapat dibatalkan dari status ${invoice.status.toDisplayString()}")
        }
        
        if (invoice.paidAmount > 0) {
            return Result.error("Invoice sudah ada pembayaran, tidak dapat dibatalkan")
        }
        
        return if (invoiceRepository.updateStatus(invoiceId, InvoiceStatus.CANCELLED)) {
            val updated = invoiceRepository.findById(invoiceId)!!
            Result.success(updated, "Invoice berhasil dibatalkan")
        } else {
            Result.error("Gagal membatalkan invoice")
        }
    }
    
    /**
     * Ambil invoice dengan line items.
     */
    fun getInvoiceById(id: Int): Invoice? = invoiceRepository.findById(id)
    
    fun getLineItems(invoiceId: Int): List<InvoiceLineItem> = lineItemRepository.findByInvoice(invoiceId)
    
    fun getAllInvoices(): List<Invoice> = invoiceRepository.findAll()
    
    fun getInvoicesByCustomer(customerId: Int): List<Invoice> = invoiceRepository.findByCustomer(customerId)
    
    fun getOverdueInvoices(): List<Invoice> = invoiceRepository.findOverdue()
    
    fun getUnpaidInvoicesByCustomer(customerId: Int): List<Invoice> = invoiceRepository.findUnpaidByCustomer(customerId)
    
    /**
     * Recalculate invoice totals berdasarkan line items.
     */
    private fun recalculateInvoiceTotals(invoiceId: Int) {
        val invoice = invoiceRepository.findById(invoiceId) ?: return
        val items = lineItemRepository.findByInvoice(invoiceId)
        val updated = invoice.recalculateTotals(items)
        invoiceRepository.update(updated)
    }
    
    /**
     * Update paid amount setelah payment allocation.
     */
    fun updateInvoicePaidAmount(invoiceId: Int, newPaidAmount: Double) {
        val invoice = invoiceRepository.findById(invoiceId) ?: return
        val newStatus = when {
            newPaidAmount >= invoice.totalAmount -> InvoiceStatus.PAID
            newPaidAmount > 0 -> InvoiceStatus.PARTIAL_PAID
            else -> invoice.status
        }
        invoiceRepository.updatePaidAmount(invoiceId, newPaidAmount, newStatus)
    }
}
