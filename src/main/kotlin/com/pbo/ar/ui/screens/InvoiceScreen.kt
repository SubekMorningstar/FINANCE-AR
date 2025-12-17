package com.pbo.ar.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pbo.ar.di.AppModule
import com.pbo.ar.domain.model.Customer
import com.pbo.ar.domain.model.Invoice
import com.pbo.ar.domain.model.InvoiceStatus
import com.pbo.ar.domain.model.Result
import com.pbo.ar.ui.components.*
import com.pbo.ar.ui.util.FormatUtils
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceScreen() {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isCreating by remember { mutableStateOf(false) }
    var selectedInvoiceForAction by remember { mutableStateOf<Invoice?>(null) }
    
    // List Data
    var invoices by remember { mutableStateOf<List<Invoice>>(emptyList()) }
    var customers by remember { mutableStateOf<List<Customer>>(emptyList()) }
    
    // Create Form Data
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var expandedCustomerDropdown by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var lineItems by remember { mutableStateOf(listOf<LineItemEntry>()) }
    
    // NEW: Due Date and Tax Rate
    var dueDateDays by remember { mutableStateOf("30") }
    var selectedTaxRate by remember { mutableStateOf(11.0) }
    var expandedTaxDropdown by remember { mutableStateOf(false) }
    
    val taxRateOptions = listOf(0.0, 11.0, 12.0)

    fun loadData() {
        scope.launch {
            try {
                invoices = AppModule.invoiceService.getAllInvoices()
                customers = AppModule.customerService.getAllCustomers()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun resetForm() {
        selectedCustomer = null
        notes = ""
        lineItems = emptyList()
        dueDateDays = "30"
        selectedTaxRate = 11.0
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    // Helper to translate status
    fun translateStatus(status: InvoiceStatus): String {
        return when(status) {
            InvoiceStatus.DRAFT -> "Konsep"
            InvoiceStatus.SENT -> "Terkirim"
            InvoiceStatus.PARTIAL_PAID -> "Dibayar Sebagian"
            InvoiceStatus.PAID -> "Lunas"
            InvoiceStatus.OVERDUE -> "Jatuh Tempo"
            InvoiceStatus.CANCELLED -> "Batal"
        }
    }
    
    // Calculate preview totals
    fun calculateSubtotal(): Double {
        return lineItems.sumOf { item ->
            val qty = item.qty.toIntOrNull() ?: 0
            val price = item.price.toDoubleOrNull() ?: 0.0
            qty * price
        }
    }
    
    fun calculateTax(): Double = calculateSubtotal() * (selectedTaxRate / 100)
    fun calculateTotal(): Double = calculateSubtotal() + calculateTax()

    // Action Dialog for Invoice
    if (selectedInvoiceForAction != null) {
        val invoice = selectedInvoiceForAction!!
        AlertDialog(
            onDismissRequest = { selectedInvoiceForAction = null },
            title = { Text("Aksi Invoice") },
            text = {
                Column {
                    Text("Invoice: ${invoice.invoiceNumber}")
                    Text("Status: ${translateStatus(invoice.status)}")
                    Text("Total: ${FormatUtils.formatCurrency(invoice.totalAmount)}")
                }
            },
            confirmButton = {
                if (invoice.status == InvoiceStatus.DRAFT) {
                    Button(onClick = {
                        scope.launch {
                            val result = AppModule.invoiceService.sendInvoice(invoice.invoiceId)
                            when (result) {
                                is Result.Success -> {
                                    snackbarHostState.showSnackbar("Invoice berhasil dikirim!")
                                    loadData()
                                }
                                is Result.Error -> {
                                    snackbarHostState.showSnackbar("Gagal: ${result.message}")
                                }
                            }
                            selectedInvoiceForAction = null
                        }
                    }) {
                        Icon(Icons.Default.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kirim Invoice")
                    }
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (invoice.status in listOf(InvoiceStatus.DRAFT, InvoiceStatus.SENT) && invoice.paidAmount == 0.0) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val result = AppModule.invoiceService.cancelInvoice(invoice.invoiceId)
                                    when (result) {
                                        is Result.Success -> {
                                            snackbarHostState.showSnackbar("Invoice dibatalkan")
                                            loadData()
                                        }
                                        is Result.Error -> {
                                            snackbarHostState.showSnackbar("Gagal: ${result.message}")
                                        }
                                    }
                                    selectedInvoiceForAction = null
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Batalkan")
                        }
                    }
                    TextButton(onClick = { selectedInvoiceForAction = null }) {
                        Text("Tutup")
                    }
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (isCreating) {
                // CREATE INVOICE FORM
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    // Header
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { 
                            isCreating = false
                            resetForm()
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                        Text("Buat Faktur Baru", style = MaterialTheme.typography.headlineSmall)
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Left Column - Form Fields
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Customer Selection
                            Text("Informasi Faktur", style = MaterialTheme.typography.titleMedium)
                            
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
                                            }
                                        )
                                    }
                                }
                            }
                            
                            // Due Date (days from now)
                            OutlinedTextField(
                                value = dueDateDays,
                                onValueChange = { if (it.all { c -> c.isDigit() }) dueDateDays = it },
                                label = { Text("Jatuh Tempo (hari dari sekarang)") },
                                modifier = Modifier.fillMaxWidth(),
                                supportingText = {
                                    val days = dueDateDays.toIntOrNull() ?: 30
                                    val dueDate = LocalDate.now().plusDays(days.toLong())
                                    Text("Jatuh tempo: ${dueDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))}")
                                }
                            )
                            
                            // Tax Rate Dropdown
                            ExposedDropdownMenuBox(
                                expanded = expandedTaxDropdown,
                                onExpandedChange = { expandedTaxDropdown = !expandedTaxDropdown }
                            ) {
                                OutlinedTextField(
                                    value = "PPN ${selectedTaxRate.toInt()}%",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Tarif Pajak") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTaxDropdown) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedTaxDropdown,
                                    onDismissRequest = { expandedTaxDropdown = false }
                                ) {
                                    taxRateOptions.forEach { rate ->
                                        DropdownMenuItem(
                                            text = { Text(if (rate == 0.0) "Tanpa PPN (0%)" else "PPN ${rate.toInt()}%") },
                                            onClick = {
                                                selectedTaxRate = rate
                                                expandedTaxDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                            
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("Catatan") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3
                            )
                        }
                        
                        // Right Column - Total Preview
                        Card(
                            modifier = Modifier.width(300.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Ringkasan", style = MaterialTheme.typography.titleMedium)
                                Divider()
                                
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Subtotal:")
                                    Text(FormatUtils.formatCurrency(calculateSubtotal()))
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("PPN (${selectedTaxRate.toInt()}%):")
                                    Text(FormatUtils.formatCurrency(calculateTax()))
                                }
                                Divider()
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Total:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                    Text(
                                        FormatUtils.formatCurrency(calculateTotal()),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Line Items Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Item Faktur", style = MaterialTheme.typography.titleMedium)
                        Button(
                            onClick = { lineItems = lineItems + LineItemEntry() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tambah Item")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Table Header
                    if (lineItems.isNotEmpty()) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Deskripsi", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold)
                                    Text("Qty", modifier = Modifier.weight(0.5f), fontWeight = FontWeight.Bold)
                                    Text("Harga Satuan", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                                    Text("Jumlah", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(48.dp))
                                }
                                Divider()
                                
                                // Line Items with proper key
                                LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
                                    itemsIndexed(lineItems, key = { index, _ -> index }) { index, item ->
                                        val qty = item.qty.toIntOrNull() ?: 0
                                        val price = item.price.toDoubleOrNull() ?: 0.0
                                        val amount = qty * price
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = item.description,
                                                onValueChange = { 
                                                    lineItems = lineItems.toMutableList().also { list ->
                                                        list[index] = item.copy(description = it)
                                                    }
                                                },
                                                placeholder = { Text("Nama barang/jasa") },
                                                modifier = Modifier.weight(2f),
                                                singleLine = true
                                            )
                                            OutlinedTextField(
                                                value = item.qty,
                                                onValueChange = { newVal ->
                                                    if (newVal.all { c -> c.isDigit() }) {
                                                        lineItems = lineItems.toMutableList().also { list ->
                                                            list[index] = item.copy(qty = newVal)
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.weight(0.5f),
                                                singleLine = true
                                            )
                                            OutlinedTextField(
                                                value = item.price,
                                                onValueChange = { newVal ->
                                                    if (newVal.all { c -> c.isDigit() || c == '.' }) {
                                                        lineItems = lineItems.toMutableList().also { list ->
                                                            list[index] = item.copy(price = newVal)
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true
                                            )
                                            Text(
                                                FormatUtils.formatCurrency(amount),
                                                modifier = Modifier.weight(1f),
                                                fontWeight = FontWeight.Medium
                                            )
                                            IconButton(
                                                onClick = {
                                                    lineItems = lineItems.toMutableList().also { it.removeAt(index) }
                                                }
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Klik 'Tambah Item' untuk menambahkan barang/jasa", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Action Buttons
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
                                        if (selectedCustomer != null && lineItems.isNotEmpty()) {
                                            val days = dueDateDays.toIntOrNull() ?: 30
                                            val dueDate = LocalDate.now().plusDays(days.toLong())
                                            
                                            val invoiceResult = AppModule.invoiceService.createInvoice(
                                                customerId = selectedCustomer!!.customerId,
                                                dueDate = dueDate,
                                                taxRate = selectedTaxRate,
                                                notes = notes
                                            )
                                            
                                            when (invoiceResult) {
                                                is Result.Success -> {
                                                    val invoiceId = invoiceResult.data.invoiceId
                                                    lineItems.forEach { item ->
                                                        val qty = item.qty.toIntOrNull() ?: 1
                                                        val price = item.price.toDoubleOrNull() ?: 0.0
                                                        if (item.description.isNotEmpty() && qty > 0) {
                                                            AppModule.invoiceService.addLineItem(invoiceId, item.description, qty, price)
                                                        }
                                                    }
                                                    snackbarHostState.showSnackbar("Invoice berhasil dibuat!")
                                                    isCreating = false
                                                    resetForm()
                                                    loadData()
                                                }
                                                is Result.Error -> {
                                                    snackbarHostState.showSnackbar("Gagal: ${invoiceResult.message}")
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Error: ${e.message}")
                                    }
                                }
                            },
                            enabled = selectedCustomer != null && lineItems.any { it.description.isNotEmpty() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simpan Faktur")
                        }
                    }
                }
            } else {
                // LIST VIEW
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Daftar Faktur", style = MaterialTheme.typography.headlineMedium)
                        Button(onClick = { 
                            resetForm()
                            isCreating = true 
                        }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Buat Faktur")
                        }
                    }
                    
                    DataTable(
                        columns = listOf(
                            DataColumn("No. Faktur", 1f),
                            DataColumn("Tanggal", 0.8f),
                            DataColumn("Jatuh Tempo", 0.8f),
                            DataColumn("Pelanggan", 1.2f),
                            DataColumn("Total", 1f),
                            DataColumn("Dibayar", 1f),
                            DataColumn("Sisa", 1f),
                            DataColumn("Status", 0.8f)
                        ),
                        data = invoices,
                        onRowClick = { invoice ->
                            selectedInvoiceForAction = invoice
                        }
                    ) { invoice ->
                        DataRow {
                            DataCell(invoice.invoiceNumber, 1f, fontWeight = FontWeight.Bold)
                            DataCell(FormatUtils.formatDate(invoice.invoiceDate), 0.8f)
                            DataCell(FormatUtils.formatDate(invoice.dueDate), 0.8f)
                            val customerName = customers.find { it.customerId == invoice.customerId }?.name ?: "Unknown"
                            DataCell(customerName, 1.2f)
                            DataCell(FormatUtils.formatCurrency(invoice.totalAmount), 1f)
                            DataCell(FormatUtils.formatCurrency(invoice.paidAmount), 1f, color = MaterialTheme.colorScheme.primary)
                            DataCell(FormatUtils.formatCurrency(invoice.getBalanceDue()), 1f, 
                                color = if (invoice.getBalanceDue() > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                            DataCell(
                                translateStatus(invoice.status), 
                                0.8f,
                                color = when (invoice.status) {
                                    InvoiceStatus.PAID -> MaterialTheme.colorScheme.primary
                                    InvoiceStatus.OVERDUE -> MaterialTheme.colorScheme.error
                                    InvoiceStatus.CANCELLED -> MaterialTheme.colorScheme.outline
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    // Hint
                    Text(
                        "💡 Klik baris invoice untuk melihat aksi (Kirim/Batalkan)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// Immutable data class for form state
data class LineItemEntry(
    val description: String = "",
    val qty: String = "1",
    val price: String = "0"
)
