package com.pbo.ar.data.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.javatime.date

/**
 * Database Tables untuk sistem Accounts Receivable.
 * 
 * @author PBO Project Team
 * @since 1.0.0
 */

/**
 * Tabel Customers - Data pelanggan.
 */
object CustomersTable : Table("customers") {
    val customerId = integer("customer_id").autoIncrement()
    val code = varchar("code", 20).uniqueIndex()
    val name = varchar("name", 100)
    val email = varchar("email", 100).default("")
    val phone = varchar("phone", 20).default("")
    val address = text("address").default("")
    val creditLimit = double("credit_limit").default(0.0)
    val createdAt = datetime("created_at")
    
    override val primaryKey = PrimaryKey(customerId)
}

/**
 * Tabel Invoices - Faktur penjualan.
 */
object InvoicesTable : Table("invoices") {
    val invoiceId = integer("invoice_id").autoIncrement()
    val invoiceNumber = varchar("invoice_number", 30).uniqueIndex()
    val customerId = integer("customer_id").references(CustomersTable.customerId)
    val invoiceDate = date("invoice_date")
    val dueDate = date("due_date")
    val status = varchar("status", 20).default("DRAFT")
    val subtotal = double("subtotal").default(0.0)
    val taxRate = double("tax_rate").default(11.0)
    val taxAmount = double("tax_amount").default(0.0)
    val totalAmount = double("total_amount").default(0.0)
    val paidAmount = double("paid_amount").default(0.0)
    val notes = text("notes").default("")
    val createdAt = datetime("created_at")
    
    override val primaryKey = PrimaryKey(invoiceId)
}

/**
 * Tabel InvoiceLineItems - Detail item faktur.
 */
object InvoiceLineItemsTable : Table("invoice_line_items") {
    val lineItemId = integer("line_item_id").autoIncrement()
    val invoiceId = integer("invoice_id").references(InvoicesTable.invoiceId)
    val description = varchar("description", 255)
    val quantity = integer("quantity").default(1)
    val unitPrice = double("unit_price")
    val amount = double("amount")
    
    override val primaryKey = PrimaryKey(lineItemId)
}

/**
 * Tabel Payments - Pembayaran dari pelanggan.
 */
object PaymentsTable : Table("payments") {
    val paymentId = integer("payment_id").autoIncrement()
    val paymentNumber = varchar("payment_number", 30).uniqueIndex()
    val customerId = integer("customer_id").references(CustomersTable.customerId)
    val paymentDate = date("payment_date")
    val amount = double("amount")
    val paymentMethod = varchar("payment_method", 20)
    val reference = varchar("reference", 100).default("")
    val notes = text("notes").default("")
    val createdAt = datetime("created_at")
    
    override val primaryKey = PrimaryKey(paymentId)
}

/**
 * Tabel PaymentAllocations - Alokasi pembayaran ke faktur.
 */
object PaymentAllocationsTable : Table("payment_allocations") {
    val allocationId = integer("allocation_id").autoIncrement()
    val paymentId = integer("payment_id").references(PaymentsTable.paymentId)
    val invoiceId = integer("invoice_id").references(InvoicesTable.invoiceId)
    val amount = double("amount")
    val allocatedAt = datetime("allocated_at")
    
    override val primaryKey = PrimaryKey(allocationId)
}
