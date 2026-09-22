package com.example.utils

import com.example.data.model.GradingRule
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object ResultCalculator {
    private val defaultRules = listOf(
        GradingRule("A+", 90.0, 100.0, "Outstanding"),
        GradingRule("A", 80.0, 89.99, "Excellent"),
        GradingRule("B", 70.0, 79.99, "Very Good"),
        GradingRule("C", 60.0, 69.99, "Good"),
        GradingRule("D", 50.0, 59.99, "Satisfactory"),
        GradingRule("F", 0.0, 49.99, "Fail")
    )

    fun calculatePercentage(obtained: Double, total: Double): Double {
        if (total <= 0) return 0.0
        val raw = (obtained / total) * 100.0
        return String.format(Locale.US, "%.2f", raw.coerceIn(0.0, 100.0)).toDoubleOrNull() ?: 0.0
    }

    fun calculateGrade(percentage: Double, customRules: List<GradingRule>? = null): String {
        val rules = if (!customRules.isNullOrEmpty()) customRules else defaultRules
        for (rule in rules) {
            if (percentage >= rule.minPercentage && percentage <= rule.maxPercentage) {
                return rule.grade
            }
        }
        return if (percentage >= 50.0) "D" else "F"
    }

    fun isPassed(grade: String): Boolean {
        return grade != "F"
    }

    fun getResultStatus(percentage: Double, grade: String): String {
        return if (isPassed(grade)) "PASS" else "FAIL"
    }
}

object DateUtils {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
    private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.US)

    fun todayString(): String {
        return dateFormat.format(Date())
    }

    fun formatDisplayDate(dateStr: String): String {
        return try {
            val date = dateFormat.parse(dateStr)
            if (date != null) displayFormat.format(date) else dateStr
        } catch (_: Exception) {
            dateStr
        }
    }

    fun currentMonthName(): String {
        return SimpleDateFormat("MMMM", Locale.US).format(Date())
    }

    fun currentYear(): Int {
        return Calendar.getInstance().get(Calendar.YEAR)
    }

    fun getMonthsList(): List<String> {
        return listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
    }

    fun getYearsList(): List<String> {
        val current = currentYear()
        return listOf((current - 1).toString(), current.toString(), (current + 1).toString())
    }
}
