package com.pbo.ar.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.pbo.ar.di.AppModule
import com.pbo.ar.ui.util.FormatUtils
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen() {
    val scope = rememberCoroutineScope()
    var totalReceivables by remember { mutableStateOf(0.0) }
    var totalOverdue by remember { mutableStateOf(0.0) }
    var totalCustomers by remember { mutableStateOf(0) }
    var totalInvoices by remember { mutableStateOf(0) }
    
    // Load Data
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                // Fetch summary data directly from Services
                val summary = AppModule.reportService.getReceivableSummary()
                totalReceivables = summary.totalReceivable
                totalOverdue = summary.totalOverdue
                totalCustomers = summary.totalCustomers
                totalInvoices = summary.totalInvoices
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        Text(
            text = "Ringkasan Dashboard",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Summary Cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SummaryCard(
                title = "Total Piutang",
                value = FormatUtils.formatCurrency(totalReceivables),
                icon = Icons.Default.AttachMoney,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "Jatuh Tempo",
                value = FormatUtils.formatCurrency(totalOverdue),
                icon = Icons.Default.Warning,
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "Pelanggan Aktif",
                value = totalCustomers.toString(),
                icon = Icons.Default.People,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "Total Faktur",
                value = totalInvoices.toString(),
                icon = Icons.Default.Description,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Recent Activity Section
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Selamat Datang di Sistem AR",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Sistem siap digunakan. Mulai dengan mengelola pelanggan atau membuat faktur baru dari menu di samping.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
