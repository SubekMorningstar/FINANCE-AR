package com.pbo.ar.ui.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object FormatUtils {
    private val localeID = Locale("id", "ID")
    private val currencyFormat = NumberFormat.getCurrencyInstance(localeID)
    private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", localeID)

    fun formatCurrency(amount: Double): String {
        return currencyFormat.format(amount)
    }

    fun formatDate(date: LocalDate): String {
        return date.format(dateFormatter)
    }
    
    fun formatDate(dateString: String): String {
        return try {
            LocalDate.parse(dateString).format(dateFormatter)
        } catch (e: Exception) {
            dateString
        }
    }
}
