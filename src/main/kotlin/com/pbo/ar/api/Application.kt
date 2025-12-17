package com.pbo.ar.api

import com.pbo.ar.api.plugins.*
import com.pbo.ar.data.database.DatabaseManager
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

/**
 * Main entry point untuk REST API Server.
 * 
 * @author PBO Project Team
 * @since 1.0.0
 */
fun main() {
    println("=".repeat(50))
    println("   Accounts Receivable System - API Server")
    println("=".repeat(50))
    
    // Initialize database
    DatabaseManager.connect()
    DatabaseManager.initializeTables()
    
    println("Starting API server on http://localhost:8080")
    println("=".repeat(50))
    
    // Start Ktor server
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configurePlugins()
    }.start(wait = true)
}

fun Application.configurePlugins() {
    configureSerialization()
    configureStatusPages()
    configureCORS()
    configureCallLogging()
    configureRouting()
}
