package com.pbo.ar.api.routes

import com.pbo.ar.api.dto.*
import com.pbo.ar.di.AppModule
import com.pbo.ar.domain.model.Result
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Customer API Routes
 */
fun Route.customerRoutes() {
    val customerService = AppModule.customerService
    
    route("/customers") {
        // GET /api/customers
        get {
            val customers = customerService.getAllCustomers()
            val response = customers.map { cust ->
                CustomerResponse(
                    customerId = cust.customerId,
                    code = cust.code,
                    name = cust.name,
                    email = cust.email,
                    phone = cust.phone,
                    address = cust.address,
                    creditLimit = cust.creditLimit,
                    balance = customerService.getCustomerBalance(cust.customerId),
                    createdAt = cust.createdAt.toString()
                )
            }
            call.respond(HttpStatusCode.OK, ApiResponse.success(response))
        }
        
        // POST /api/customers
        post {
            val request = call.receive<CustomerRequest>()
            
            val result = customerService.createCustomer(
                name = request.name,
                email = request.email,
                phone = request.phone,
                address = request.address,
                creditLimit = request.creditLimit
            )
            
            when (result) {
                is Result.Success -> {
                    val cust = result.data
                    call.respond(HttpStatusCode.Created, ApiResponse.success(
                        CustomerResponse(
                            customerId = cust.customerId,
                            code = cust.code,
                            name = cust.name,
                            email = cust.email,
                            phone = cust.phone,
                            address = cust.address,
                            creditLimit = cust.creditLimit,
                            balance = 0.0,
                            createdAt = cust.createdAt.toString()
                        ),
                        result.message
                    ))
                }
                is Result.Error -> {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>(result.message))
                }
            }
        }
        
        // GET /api/customers/{id}
        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid ID"))
            
            val customer = customerService.getCustomerById(id)
            if (customer != null) {
                call.respond(HttpStatusCode.OK, ApiResponse.success(
                    CustomerResponse(
                        customerId = customer.customerId,
                        code = customer.code,
                        name = customer.name,
                        email = customer.email,
                        phone = customer.phone,
                        address = customer.address,
                        creditLimit = customer.creditLimit,
                        balance = customerService.getCustomerBalance(customer.customerId),
                        createdAt = customer.createdAt.toString()
                    )
                ))
            } else {
                call.respond(HttpStatusCode.NotFound, ApiResponse.error<Unit>("Customer tidak ditemukan"))
            }
        }
        
        // PUT /api/customers/{id}
        put("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid ID"))
            
            val request = call.receive<CustomerRequest>()
            
            val result = customerService.updateCustomer(
                customerId = id,
                name = request.name,
                email = request.email,
                phone = request.phone,
                address = request.address,
                creditLimit = request.creditLimit
            )
            
            when (result) {
                is Result.Success -> {
                    val cust = result.data
                    call.respond(HttpStatusCode.OK, ApiResponse.success(
                        CustomerResponse(
                            customerId = cust.customerId,
                            code = cust.code,
                            name = cust.name,
                            email = cust.email,
                            phone = cust.phone,
                            address = cust.address,
                            creditLimit = cust.creditLimit,
                            balance = customerService.getCustomerBalance(cust.customerId),
                            createdAt = cust.createdAt.toString()
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
        
        // DELETE /api/customers/{id}
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid ID"))
            
            val result = customerService.deleteCustomer(id)
            
            when (result) {
                is Result.Success -> {
                    call.respond(HttpStatusCode.OK, ApiResponse.success(DeleteResponse(deleted = id), result.message))
                }
                is Result.Error -> {
                    val status = if (result.code == 404) HttpStatusCode.NotFound else HttpStatusCode.BadRequest
                    call.respond(status, ApiResponse.error<Unit>(result.message))
                }
            }
        }
    }
}
