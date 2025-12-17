package com.pbo.ar.data.repository

import com.pbo.ar.data.database.CustomersTable
import com.pbo.ar.domain.model.Customer
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

/**
 * CustomerRepository - Repository Pattern untuk Customer.
 * 
 * Abstraksi: Menyembunyikan detail akses database
 * 
 * @author PBO Project Team
 * @since 1.0.0
 */
class CustomerRepository {
    
    fun create(customer: Customer): Customer = transaction {
        val id = CustomersTable.insert {
            it[code] = customer.code
            it[name] = customer.name
            it[email] = customer.email
            it[phone] = customer.phone
            it[address] = customer.address
            it[creditLimit] = customer.creditLimit
            it[createdAt] = LocalDateTime.now()
        } get CustomersTable.customerId
        
        customer.copy(customerId = id)
    }
    
    fun findById(id: Int): Customer? = transaction {
        CustomersTable.select(CustomersTable.customerId eq id)
            .map { toCustomer(it) }
            .singleOrNull()
    }
    
    fun findByCode(code: String): Customer? = transaction {
        CustomersTable.select(CustomersTable.code eq code)
            .map { toCustomer(it) }
            .singleOrNull()
    }
    
    fun findAll(): List<Customer> = transaction {
        CustomersTable.selectAll()
            .orderBy(CustomersTable.name)
            .map { toCustomer(it) }
    }
    
    fun update(customer: Customer): Boolean = transaction {
        CustomersTable.update({ CustomersTable.customerId eq customer.customerId }) {
            it[name] = customer.name
            it[email] = customer.email
            it[phone] = customer.phone
            it[address] = customer.address
            it[creditLimit] = customer.creditLimit
        } > 0
    }
    
    fun delete(id: Int): Boolean = transaction {
        CustomersTable.deleteWhere { customerId eq id } > 0
    }
    
    fun codeExists(code: String): Boolean = transaction {
        CustomersTable.select(CustomersTable.code eq code).count() > 0
    }
    
    fun getNextSequence(): Int = transaction {
        (CustomersTable.selectAll().count().toInt()) + 1
    }
    
    private fun toCustomer(row: ResultRow) = Customer(
        customerId = row[CustomersTable.customerId],
        code = row[CustomersTable.code],
        name = row[CustomersTable.name],
        email = row[CustomersTable.email],
        phone = row[CustomersTable.phone],
        address = row[CustomersTable.address],
        creditLimit = row[CustomersTable.creditLimit],
        createdAt = row[CustomersTable.createdAt]
    )
}
