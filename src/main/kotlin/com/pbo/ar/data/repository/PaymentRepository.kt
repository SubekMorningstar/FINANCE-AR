package com.pbo.ar.data.repository

import com.pbo.ar.data.database.PaymentsTable
import com.pbo.ar.data.database.PaymentAllocationsTable
import com.pbo.ar.domain.model.Payment
import com.pbo.ar.domain.model.PaymentMethod
import com.pbo.ar.domain.model.PaymentAllocation
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

/**
 * PaymentRepository - Repository Pattern untuk Payment.
 * 
 * @author PBO Project Team
 * @since 1.0.0
 */
class PaymentRepository {
    
    fun create(payment: Payment): Payment = transaction {
        val id = PaymentsTable.insert {
            it[paymentNumber] = payment.paymentNumber
            it[customerId] = payment.customerId
            it[paymentDate] = payment.paymentDate
            it[amount] = payment.amount
            it[paymentMethod] = payment.paymentMethod.value
            it[reference] = payment.reference
            it[notes] = payment.notes
            it[createdAt] = LocalDateTime.now()
        } get PaymentsTable.paymentId
        
        payment.copy(paymentId = id)
    }
    
    fun findById(id: Int): Payment? = transaction {
        PaymentsTable.select(PaymentsTable.paymentId eq id)
            .map { toPayment(it) }
            .singleOrNull()
    }
    
    fun findByCustomer(customerId: Int): List<Payment> = transaction {
        PaymentsTable.select(PaymentsTable.customerId eq customerId)
            .orderBy(PaymentsTable.paymentDate, SortOrder.DESC)
            .map { toPayment(it) }
    }
    
    fun findAll(): List<Payment> = transaction {
        PaymentsTable.selectAll()
            .orderBy(PaymentsTable.paymentDate, SortOrder.DESC)
            .map { toPayment(it) }
    }
    
    fun delete(id: Int): Boolean = transaction {
        PaymentsTable.deleteWhere { paymentId eq id } > 0
    }
    
    fun getNextSequence(): Int = transaction {
        (PaymentsTable.selectAll().count().toInt()) + 1
    }
    
    private fun toPayment(row: ResultRow) = Payment(
        paymentId = row[PaymentsTable.paymentId],
        paymentNumber = row[PaymentsTable.paymentNumber],
        customerId = row[PaymentsTable.customerId],
        paymentDate = row[PaymentsTable.paymentDate],
        amount = row[PaymentsTable.amount],
        paymentMethod = PaymentMethod.fromString(row[PaymentsTable.paymentMethod]),
        reference = row[PaymentsTable.reference],
        notes = row[PaymentsTable.notes],
        createdAt = row[PaymentsTable.createdAt]
    )
}

/**
 * AllocationRepository - Repository untuk Payment Allocation.
 */
class AllocationRepository {
    
    fun create(allocation: PaymentAllocation): PaymentAllocation = transaction {
        val id = PaymentAllocationsTable.insert {
            it[paymentId] = allocation.paymentId
            it[invoiceId] = allocation.invoiceId
            it[amount] = allocation.amount
            it[allocatedAt] = LocalDateTime.now()
        } get PaymentAllocationsTable.allocationId
        
        allocation.copy(allocationId = id)
    }
    
    fun findByPayment(paymentId: Int): List<PaymentAllocation> = transaction {
        PaymentAllocationsTable.select(PaymentAllocationsTable.paymentId eq paymentId)
            .map { toAllocation(it) }
    }
    
    fun findByInvoice(invoiceId: Int): List<PaymentAllocation> = transaction {
        PaymentAllocationsTable.select(PaymentAllocationsTable.invoiceId eq invoiceId)
            .map { toAllocation(it) }
    }
    
    fun getAllocatedAmount(paymentId: Int): Double = transaction {
        PaymentAllocationsTable.select(PaymentAllocationsTable.paymentId eq paymentId)
            .sumOf { it[PaymentAllocationsTable.amount] }
    }
    
    fun getInvoicePaidAmount(invoiceId: Int): Double = transaction {
        PaymentAllocationsTable.select(PaymentAllocationsTable.invoiceId eq invoiceId)
            .sumOf { it[PaymentAllocationsTable.amount] }
    }
    
    fun delete(allocationId: Int): Boolean = transaction {
        PaymentAllocationsTable.deleteWhere { PaymentAllocationsTable.allocationId eq allocationId } > 0
    }
    
    private fun toAllocation(row: ResultRow) = PaymentAllocation(
        allocationId = row[PaymentAllocationsTable.allocationId],
        paymentId = row[PaymentAllocationsTable.paymentId],
        invoiceId = row[PaymentAllocationsTable.invoiceId],
        amount = row[PaymentAllocationsTable.amount],
        allocatedAt = row[PaymentAllocationsTable.allocatedAt]
    )
}
