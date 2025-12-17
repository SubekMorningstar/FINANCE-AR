package com.pbo.ar.domain.service

import com.pbo.ar.data.repository.CustomerRepository
import com.pbo.ar.data.repository.InvoiceRepository
import com.pbo.ar.domain.model.Customer
import com.pbo.ar.domain.model.Result

/**
 * CustomerService - Business logic untuk Customer.
 * 
 * Enkapsulasi: Menyembunyikan detail operasi customer
 * 
 * @author PBO Project Team
 * @since 1.0.0
 */
class CustomerService(
    private val customerRepository: CustomerRepository,
    private val invoiceRepository: InvoiceRepository
) {
    
    /**
     * Buat customer baru dengan validasi.
     */
    fun createCustomer(
        name: String,
        email: String = "",
        phone: String = "",
        address: String = "",
        creditLimit: Double = 0.0
    ): Result<Customer> {
        // Validasi input
        if (name.isBlank()) {
            return Result.error("Nama customer tidak boleh kosong")
        }
        
        if (creditLimit < 0) {
            return Result.error("Credit limit tidak boleh negatif")
        }
        
        // Generate code
        val code = Customer.generateCode(customerRepository.getNextSequence())
        
        // Cek duplikasi code
        if (customerRepository.codeExists(code)) {
            return Result.error("Customer code sudah ada")
        }
        
        val customer = Customer(
            code = code,
            name = name.trim(),
            email = email.trim(),
            phone = phone.trim(),
            address = address.trim(),
            creditLimit = creditLimit
        )
        
        return try {
            val created = customerRepository.create(customer)
            Result.success(created, "Customer berhasil dibuat")
        } catch (e: Exception) {
            Result.error("Gagal membuat customer: ${e.message}")
        }
    }
    
    /**
     * Update data customer.
     */
    fun updateCustomer(
        customerId: Int,
        name: String,
        email: String,
        phone: String,
        address: String,
        creditLimit: Double
    ): Result<Customer> {
        val existing = customerRepository.findById(customerId)
            ?: return Result.error("Customer tidak ditemukan", 404)
        
        if (name.isBlank()) {
            return Result.error("Nama customer tidak boleh kosong")
        }
        
        val updated = existing.copy(
            name = name.trim(),
            email = email.trim(),
            phone = phone.trim(),
            address = address.trim(),
            creditLimit = creditLimit
        )
        
        return if (customerRepository.update(updated)) {
            Result.success(updated, "Customer berhasil diupdate")
        } else {
            Result.error("Gagal mengupdate customer")
        }
    }
    
    /**
     * Ambil customer by ID.
     */
    fun getCustomerById(id: Int): Customer? = customerRepository.findById(id)
    
    /**
     * Ambil semua customer.
     */
    fun getAllCustomers(): List<Customer> = customerRepository.findAll()
    
    /**
     * Ambil saldo piutang customer.
     */
    fun getCustomerBalance(customerId: Int): Double = invoiceRepository.getCustomerBalance(customerId)
    
    /**
     * Hapus customer jika tidak memiliki transaksi.
     */
    fun deleteCustomer(customerId: Int): Result<Boolean> {
        val invoices = invoiceRepository.findByCustomer(customerId)
        if (invoices.isNotEmpty()) {
            return Result.error("Customer memiliki invoice, tidak dapat dihapus")
        }
        
        return if (customerRepository.delete(customerId)) {
            Result.success(true, "Customer berhasil dihapus")
        } else {
            Result.error("Customer tidak ditemukan", 404)
        }
    }
}
