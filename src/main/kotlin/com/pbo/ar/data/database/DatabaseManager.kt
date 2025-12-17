package com.pbo.ar.data.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.util.Properties

/**
 * DatabaseManager - Mengelola koneksi MySQL dengan HikariCP.
 * 
 * Singleton Pattern
 * 
 * @author PBO Project Team
 * @since 1.0.0
 */
object DatabaseManager {
    
    private var dataSource: HikariDataSource? = null
    private var isConnected = false
    
    private var host = "localhost"
    private var port = 3306
    private var database = "ar_system"
    private var username = "root"
    private var password = ""
    
    private fun loadConfig() {
        val configFile = File("config/database.properties")
        if (configFile.exists()) {
            val props = Properties()
            props.load(configFile.inputStream())
            host = props.getProperty("db.host", host)
            port = props.getProperty("db.port", port.toString()).toInt()
            database = props.getProperty("db.database", database)
            username = props.getProperty("db.username", username)
            password = props.getProperty("db.password", password)
        }
    }
    
    fun connect() {
        if (isConnected) return
        
        loadConfig()
        
        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:mysql://$host:$port/$database?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
            driverClassName = "com.mysql.cj.jdbc.Driver"
            this.username = this@DatabaseManager.username
            this.password = this@DatabaseManager.password
            maximumPoolSize = 10
            minimumIdle = 2
            idleTimeout = 30000
            connectionTimeout = 30000
            
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
        }
        
        try {
            dataSource = HikariDataSource(config)
            Database.connect(dataSource!!)
            isConnected = true
            println("✅ MySQL connected: $host:$port/$database")
        } catch (e: Exception) {
            println("❌ MySQL connection failed: ${e.message}")
            throw e
        }
    }
    
    fun initializeTables() {
        transaction {
            SchemaUtils.create(
                CustomersTable,
                InvoicesTable,
                InvoiceLineItemsTable,
                PaymentsTable,
                PaymentAllocationsTable
            )
            println("✅ Database tables initialized")
        }
    }
    
    fun close() {
        dataSource?.close()
        isConnected = false
    }
    
    fun isConnected(): Boolean = isConnected
}
