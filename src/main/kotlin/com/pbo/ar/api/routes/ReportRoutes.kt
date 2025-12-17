package com.pbo.ar.api.routes

import com.pbo.ar.api.dto.*
import com.pbo.ar.di.AppModule
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.LocalDate

/**
 * Report API Routes
 */
fun Route.reportRoutes() {
    val reportService = AppModule.reportService
    val customerService = AppModule.customerService
    val paymentService = AppModule.paymentService
    val invoiceService = AppModule.invoiceService
    
    route("/reports") {
        // GET /api/reports/aging
        get("/aging") {
            val report = reportService.getAgingReport()
            call.respond(HttpStatusCode.OK, ApiResponse.success(
                AgingReportResponse(
                    asOfDate = report.asOfDate.toString(),
                    buckets = report.buckets.map { bucket ->
                        AgingBucketResponse(
                            label = bucket.label,
                            invoiceCount = bucket.invoiceCount,
                            totalAmount = bucket.totalAmount
                        )
                    },
                    totalReceivable = report.totalReceivable
                )
            ))
        }
        
        // GET /api/reports/summary
        get("/summary") {
            val summary = reportService.getReceivableSummary()
            call.respond(HttpStatusCode.OK, ApiResponse.success(
                SummaryResponse(
                    totalCustomers = summary.totalCustomers,
                    totalInvoices = summary.totalInvoices,
                    totalReceivable = summary.totalReceivable,
                    totalOverdue = summary.totalOverdue,
                    totalCollected = summary.totalCollected,
                    collectionRate = summary.collectionRate
                )
            ))
        }
        
        // GET /api/reports/statement/{customerId}
        get("/statement/{customerId}") {
            val customerId = call.parameters["customerId"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid customer ID"))
            
            val startDate = call.request.queryParameters["startDate"]?.let { LocalDate.parse(it) }
                ?: LocalDate.now().minusMonths(3)
            val endDate = call.request.queryParameters["endDate"]?.let { LocalDate.parse(it) }
                ?: LocalDate.now()
            
            val statement = reportService.getCustomerStatement(customerId, startDate, endDate)
            
            if (statement != null) {
                call.respond(HttpStatusCode.OK, ApiResponse.success(
                    StatementResponse(
                        customer = CustomerResponse(
                            customerId = statement.customer.customerId,
                            code = statement.customer.code,
                            name = statement.customer.name,
                            email = statement.customer.email,
                            phone = statement.customer.phone,
                            address = statement.customer.address,
                            creditLimit = statement.customer.creditLimit,
                            balance = statement.balance,
                            createdAt = statement.customer.createdAt.toString()
                        ),
                        invoices = statement.invoices.map { inv ->
                            InvoiceResponse(
                                invoiceId = inv.invoiceId,
                                invoiceNumber = inv.invoiceNumber,
                                customerId = inv.customerId,
                                invoiceDate = inv.invoiceDate.toString(),
                                dueDate = inv.dueDate.toString(),
                                status = inv.status.value,
                                statusDisplay = inv.status.toDisplayString(),
                                subtotal = inv.subtotal,
                                taxRate = inv.taxRate,
                                taxAmount = inv.taxAmount,
                                totalAmount = inv.totalAmount,
                                paidAmount = inv.paidAmount,
                                balanceDue = inv.getBalanceDue(),
                                notes = inv.notes,
                                createdAt = inv.createdAt.toString()
                            )
                        },
                        payments = statement.payments.map { pay ->
                            PaymentResponse(
                                paymentId = pay.paymentId,
                                paymentNumber = pay.paymentNumber,
                                customerId = pay.customerId,
                                paymentDate = pay.paymentDate.toString(),
                                amount = pay.amount,
                                paymentMethod = pay.paymentMethod.value,
                                paymentMethodDisplay = pay.paymentMethod.toDisplayString(),
                                reference = pay.reference,
                                notes = pay.notes,
                                createdAt = pay.createdAt.toString()
                            )
                        },
                        totalInvoiced = statement.totalInvoiced,
                        totalPaid = statement.totalPaid,
                        balance = statement.balance,
                        startDate = statement.startDate.toString(),
                        endDate = statement.endDate.toString()
                    )
                ))
            } else {
                call.respond(HttpStatusCode.NotFound, ApiResponse.error<Unit>("Customer tidak ditemukan"))
            }
        }
    }
}
