package com.pbo.ar.data.repository

import com.pbo.ar.data.database.InvoicesTable
import com.pbo.ar.data.database.InvoiceLineItemsTable
import com.pbo.ar.domain.model.Invoice
import com.pbo.ar.domain.model.InvoiceStatus
import com.pbo.ar.domain.model.InvoiceLineItem
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * InvoiceRepository - Repository Pattern untuk Invoice.
 * 
 * @author PBO Project Team
 * @since 1.0.0
 */
class InvoiceRepository {
    
    fun create(invoice: Invoice): Invoice = transaction {
        val id = InvoicesTable.insert {
            it[invoiceNumber] = invoice.invoiceNumber
            it[customerId] = invoice.customerId
            it[invoiceDate] = invoice.invoiceDate
            it[dueDate] = invoice.dueDate
            it[status] = invoice.status.value
            it[subtotal] = invoice.subtotal
            it[taxRate] = invoice.taxRate
            it[taxAmount] = invoice.taxAmount
            it[totalAmount] = invoice.totalAmount
            it[paidAmount] = invoice.paidAmount
            it[notes] = invoice.notes
            it[createdAt] = LocalDateTime.now()
        } get InvoicesTable.invoiceId
        
        invoice.copy(invoiceId = id)
    }
    
    fun findById(id: Int): Invoice? = transaction {
        InvoicesTable.select(InvoicesTable.invoiceId eq id)
            .map { toInvoice(it) }
            .singleOrNull()
    }
    
    fun findByNumber(number: String): Invoice? = transaction {
        InvoicesTable.select(InvoicesTable.invoiceNumber eq number)
            .map { toInvoice(it) }
            .singleOrNull()
    }
    
    fun findByCustomer(customerId: Int): List<Invoice> = transaction {
        InvoicesTable.select(InvoicesTable.customerId eq customerId)
            .orderBy(InvoicesTable.invoiceDate, SortOrder.DESC)
            .map { toInvoice(it) }
    }
    
    fun findAll(): List<Invoice> = transaction {
        InvoicesTable.selectAll()
            .orderBy(InvoicesTable.invoiceDate, SortOrder.DESC)
            .map { toInvoice(it) }
    }
    
    fun findByStatus(status: InvoiceStatus): List<Invoice> = transaction {
        InvoicesTable.select(InvoicesTable.status eq status.value)
            .orderBy(InvoicesTable.dueDate)
            .map { toInvoice(it) }
    }
    
    fun findOverdue(): List<Invoice> = transaction {
        InvoicesTable.select(
            (InvoicesTable.dueDate less LocalDate.now()) and
            (InvoicesTable.status inList listOf("SENT", "PARTIAL_PAID"))
        )
            .orderBy(InvoicesTable.dueDate)
            .map { toInvoice(it) }
    }
    
    fun findUnpaidByCustomer(customerId: Int): List<Invoice> = transaction {
        InvoicesTable.select(
            (InvoicesTable.customerId eq customerId) and
            (InvoicesTable.status inList listOf("SENT", "PARTIAL_PAID", "OVERDUE"))
        )
            .orderBy(InvoicesTable.dueDate)
            .map { toInvoice(it) }
    }
    
    fun update(invoice: Invoice): Boolean = transaction {
        InvoicesTable.update({ InvoicesTable.invoiceId eq invoice.invoiceId }) {
            it[status] = invoice.status.value
            it[subtotal] = invoice.subtotal
            it[taxAmount] = invoice.taxAmount
            it[totalAmount] = invoice.totalAmount
            it[paidAmount] = invoice.paidAmount
            it[notes] = invoice.notes
        } > 0
    }
    
    fun updateStatus(invoiceId: Int, status: InvoiceStatus): Boolean = transaction {
        InvoicesTable.update({ InvoicesTable.invoiceId eq invoiceId }) {
            it[InvoicesTable.status] = status.value
        } > 0
    }
    
    fun updatePaidAmount(invoiceId: Int, paidAmount: Double, status: InvoiceStatus): Boolean = transaction {
        InvoicesTable.update({ InvoicesTable.invoiceId eq invoiceId }) {
            it[InvoicesTable.paidAmount] = paidAmount
            it[InvoicesTable.status] = status.value
        } > 0
    }
    
    fun delete(id: Int): Boolean = transaction {
        InvoicesTable.deleteWhere { invoiceId eq id } > 0
    }
    
    fun getNextSequence(): Int = transaction {
        (InvoicesTable.selectAll().count().toInt()) + 1
    }
    
    fun getCustomerBalance(customerId: Int): Double = transaction {
        InvoicesTable.select(
            (InvoicesTable.customerId eq customerId) and
            (InvoicesTable.status inList listOf("SENT", "PARTIAL_PAID", "OVERDUE"))
        ).sumOf { it[InvoicesTable.totalAmount] - it[InvoicesTable.paidAmount] }
    }
    
    private fun toInvoice(row: ResultRow) = Invoice(
        invoiceId = row[InvoicesTable.invoiceId],
        invoiceNumber = row[InvoicesTable.invoiceNumber],
        customerId = row[InvoicesTable.customerId],
        invoiceDate = row[InvoicesTable.invoiceDate],
        dueDate = row[InvoicesTable.dueDate],
        status = InvoiceStatus.fromString(row[InvoicesTable.status]),
        subtotal = row[InvoicesTable.subtotal],
        taxRate = row[InvoicesTable.taxRate],
        taxAmount = row[InvoicesTable.taxAmount],
        totalAmount = row[InvoicesTable.totalAmount],
        paidAmount = row[InvoicesTable.paidAmount],
        notes = row[InvoicesTable.notes],
        createdAt = row[InvoicesTable.createdAt]
    )
}

/**
 * LineItemRepository - Repository untuk Invoice Line Items.
 */
class LineItemRepository {
    
    fun create(item: InvoiceLineItem): InvoiceLineItem = transaction {
        val id = InvoiceLineItemsTable.insert {
            it[invoiceId] = item.invoiceId
            it[description] = item.description
            it[quantity] = item.quantity
            it[unitPrice] = item.unitPrice
            it[amount] = item.amount
        } get InvoiceLineItemsTable.lineItemId
        
        item.copy(lineItemId = id)
    }
    
    fun findByInvoice(invoiceId: Int): List<InvoiceLineItem> = transaction {
        InvoiceLineItemsTable.select(InvoiceLineItemsTable.invoiceId eq invoiceId)
            .map { toLineItem(it) }
    }
    
    fun delete(lineItemId: Int): Boolean = transaction {
        InvoiceLineItemsTable.deleteWhere { InvoiceLineItemsTable.lineItemId eq lineItemId } > 0
    }
    
    fun deleteByInvoice(invoiceId: Int): Int = transaction {
        InvoiceLineItemsTable.deleteWhere { InvoiceLineItemsTable.invoiceId eq invoiceId }
    }
    
    private fun toLineItem(row: ResultRow) = InvoiceLineItem(
        lineItemId = row[InvoiceLineItemsTable.lineItemId],
        invoiceId = row[InvoiceLineItemsTable.invoiceId],
        description = row[InvoiceLineItemsTable.description],
        quantity = row[InvoiceLineItemsTable.quantity],
        unitPrice = row[InvoiceLineItemsTable.unitPrice],
        amount = row[InvoiceLineItemsTable.amount]
    )
}
