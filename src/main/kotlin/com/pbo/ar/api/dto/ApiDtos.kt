package com.pbo.ar.api.dto

import kotlinx.serialization.Serializable

/**
 * API DTOs for Accounts Receivable System.
 * 
 * @author PBO Project Team
 * @since 1.0.0
 */

// ==================== Response Wrapper ====================

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null
) {
    companion object {
        fun <T> success(data: T, message: String = "Success"): ApiResponse<T> =
            ApiResponse(success = true, message = message, data = data)
        
        fun <T> error(message: String): ApiResponse<T> =
            ApiResponse(success = false, message = message, data = null)
    }
}

// ==================== Customer DTOs ====================

@Serializable
data class CustomerRequest(
    val name: String,
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val creditLimit: Double = 0.0
)

@Serializable
data class CustomerResponse(
    val customerId: Int,
    val code: String,
    val name: String,
    val email: String,
    val phone: String,
    val address: String,
    val creditLimit: Double,
    val balance: Double = 0.0,
    val createdAt: String
)

// ==================== Invoice DTOs ====================

@Serializable
data class InvoiceRequest(
    val customerId: Int,
    val dueDate: String, // yyyy-MM-dd
    val taxRate: Double = 11.0,
    val notes: String = ""
)

@Serializable
data class LineItemRequest(
    val description: String,
    val quantity: Int = 1,
    val unitPrice: Double
)

@Serializable
data class LineItemResponse(
    val lineItemId: Int,
    val description: String,
    val quantity: Int,
    val unitPrice: Double,
    val amount: Double
)

@Serializable
data class InvoiceResponse(
    val invoiceId: Int,
    val invoiceNumber: String,
    val customerId: Int,
    val customerName: String? = null,
    val invoiceDate: String,
    val dueDate: String,
    val status: String,
    val statusDisplay: String,
    val subtotal: Double,
    val taxRate: Double,
    val taxAmount: Double,
    val totalAmount: Double,
    val paidAmount: Double,
    val balanceDue: Double,
    val notes: String,
    val items: List<LineItemResponse> = emptyList(),
    val createdAt: String
)

// ==================== Payment DTOs ====================

@Serializable
data class PaymentRequest(
    val customerId: Int,
    val amount: Double,
    val paymentMethod: String, // CASH, BANK_TRANSFER, etc.
    val paymentDate: String = "", // yyyy-MM-dd, default today
    val reference: String = "",
    val notes: String = ""
)

@Serializable
data class AllocationRequest(
    val invoiceId: Int,
    val amount: Double
)

@Serializable
data class AllocationResponse(
    val allocationId: Int,
    val paymentId: Int,
    val invoiceId: Int,
    val invoiceNumber: String? = null,
    val amount: Double,
    val allocatedAt: String
)

@Serializable
data class PaymentResponse(
    val paymentId: Int,
    val paymentNumber: String,
    val customerId: Int,
    val customerName: String? = null,
    val paymentDate: String,
    val amount: Double,
    val allocatedAmount: Double = 0.0,
    val unallocatedAmount: Double = 0.0,
    val paymentMethod: String,
    val paymentMethodDisplay: String,
    val reference: String,
    val notes: String,
    val allocations: List<AllocationResponse> = emptyList(),
    val createdAt: String
)

// ==================== Report DTOs ====================

@Serializable
data class AgingBucketResponse(
    val label: String,
    val invoiceCount: Int,
    val totalAmount: Double
)

@Serializable
data class AgingReportResponse(
    val asOfDate: String,
    val buckets: List<AgingBucketResponse>,
    val totalReceivable: Double
)

@Serializable
data class StatementResponse(
    val customer: CustomerResponse,
    val invoices: List<InvoiceResponse>,
    val payments: List<PaymentResponse>,
    val totalInvoiced: Double,
    val totalPaid: Double,
    val balance: Double,
    val startDate: String,
    val endDate: String
)

@Serializable
data class SummaryResponse(
    val totalCustomers: Int,
    val totalInvoices: Int,
    val totalReceivable: Double,
    val totalOverdue: Double,
    val totalCollected: Double,
    val collectionRate: Double
)

// ==================== Common DTOs ====================

@Serializable
data class DeleteResponse(
    val deleted: Int
)
