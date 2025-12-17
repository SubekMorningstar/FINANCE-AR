package com.pbo.ar.api.routes

import com.pbo.ar.api.dto.*
import com.pbo.ar.di.AppModule
import com.pbo.ar.domain.model.Result
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.LocalDate

/**
 * Invoice API Routes
 */
fun Route.invoiceRoutes() {
    val invoiceService = AppModule.invoiceService
    val customerService = AppModule.customerService
    
    route("/invoices") {
        // GET /api/invoices
        get {
            val invoices = invoiceService.getAllInvoices()
            val response = invoices.map { inv ->
                val customer = customerService.getCustomerById(inv.customerId)
                inv.toResponse(customer?.name, invoiceService.getLineItems(inv.invoiceId))
            }
            call.respond(HttpStatusCode.OK, ApiResponse.success(response))
        }
        
        // POST /api/invoices
        post {
            val request = call.receive<InvoiceRequest>()
            
            val dueDate = try {
                LocalDate.parse(request.dueDate)
            } catch (e: Exception) {
                return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Format tanggal tidak valid (gunakan yyyy-MM-dd)"))
            }
            
            val result = invoiceService.createInvoice(
                customerId = request.customerId,
                dueDate = dueDate,
                taxRate = request.taxRate,
                notes = request.notes
            )
            
            when (result) {
                is Result.Success -> {
                    val inv = result.data
                    call.respond(HttpStatusCode.Created, ApiResponse.success(
                        inv.toResponse(null, emptyList()),
                        result.message
                    ))
                }
                is Result.Error -> {
                    val status = if (result.code == 404) HttpStatusCode.NotFound else HttpStatusCode.BadRequest
                    call.respond(status, ApiResponse.error<Unit>(result.message))
                }
            }
        }
        
        // GET /api/invoices/{id}
        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid ID"))
            
            val invoice = invoiceService.getInvoiceById(id)
            if (invoice != null) {
                val customer = customerService.getCustomerById(invoice.customerId)
                val items = invoiceService.getLineItems(id)
                call.respond(HttpStatusCode.OK, ApiResponse.success(
                    invoice.toResponse(customer?.name, items)
                ))
            } else {
                call.respond(HttpStatusCode.NotFound, ApiResponse.error<Unit>("Invoice tidak ditemukan"))
            }
        }
        
        // POST /api/invoices/{id}/items - Add line item
        post("/{id}/items") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid ID"))
            
            val request = call.receive<LineItemRequest>()
            
            val result = invoiceService.addLineItem(
                invoiceId = id,
                description = request.description,
                quantity = request.quantity,
                unitPrice = request.unitPrice
            )
            
            when (result) {
                is Result.Success -> {
                    val item = result.data
                    call.respond(HttpStatusCode.Created, ApiResponse.success(
                        LineItemResponse(
                            lineItemId = item.lineItemId,
                            description = item.description,
                            quantity = item.quantity,
                            unitPrice = item.unitPrice,
                            amount = item.amount
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
        
        // DELETE /api/invoices/{id}/items/{itemId}
        delete("/{id}/items/{itemId}") {
            val invoiceId = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid invoice ID"))
            val itemId = call.parameters["itemId"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid item ID"))
            
            val result = invoiceService.removeLineItem(invoiceId, itemId)
            
            when (result) {
                is Result.Success -> {
                    call.respond(HttpStatusCode.OK, ApiResponse.success(DeleteResponse(deleted = itemId), result.message))
                }
                is Result.Error -> {
                    val status = if (result.code == 404) HttpStatusCode.NotFound else HttpStatusCode.BadRequest
                    call.respond(status, ApiResponse.error<Unit>(result.message))
                }
            }
        }
        
        // POST /api/invoices/{id}/send
        post("/{id}/send") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid ID"))
            
            val result = invoiceService.sendInvoice(id)
            
            when (result) {
                is Result.Success -> {
                    val inv = result.data
                    call.respond(HttpStatusCode.OK, ApiResponse.success(
                        inv.toResponse(null, invoiceService.getLineItems(id)),
                        result.message
                    ))
                }
                is Result.Error -> {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>(result.message))
                }
            }
        }
        
        // POST /api/invoices/{id}/cancel
        post("/{id}/cancel") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid ID"))
            
            val result = invoiceService.cancelInvoice(id)
            
            when (result) {
                is Result.Success -> {
                    val inv = result.data
                    call.respond(HttpStatusCode.OK, ApiResponse.success(
                        inv.toResponse(null, emptyList()),
                        result.message
                    ))
                }
                is Result.Error -> {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>(result.message))
                }
            }
        }
    }
}

// Extension function untuk konversi ke Response
private fun com.pbo.ar.domain.model.Invoice.toResponse(
    customerName: String?,
    items: List<com.pbo.ar.domain.model.InvoiceLineItem>
) = InvoiceResponse(
    invoiceId = invoiceId,
    invoiceNumber = invoiceNumber,
    customerId = customerId,
    customerName = customerName,
    invoiceDate = invoiceDate.toString(),
    dueDate = dueDate.toString(),
    status = status.value,
    statusDisplay = status.toDisplayString(),
    subtotal = subtotal,
    taxRate = taxRate,
    taxAmount = taxAmount,
    totalAmount = totalAmount,
    paidAmount = paidAmount,
    balanceDue = getBalanceDue(),
    notes = notes,
    items = items.map { item ->
        LineItemResponse(
            lineItemId = item.lineItemId,
            description = item.description,
            quantity = item.quantity,
            unitPrice = item.unitPrice,
            amount = item.amount
        )
    },
    createdAt = createdAt.toString()
)
