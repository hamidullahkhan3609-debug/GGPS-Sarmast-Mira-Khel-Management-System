package com.example.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object Students : Screen("students")
    object StudentDetails : Screen("student_details/{studentId}") {
        fun createRoute(studentId: String) = "student_details/$studentId"
    }
    object AddStudent : Screen("add_student")
    object EditStudent : Screen("edit_student/{studentId}") {
        fun createRoute(studentId: String) = "edit_student/$studentId"
    }
    object Teachers : Screen("teachers")
    object Subjects : Screen("subjects")
    object Exams : Screen("exams")
    object Marks : Screen("marks")
    object Attendance : Screen("attendance")
    object MonthlyAttendance : Screen("monthly_attendance")
    object Timetable : Screen("timetable")
    object Notices : Screen("notices")
    object Reports : Screen("reports")
    object Users : Screen("users")
    object Profile : Screen("profile")
    object Settings : Screen("settings")
}
