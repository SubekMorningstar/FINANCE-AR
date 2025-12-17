package com.pbo.ar.di

import com.pbo.ar.data.repository.*
import com.pbo.ar.domain.service.*

/**
 * AppModule - Dependency Injection sederhana.
 * 
 * @author PBO Project Team
 * @since 1.0.0
 */
object AppModule {
    
    // Repositories
    val customerRepository by lazy { CustomerRepository() }
    val invoiceRepository by lazy { InvoiceRepository() }
    val lineItemRepository by lazy { LineItemRepository() }
    val paymentRepository by lazy { PaymentRepository() }
    val allocationRepository by lazy { AllocationRepository() }
    
    // Services
    val customerService by lazy { 
        CustomerService(customerRepository, invoiceRepository) 
    }
    
    val invoiceService by lazy { 
        InvoiceService(invoiceRepository, lineItemRepository, customerRepository) 
    }
    
    val paymentService by lazy { 
        PaymentService(
            paymentRepository, 
            allocationRepository, 
            invoiceRepository, 
            customerRepository,
            invoiceService
        ) 
    }
    
    val reportService by lazy { 
        ReportService(invoiceRepository, paymentRepository, allocationRepository, customerRepository) 
    }
}
