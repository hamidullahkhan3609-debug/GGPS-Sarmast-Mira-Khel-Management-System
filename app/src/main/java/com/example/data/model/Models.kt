package com.example.data.model

enum class UserRole(val displayName: String, val level: Int) {
    SUPER_ADMIN("Super Admin", 1),
    ADMIN("Admin", 2),
    ACCOUNTANT("Accountant", 3),
    TEACHER("Teacher", 4),
    STUDENT("Student", 5),
    PARENT("Parent", 6);

    companion object {
        fun fromString(role: String?): UserRole {
            return when (role?.uppercase()?.replace(" ", "_")) {
                "SUPER_ADMIN" -> SUPER_ADMIN
                "ADMIN" -> ADMIN
                "ACCOUNTANT" -> ACCOUNTANT
                "TEACHER" -> TEACHER
                "STUDENT" -> STUDENT
                "PARENT" -> PARENT
                else -> TEACHER
            }
        }
    }
}

data class SchoolUser(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = UserRole.TEACHER.name,
    val status: String = "active", // active, inactive, disabled
    val phone: String = "",
    val linkedStudentId: String? = null,
    val linkedTeacherId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val userRole: UserRole get() = UserRole.fromString(role)
}

data class Student(
    val studentId: String = "",
    val name: String = "",
    val fatherName: String = "",
    val admissionNumber: String = "",
    val classId: String = "Class 1",
    val className: String = "Class 1",
    val dob: String = "",
    val phone: String = "",
    val admissionDate: String = "",
    val status: String = "active", // active, inactive, archived
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class Teacher(
    val teacherId: String = "",
    val name: String = "",
    val fatherName: String = "",
    val phone: String = "",
    val designation: String = "Primary School Teacher (PST)",
    val subjects: List<String> = emptyList(),
    val classes: List<String> = emptyList(),
    val status: String = "active", // active, inactive
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class Subject(
    val subjectId: String = "",
    val name: String = "",
    val classIds: List<String> = emptyList(),
    val teacherIds: List<String> = emptyList(),
    val status: String = "active",
    val createdAt: Long = System.currentTimeMillis()
)

data class Exam(
    val examId: String = "",
    val name: String = "",
    val type: String = "Mid-Term", // Mid-Term, Annual, Monthly Test
    val session: String = "2026-2027",
    val classId: String = "All Classes",
    val startDate: String = "",
    val endDate: String = "",
    val status: String = "active", // upcoming, active, completed
    val subjects: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

data class MarkRecord(
    val markId: String = "",
    val examId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val admissionNumber: String = "",
    val classId: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val obtainedMarks: Double = 0.0,
    val totalMarks: Double = 100.0,
    val percentage: Double = 0.0,
    val grade: String = "F",
    val remarks: String = "",
    val markedBy: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

data class AttendanceRecord(
    val attendanceId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val admissionNumber: String = "",
    val classId: String = "",
    val date: String = "", // YYYY-MM-DD
    val status: String = "Present", // Present, Absent, Leave
    val markedBy: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class TimetableEntry(
    val timetableId: String = "",
    val classId: String = "Class 1",
    val day: String = "Monday",
    val period: Int = 1,
    val subject: String = "",
    val teacher: String = "",
    val startTime: String = "08:00 AM",
    val endTime: String = "08:45 AM"
)

data class Notice(
    val noticeId: String = "",
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val author: String = "School Administration",
    val priority: String = "Normal", // High, Medium, Normal
    val status: String = "published", // published, archived
    val attachmentUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class GradingRule(
    val grade: String = "A+",
    val minPercentage: Double = 90.0,
    val maxPercentage: Double = 100.0,
    val description: String = "Outstanding"
)

data class SchoolSettings(
    val schoolName: String = "Government Girls Primary School Sarmast Mira Khel Bannu",
    val shortName: String = "GGPS Sarmast Mira Khel Bannu",
    val headMistress: String = "Miss Sheeba Khan",
    val motto: String = "Educate Today, Empower Tomorrow.",
    val currentSession: String = "2026-2027",
    val location: String = "Sarmast Mira Khel, Bannu, Khyber Pakhtunkhwa, Pakistan",
    val gradingRules: List<GradingRule> = listOf(
        GradingRule("A+", 90.0, 100.0, "Outstanding"),
        GradingRule("A", 80.0, 89.99, "Excellent"),
        GradingRule("B", 70.0, 79.99, "Very Good"),
        GradingRule("C", 60.0, 69.99, "Good"),
        GradingRule("D", 50.0, 59.99, "Satisfactory"),
        GradingRule("F", 0.0, 49.99, "Fail")
    )
)

val SCHOOL_CLASSES = listOf("Class 1", "Class 2", "Class 3", "Class 4", "Class 5")
val SCHOOL_DAYS = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
val STANDARD_SUBJECTS = listOf("English", "Urdu", "Mathematics", "General Science", "Islamiat", "Social Studies")
