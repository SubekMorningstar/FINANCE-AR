package com.pbo.ar.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pbo.ar.di.AppModule
import com.pbo.ar.domain.model.*
import com.pbo.ar.ui.components.*
import com.pbo.ar.ui.util.FormatUtils
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen() {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isCreating by remember { mutableStateOf(false) }
    
    var payments by remember { mutableStateOf<List<Payment>>(emptyList()) }
    var customers by remember { mutableStateOf<List<Customer>>(emptyList()) }
    
    // Form Data
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var expandedCustomerDropdown by remember { mutableStateOf(false) }
    var amountStr by remember { mutableStateOf("") }
    var referenceNumber by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    
    // NEW: Payment Method
    var selectedPaymentMethod: PaymentMethod by remember { mutableStateOf(PaymentMethod.BANK_TRANSFER as PaymentMethod) }
    var expandedMethodDropdown by remember { mutableStateOf(false) }
    
    // NEW: Outstanding Invoices for selected customer
    var outstandingInvoices by remember { mutableStateOf<List<Invoice>>(emptyList()) }
    var autoAllocate by remember { mutableStateOf(true) }
    
    val paymentMethods = listOf(
        PaymentMethod.CASH,
        PaymentMethod.BANK_TRANSFER,
        PaymentMethod.CREDIT_CARD,
        PaymentMethod.CHECK,
        PaymentMethod.OTHER
    )

    fun loadData() {
        scope.launch {
            try {
                payments = AppModule.paymentService.getAllPayments()
                customers = AppModule.customerService.getAllCustomers()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    // Load outstanding invoices when customer changes
    fun loadOutstandingInvoices(customerId: Int) {
        scope.launch {
            try {
                outstandingInvoices = AppModule.invoiceService.getUnpaidInvoicesByCustomer(customerId)
            } catch (e: Exception) {
                e.printStackTrace()
                outstandingInvoices = emptyList()
            }
        }
    }
    
    fun resetForm() {
        selectedCustomer = null
        amountStr = ""
        referenceNumber = ""
        notes = ""
        selectedPaymentMethod = PaymentMethod.BANK_TRANSFER
        outstandingInvoices = emptyList()
        autoAllocate = true
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (isCreating) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { 
                            isCreating = false
                            resetForm()
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                        Text("Catat Pembayaran Baru", style = MaterialTheme.typography.headlineSmall)
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        // Left Column - Form
                        Card(modifier = Modifier.width(450.dp)) {
                            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("Detail Pembayaran", style = MaterialTheme.typography.titleMedium)
                                
                                // Customer
                                ExposedDropdownMenuBox(
                                    expanded = expandedCustomerDropdown,
                                    onExpandedChange = { expandedCustomerDropdown = !expandedCustomerDropdown }
                                ) {
                                    OutlinedTextField(
                                        value = selectedCustomer?.name ?: "",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Pilih Pelanggan *") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCustomerDropdown) },
                                        modifier = Modifier.menuAnchor().fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = expandedCustomerDropdown,
                                        onDismissRequest = { expandedCustomerDropdown = false }
                                    ) {
                                        customers.forEach { customer ->
                                            DropdownMenuItem(
                                                text = { Text("${customer.name} (${customer.code})") },
                                                onClick = {
                                                    selectedCustomer = customer
                                                    expandedCustomerDropdown = false
                                                    loadOutstandingInvoices(customer.customerId)
                                                }
                                            )
                                        }
                                    }
                                }
                                
                                // Payment Method - NEW
                                ExposedDropdownMenuBox(
                                    expanded = expandedMethodDropdown,
                                    onExpandedChange = { expandedMethodDropdown = !expandedMethodDropdown }
                                ) {
                                    OutlinedTextField(
                                        value = selectedPaymentMethod.toDisplayString(),
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Metode Pembayaran") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMethodDropdown) },
                                        modifier = Modifier.menuAnchor().fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = expandedMethodDropdown,
                                        onDismissRequest = { expandedMethodDropdown = false }
                                    ) {
                                        paymentMethods.forEach { method ->
                                            DropdownMenuItem(
                                                text = { Text(method.toDisplayString()) },
                                                onClick = {
                                                    selectedPaymentMethod = method
                                                    expandedMethodDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                                
                                OutlinedTextField(
                                    value = amountStr,
                                    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) amountStr = it },
                                    label = { Text("Jumlah Pembayaran (Rp) *") },
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = { Text("Rp") }
                                )
                                
                                OutlinedTextField(
                                    value = referenceNumber,
                                    onValueChange = { referenceNumber = it },
                                    label = { Text("No. Referensi / Bukti Transfer") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                OutlinedTextField(
                                    value = notes,
                                    onValueChange = { notes = it },
                                    label = { Text("Catatan") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 2
                                )
                                
                                // Auto-allocate option
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Checkbox(
                                        checked = autoAllocate,
                                        onCheckedChange = { autoAllocate = it }
                                    )
                                    Column {
                                        Text("Alokasikan otomatis ke invoice")
                                        Text(
                                            "Pembayaran akan dialokasikan ke invoice tertua terlebih dahulu",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { 
                                            isCreating = false
                                            resetForm()
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Batal")
                                    }
                                    
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                try {
                                                    if (selectedCustomer != null) {
                                                        val amount = amountStr.toDoubleOrNull() ?: 0.0
                                                        if (amount > 0) {
                                                            val result = AppModule.paymentService.createPayment(
                                                                selectedCustomer!!.customerId,
                                                                amount,
                                                                selectedPaymentMethod,
                                                                LocalDate.now(),
                                                                referenceNumber,
                                                                notes
                                                            )
                                                            
                                                            when (result) {
                                                                is Result.Success -> {
                                                                    // Auto-allocate if checked
                                                                    if (autoAllocate) {
                                                                        val allocResult = AppModule.paymentService.autoAllocatePayment(result.data.paymentId)
                                                                        when (allocResult) {
                                                                            is Result.Success -> {
                                                                                snackbarHostState.showSnackbar("Pembayaran berhasil disimpan dan dialokasikan ke ${allocResult.data.size} invoice")
                                                                            }
                                                                            is Result.Error -> {
                                                                                snackbarHostState.showSnackbar("Pembayaran disimpan, tapi gagal alokasi: ${allocResult.message}")
                                                                            }
                                                                        }
                                                                    } else {
                                                                        snackbarHostState.showSnackbar("Pembayaran berhasil disimpan")
                                                                    }
                                                                    isCreating = false
                                                                    resetForm()
                                                                    loadData()
                                                                }
                                                                is Result.Error -> {
                                                                    snackbarHostState.showSnackbar("Gagal: ${result.message}")
                                                                }
                                                            }
                                                        } else {
                                                            snackbarHostState.showSnackbar("Jumlah harus lebih dari 0")
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    snackbarHostState.showSnackbar("Error: ${e.message}")
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        enabled = selectedCustomer != null && amountStr.isNotEmpty()
                                    ) {
                                        Icon(Icons.Default.Save, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Simpan Pembayaran")
                                    }
                                }
                            }
                        }
                        
                        // Right Column - Outstanding Invoices
                        Card(modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Text("Invoice Belum Lunas", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                if (selectedCustomer == null) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(200.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "Pilih pelanggan untuk melihat invoice yang belum lunas",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else if (outstandingInvoices.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(200.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                modifier = Modifier.size(48.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Tidak ada invoice yang belum lunas", color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                } else {
                                    // Summary
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("Total Hutang", style = MaterialTheme.typography.labelMedium)
                                                val totalDue = outstandingInvoices.sumOf { it.getBalanceDue() }
                                                Text(
                                                    FormatUtils.formatCurrency(totalDue),
                                                    style = MaterialTheme.typography.headlineSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text("${outstandingInvoices.size} invoice")
                                        }
                                    }
                                    
                                    // Invoice List
                                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                                        items(outstandingInvoices) { invoice ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(invoice.invoiceNumber, fontWeight = FontWeight.Bold)
                                                        Text(
                                                            "Jatuh tempo: ${FormatUtils.formatDate(invoice.dueDate)}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = if (invoice.isOverdue()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    Column(horizontalAlignment = Alignment.End) {
                                                        Text(
                                                            FormatUtils.formatCurrency(invoice.getBalanceDue()),
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.error
                                                        )
                                                        if (invoice.isOverdue()) {
                                                            Text(
                                                                "JATUH TEMPO",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.error
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Riwayat Pembayaran", style = MaterialTheme.typography.headlineMedium)
                        Button(onClick = { 
                            resetForm()
                            isCreating = true 
                        }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Catat Pembayaran")
                        }
                    }
                    
                    DataTable(
                        columns = listOf(
                            DataColumn("No. Pembayaran", 1f),
                            DataColumn("Tanggal", 0.8f),
                            DataColumn("Pelanggan", 1.2f),
                            DataColumn("Metode", 0.8f),
                            DataColumn("Referensi", 1f),
                            DataColumn("Jumlah", 1f)
                        ),
                        data = payments
                    ) { payment ->
                        DataRow {
                            DataCell(payment.paymentNumber, 1f, fontWeight = FontWeight.Bold)
                            DataCell(FormatUtils.formatDate(payment.paymentDate), 0.8f)
                            val customerName = customers.find { it.customerId == payment.customerId }?.name ?: "Unknown"
                            DataCell(customerName, 1.2f)
                            DataCell(payment.paymentMethod.toDisplayString(), 0.8f)
                            DataCell(payment.reference, 1f)
                            DataCell(
                                FormatUtils.formatCurrency(payment.amount), 
                                1f, 
                                fontWeight = FontWeight.Bold, 
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
