package com.pbo.ar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.pbo.ar.ui.navigation.NavigationState
import com.pbo.ar.ui.navigation.Screen

@Composable
fun AppLayout(
    content: @Composable () -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        // Sidebar
        NavigationSidebar()
        
        // Main Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp)
        ) {
            content()
        }
    }
}

@Composable
fun NavigationSidebar() {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxHeight().width(250.dp), // Using a wide NavigationRail as a permanent drawer equivalent
        header = {
            Column(
                modifier = Modifier.padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = "Logo",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "AR System",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) {
        // Menu Items
        Spacer(modifier = Modifier.height(12.dp))
        
        NavRailItem("Dashboard", Screen.Dashboard, Icons.Default.Dashboard)
        NavRailItem("Pelanggan", Screen.Customers, Icons.Default.People)
        NavRailItem("Faktur", Screen.Invoices, Icons.Default.Receipt)
        NavRailItem("Pembayaran", Screen.Payments, Icons.Default.Payments)
        NavRailItem("Laporan", Screen.Reports, Icons.Default.Assessment)
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Footer (Settings/Logout placeholder)
        Text(
            text = "Sistem AR v1.0.0",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(bottom = 16.dp),
            color = MaterialTheme.colorScheme.outline
        )
    }
}


@Composable
private fun NavRailItem(label: String, screen: Screen, icon: ImageVector) {
    val selected = NavigationState.currentScreen == screen
    
    NavigationRailItem(
        selected = selected,
        onClick = { NavigationState.navigateTo(screen) },
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label) },
        colors = NavigationRailItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
            selectedTextColor = MaterialTheme.colorScheme.onSurface,
            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
    )
}
