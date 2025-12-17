package com.pbo.ar.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class Screen {
    Dashboard,
    Customers,
    Invoices,
    Payments,
    Reports
}

object NavigationState {
    var currentScreen by mutableStateOf(Screen.Dashboard)
    
    fun navigateTo(screen: Screen) {
        currentScreen = screen
    }
}
