package com.pbo.ar.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pbo.ar.di.AppModule
import com.pbo.ar.domain.service.AgingBucket // Import this
import com.pbo.ar.ui.components.DataCell
import com.pbo.ar.ui.components.DataColumn
import com.pbo.ar.ui.components.DataRow
import com.pbo.ar.ui.components.DataTable
import com.pbo.ar.ui.util.FormatUtils
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun ReportScreen() {
    val scope = rememberCoroutineScope()
    var agingBuckets by remember { mutableStateOf<List<AgingBucket>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    fun loadData() {
        scope.launch {
            isLoading = true
            try {
                // Return type is AgingReport, we want buckets from it
                val report = AppModule.reportService.getAgingReport()
                agingBuckets = report.buckets
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

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Laporan Umur Piutang (Aging AR)", style = MaterialTheme.typography.headlineMedium)
            
            OutlinedButton(onClick = { loadData() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Refresh")
            }
        }
        
        if (isLoading) {
             Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // Summary Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Piutang Tertunggak", style = MaterialTheme.typography.labelLarge)
                        val total = agingBuckets.sumOf { it.totalAmount }
                        Text(FormatUtils.formatCurrency(total), style = MaterialTheme.typography.displaySmall)
                    }
                    Text("Per: ${FormatUtils.formatDate(LocalDate.now())}", style = MaterialTheme.typography.bodyMedium)
                }
            }

            DataTable(
                columns = listOf(
                    DataColumn("Rentang Waktu (Hari)", 1f),
                    DataColumn("Jumlah Faktur", 1f),
                    DataColumn("Total Nominal", 1f)
                ),
                data = agingBuckets
            ) { bucket ->
                DataRow {
                    DataCell(bucket.label, 1f, fontWeight = FontWeight.Medium) // Use bucket.label
                    DataCell(bucket.invoiceCount.toString(), 1f)
                    DataCell(FormatUtils.formatCurrency(bucket.totalAmount), 1f, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
