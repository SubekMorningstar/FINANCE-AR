package com.pbo.ar.api.routes

import com.pbo.ar.api.dto.*
import com.pbo.ar.di.AppModule
import com.pbo.ar.domain.model.PaymentMethod
import com.pbo.ar.domain.model.Result
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.LocalDate

/**
 * Payment API Routes
 */
fun Route.paymentRoutes() {
    val paymentService = AppModule.paymentService
    val customerService = AppModule.customerService
    val invoiceService = AppModule.invoiceService
    
    route("/payments") {
        // GET /api/payments
        get {
            val payments = paymentService.getAllPayments()
            val response = payments.map { pay ->
                val customer = customerService.getCustomerById(pay.customerId)
                pay.toResponse(customer?.name, paymentService.getAllocations(pay.paymentId), paymentService.getUnallocatedAmount(pay.paymentId))
            }
            call.respond(HttpStatusCode.OK, ApiResponse.success(response))
        }
        
        // POST /api/payments
        post {
            val request = call.receive<PaymentRequest>()
            
            val paymentDate = if (request.paymentDate.isBlank()) {
                LocalDate.now()
            } else {
                try {
                    LocalDate.parse(request.paymentDate)
                } catch (e: Exception) {
                    return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Format tanggal tidak valid (gunakan yyyy-MM-dd)"))
                }
            }
            
            val paymentMethod = try {
                PaymentMethod.fromString(request.paymentMethod)
            } catch (e: Exception) {
                return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Payment method tidak valid (CASH, BANK_TRANSFER, CREDIT_CARD, CHECK, OTHER)"))
            }
            
            val result = paymentService.createPayment(
                customerId = request.customerId,
                amount = request.amount,
                paymentMethod = paymentMethod,
                paymentDate = paymentDate,
                reference = request.reference,
                notes = request.notes
            )
            
            when (result) {
                is Result.Success -> {
                    val pay = result.data
                    call.respond(HttpStatusCode.Created, ApiResponse.success(
                        pay.toResponse(null, emptyList(), pay.amount),
                        result.message
                    ))
                }
                is Result.Error -> {
                    val status = if (result.code == 404) HttpStatusCode.NotFound else HttpStatusCode.BadRequest
                    call.respond(status, ApiResponse.error<Unit>(result.message))
                }
            }
        }
        
        // GET /api/payments/{id}
        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid ID"))
            
            val payment = paymentService.getPaymentById(id)
            if (payment != null) {
                val customer = customerService.getCustomerById(payment.customerId)
                val allocations = paymentService.getAllocations(id)
                val unallocated = paymentService.getUnallocatedAmount(id)
                call.respond(HttpStatusCode.OK, ApiResponse.success(
                    payment.toResponse(customer?.name, allocations, unallocated)
                ))
            } else {
                call.respond(HttpStatusCode.NotFound, ApiResponse.error<Unit>("Payment tidak ditemukan"))
            }
        }
        
        // POST /api/payments/{id}/allocate - Allocate to invoice
        post("/{id}/allocate") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid ID"))
            
            val request = call.receive<AllocationRequest>()
            
            val result = paymentService.allocatePayment(
                paymentId = id,
                invoiceId = request.invoiceId,
                amount = request.amount
            )
            
            when (result) {
                is Result.Success -> {
                    val alloc = result.data
                    val invoice = invoiceService.getInvoiceById(alloc.invoiceId)
                    call.respond(HttpStatusCode.Created, ApiResponse.success(
                        AllocationResponse(
                            allocationId = alloc.allocationId,
                            paymentId = alloc.paymentId,
                            invoiceId = alloc.invoiceId,
                            invoiceNumber = invoice?.invoiceNumber,
                            amount = alloc.amount,
                            allocatedAt = alloc.allocatedAt.toString()
                        ),
                        result.message
                    ))
                }
                is Result.Error -> {
                    val status = if (result.code == 404) HttpStatusCode.NotFound else HttpStatusCode.BadRequest
                    call.respond(status, ApiResponse.error<Unit>(result.message))
                }
            }
        }
        
        // POST /api/payments/{id}/auto-allocate
        post("/{id}/auto-allocate") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid ID"))
            
            val result = paymentService.autoAllocatePayment(id)
            
            when (result) {
                is Result.Success -> {
                    val allocations = result.data.map { alloc ->
                        val invoice = invoiceService.getInvoiceById(alloc.invoiceId)
                        AllocationResponse(
                            allocationId = alloc.allocationId,
                            paymentId = alloc.paymentId,
                            invoiceId = alloc.invoiceId,
                            invoiceNumber = invoice?.invoiceNumber,
                            amount = alloc.amount,
                            allocatedAt = alloc.allocatedAt.toString()
                        )
                    }
                    call.respond(HttpStatusCode.OK, ApiResponse.success(allocations, result.message))
                }
                is Result.Error -> {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>(result.message))
                }
            }
        }
    }
}

// Extension function
private fun com.pbo.ar.domain.model.Payment.toResponse(
    customerName: String?,
    allocations: List<com.pbo.ar.domain.model.PaymentAllocation>,
    unallocated: Double
) = PaymentResponse(
    paymentId = paymentId,
    paymentNumber = paymentNumber,
    customerId = customerId,
    customerName = customerName,
    paymentDate = paymentDate.toString(),
    amount = amount,
    allocatedAmount = amount - unallocated,
    unallocatedAmount = unallocated,
    paymentMethod = paymentMethod.value,
    paymentMethodDisplay = paymentMethod.toDisplayString(),
    reference = reference,
    notes = notes,
    allocations = allocations.map { alloc ->
        AllocationResponse(
            allocationId = alloc.allocationId,
            paymentId = alloc.paymentId,
            invoiceId = alloc.invoiceId,
            amount = alloc.amount,
            allocatedAt = alloc.allocatedAt.toString()
        )
    },
    createdAt = createdAt.toString()
)
