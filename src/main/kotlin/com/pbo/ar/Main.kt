package com.pbo.ar

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.pbo.ar.data.database.DatabaseManager
import com.pbo.ar.ui.components.AppLayout
import com.pbo.ar.ui.navigation.NavigationState
import com.pbo.ar.ui.navigation.Screen
import com.pbo.ar.ui.screens.*
import com.pbo.ar.ui.theme.AppTheme
import java.io.File

@Composable
@Preview
fun App() {
    AppTheme {
        AppLayout {
            // Screen Switching
            when (NavigationState.currentScreen) {
                Screen.Dashboard -> DashboardScreen()
                Screen.Customers -> CustomerScreen()
                Screen.Invoices -> InvoiceScreen()
                Screen.Payments -> PaymentScreen()
                Screen.Reports -> ReportScreen()
            }
        }
    }
}

fun main() = application {
    // Ensure database connection
    try {
        DatabaseManager.connect()
    } catch (e: Exception) {
        println("Failed to connect to database: ${e.message}")
        // In a real app, logic to show error dialog would go here
    }
    
    Window(
        onCloseRequest = ::exitApplication, 
        title = "Accounts Receivable System"
    ) {
        App()
    }
}
