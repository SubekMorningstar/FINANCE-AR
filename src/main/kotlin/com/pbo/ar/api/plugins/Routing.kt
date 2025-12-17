package com.pbo.ar.api.plugins

import com.pbo.ar.api.routes.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val name: String,
    val version: String,
    val status: String,
    val endpoints: List<String>
)

@Serializable
data class StatusResponse(val status: String)

fun Application.configureRouting() {
    routing {
        // Health check
        get("/") {
            call.respond(HealthResponse(
                name = "Accounts Receivable API",
                version = "1.0.0",
                status = "running",
                endpoints = listOf(
                    "/api/customers",
                    "/api/invoices",
                    "/api/payments",
                    "/api/reports"
                )
            ))
        }
        
        get("/health") {
            call.respond(StatusResponse(status = "healthy"))
        }
        
        // API routes
        route("/api") {
            customerRoutes()
            invoiceRoutes()
            paymentRoutes()
            reportRoutes()
        }
    }
}
