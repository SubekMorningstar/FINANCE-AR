package com.pbo.ar.ui.screens

import androidx.compose.foundation.layout.*
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
import com.pbo.ar.domain.model.Result
import com.pbo.ar.ui.components.*
import com.pbo.ar.ui.util.FormatUtils
import kotlinx.coroutines.launch

@Composable
fun CustomerScreen() {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var customers by remember { mutableStateOf<List<Customer>>(emptyList()) }
    var balances by remember { mutableStateOf<Map<Int, Double>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(false) }
    
    // Dialog State
    var showDialog by remember { mutableStateOf(false) }
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    
    // NEW: Delete Confirmation Dialog
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var customerToDelete by remember { mutableStateOf<Customer?>(null) }

    fun loadData() {
        scope.launch {
            isLoading = true
            try {
                val list = AppModule.customerService.getAllCustomers()
                customers = list
                
                // Fetch balances
                val balanceMap = list.associate { 
                    it.customerId to AppModule.customerService.getCustomerBalance(it.customerId) 
                }
                balances = balanceMap
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    // Edit/Add Dialog
    if (showDialog) {
        CustomerDialog(
            customer = selectedCustomer,
            onDismiss = { 
                showDialog = false
                selectedCustomer = null 
            },
            onConfirm = { name, email, phone, address, limit ->
                scope.launch {
                    try {
                        if (selectedCustomer == null) {
                            val result = AppModule.customerService.createCustomer(name, email, phone, address, limit)
                            when (result) {
                                is Result.Success -> snackbarHostState.showSnackbar("Pelanggan berhasil ditambahkan")
                                is Result.Error -> snackbarHostState.showSnackbar("Gagal: ${result.message}")
                            }
                        } else {
                            val result = AppModule.customerService.updateCustomer(selectedCustomer!!.customerId, name, email, phone, address, limit)
                            when (result) {
                                is Result.Success -> snackbarHostState.showSnackbar("Pelanggan berhasil diupdate")
                                is Result.Error -> snackbarHostState.showSnackbar("Gagal: ${result.message}")
                            }
                        }
                        showDialog = false
                        selectedCustomer = null
                        loadData() // Auto refresh after save
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Error: ${e.message}")
                    }
                }
            }
        )
    }
    
    // Delete Confirmation Dialog
    if (showDeleteConfirm && customerToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteConfirm = false
                customerToDelete = null
            },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Hapus Pelanggan?") },
            text = { 
                Column {
                    Text("Apakah Anda yakin ingin menghapus pelanggan ini?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Kode: ${customerToDelete!!.code}", fontWeight = FontWeight.Bold)
                            Text("Nama: ${customerToDelete!!.name}")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Aksi ini tidak dapat dibatalkan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val result = AppModule.customerService.deleteCustomer(customerToDelete!!.customerId)
                            when (result) {
                                is Result.Success -> {
                                    snackbarHostState.showSnackbar("Pelanggan berhasil dihapus")
                                    loadData()
                                }
                                is Result.Error -> {
                                    snackbarHostState.showSnackbar("Gagal hapus: ${result.message}")
                                }
                            }
                            showDeleteConfirm = false
                            customerToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { 
                    showDeleteConfirm = false
                    customerToDelete = null
                }) {
                    Text("Batal")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Toolbar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Kelola Pelanggan",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Refresh")
                    }
                    
                    Button(onClick = { 
                        selectedCustomer = null
                        showDialog = true 
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tambah Pelanggan")
                    }
                }
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // Data Table with actions
                DataTable(
                    columns = listOf(
                        DataColumn("Kode", 0.5f),
                        DataColumn("Nama Lengkap", 1.3f),
                        DataColumn("Email", 1.2f),
                        DataColumn("Telepon", 0.8f),
                        DataColumn("Batas Kredit", 1f),
                        DataColumn("Saldo Hutang", 1f),
                        DataColumn("Aksi", 0.6f)
                    ),
                    data = customers
                ) { customer ->
                    DataRow {
                        DataCell(customer.code, 0.5f)
                        DataCell(customer.name, 1.3f, fontWeight = FontWeight.Medium)
                        DataCell(customer.email, 1.2f)
                        DataCell(customer.phone, 0.8f)
                        DataCell(FormatUtils.formatCurrency(customer.creditLimit), 1f)
                        
                        val balance = balances[customer.customerId] ?: 0.0
                        DataCell(
                            text = FormatUtils.formatCurrency(balance),
                            weight = 1f,
                            color = if (balance > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            fontWeight = if (balance > 0) FontWeight.Bold else null
                        )
                        
                        // Action buttons
                        Row(
                            modifier = Modifier.weight(0.6f),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = { 
                                    selectedCustomer = customer
                                    showDialog = true 
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit, 
                                    contentDescription = "Edit",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = { 
                                    customerToDelete = customer
                                    showDeleteConfirm = true 
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete, 
                                    contentDescription = "Delete",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                
                // Hint
                Text(
                    "💡 Gunakan tombol Edit untuk mengubah data atau Delete untuk menghapus pelanggan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
