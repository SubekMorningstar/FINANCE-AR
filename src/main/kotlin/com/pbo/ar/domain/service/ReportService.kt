package com.pbo.ar.domain.service

import com.pbo.ar.data.repository.InvoiceRepository
import com.pbo.ar.data.repository.PaymentRepository
import com.pbo.ar.data.repository.AllocationRepository
import com.pbo.ar.data.repository.CustomerRepository
import com.pbo.ar.domain.model.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Report DTOs
 */
data class AgingBucket(
    val label: String,
    val invoiceCount: Int,
    val totalAmount: Double
)

data class AgingReport(
    val asOfDate: LocalDate,
    val buckets: List<AgingBucket>,
    val totalReceivable: Double
)

data class CustomerStatement(
    val customer: Customer,
    val invoices: List<Invoice>,
    val payments: List<Payment>,
    val totalInvoiced: Double,
    val totalPaid: Double,
    val balance: Double,
    val startDate: LocalDate,
    val endDate: LocalDate
)

data class ReceivableSummary(
    val totalCustomers: Int,
    val totalInvoices: Int,
    val totalReceivable: Double,
    val totalOverdue: Double,
    val totalCollected: Double,
    val collectionRate: Double
)

/**
 * ReportService - Laporan dan analitik AR.
 * 
 * @author PBO Project Team
 * @since 1.0.0
 */
class ReportService(
    private val invoiceRepository: InvoiceRepository,
    private val paymentRepository: PaymentRepository,
    private val allocationRepository: AllocationRepository,
    private val customerRepository: CustomerRepository
) {
    
    /**
     * Generate Aging Report - Pengelompokan piutang berdasarkan umur.
     */
    fun getAgingReport(): AgingReport {
        val today = LocalDate.now()
        val invoices = invoiceRepository.findAll()
            .filter { it.status in listOf(InvoiceStatus.SENT, InvoiceStatus.PARTIAL_PAID, InvoiceStatus.OVERDUE) }
        
        // Aging buckets: Current, 1-30, 31-60, 61-90, >90
        val buckets = mutableMapOf(
            "Current" to mutableListOf<Invoice>(),
            "1-30 Days" to mutableListOf<Invoice>(),
            "31-60 Days" to mutableListOf<Invoice>(),
            "61-90 Days" to mutableListOf<Invoice>(),
            ">90 Days" to mutableListOf<Invoice>()
        )
        
        invoices.forEach { invoice ->
            val daysPastDue = ChronoUnit.DAYS.between(invoice.dueDate, today)
            val bucket = when {
                daysPastDue <= 0 -> "Current"
                daysPastDue <= 30 -> "1-30 Days"
                daysPastDue <= 60 -> "31-60 Days"
                daysPastDue <= 90 -> "61-90 Days"
                else -> ">90 Days"
            }
            buckets[bucket]?.add(invoice)
        }
        
        val agingBuckets = buckets.map { (label, list) ->
            AgingBucket(
                label = label,
                invoiceCount = list.size,
                totalAmount = list.sumOf { it.getBalanceDue() }
            )
        }
        
        return AgingReport(
            asOfDate = today,
            buckets = agingBuckets,
            totalReceivable = invoices.sumOf { it.getBalanceDue() }
        )
    }
    
    /**
     * Generate Customer Statement.
     */
    fun getCustomerStatement(
        customerId: Int,
        startDate: LocalDate = LocalDate.now().minusMonths(3),
        endDate: LocalDate = LocalDate.now()
    ): CustomerStatement? {
        val customer = customerRepository.findById(customerId) ?: return null
        
        val invoices = invoiceRepository.findByCustomer(customerId)
            .filter { 
                !it.invoiceDate.isBefore(startDate) && 
                !it.invoiceDate.isAfter(endDate) 
            }
        
        val payments = paymentRepository.findByCustomer(customerId)
            .filter {
                !it.paymentDate.isBefore(startDate) &&
                !it.paymentDate.isAfter(endDate)
            }
        
        val totalInvoiced = invoices.sumOf { it.totalAmount }
        val totalPaid = payments.sumOf { it.amount }
        
        return CustomerStatement(
            customer = customer,
            invoices = invoices,
            payments = payments,
            totalInvoiced = totalInvoiced,
            totalPaid = totalPaid,
            balance = invoiceRepository.getCustomerBalance(customerId),
            startDate = startDate,
            endDate = endDate
        )
    }
    
    /**
     * Get Receivable Summary.
     */
    fun getReceivableSummary(): ReceivableSummary {
        val customers = customerRepository.findAll()
        val invoices = invoiceRepository.findAll()
        val payments = paymentRepository.findAll()
        
        val activeInvoices = invoices.filter { 
            it.status in listOf(InvoiceStatus.SENT, InvoiceStatus.PARTIAL_PAID, InvoiceStatus.OVERDUE) 
        }
        
        val overdueInvoices = invoices.filter { it.isOverdue() }
        
        val totalReceivable = activeInvoices.sumOf { it.getBalanceDue() }
        val totalOverdue = overdueInvoices.sumOf { it.getBalanceDue() }
        val totalInvoiced = invoices.filter { it.status != InvoiceStatus.CANCELLED }.sumOf { it.totalAmount }
        val totalCollected = payments.sumOf { it.amount }
        
        val collectionRate = if (totalInvoiced > 0) {
            (totalCollected / totalInvoiced) * 100
        } else 0.0
        
        return ReceivableSummary(
            totalCustomers = customers.size,
            totalInvoices = activeInvoices.size,
            totalReceivable = totalReceivable,
            totalOverdue = totalOverdue,
            totalCollected = totalCollected,
            collectionRate = collectionRate
        )
    }
}
