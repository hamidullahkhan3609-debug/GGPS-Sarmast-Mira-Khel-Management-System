package com.example.ui.students

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AttendanceRecord
import com.example.data.model.MarkRecord
import com.example.data.model.SchoolUser
import com.example.data.model.Student
import com.example.data.model.UserRole
import com.example.data.repository.SchoolRepository
import com.example.ui.components.EmptyStateView
import com.example.ui.components.LoadingStateView
import com.example.ui.theme.SchoolNavyPrimary
import com.example.ui.theme.StatusAbsent
import com.example.ui.theme.StatusLeave
import com.example.ui.theme.StatusPresent
import com.example.utils.DateUtils
import com.example.utils.ResultCalculator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDetailsScreen(
    studentId: String,
    currentUser: SchoolUser,
    schoolRepository: SchoolRepository,
    onNavigateBack: () -> Unit,
    onNavigateToEditStudent: (String) -> Unit
) {
    var student by remember { mutableStateOf<Student?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val attendanceList by schoolRepository.getStudentAttendanceFlow(studentId).collectAsState(initial = emptyList())
    val marksList by schoolRepository.getStudentMarksFlow(studentId).collectAsState(initial = emptyList())

    LaunchedEffect(studentId) {
        isLoading = true
        student = schoolRepository.getStudent(studentId)
        isLoading = false
    }

    val canEdit = currentUser.userRole == UserRole.SUPER_ADMIN || currentUser.userRole == UserRole.ADMIN

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(student?.name ?: "Student Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (canEdit && student != null) {
                        IconButton(onClick = { onNavigateToEditStudent(studentId) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Student Profile")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            LoadingStateView(message = "Loading student profile...", modifier = Modifier.padding(padding))
        } else if (student == null) {
            EmptyStateView(
                message = "Student record not found.",
                modifier = Modifier.padding(padding)
            )
        } else {
            val st = student!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Header Profile Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = st.name.take(1).uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = st.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "D/O ${st.fatherName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "Admission #: ${st.admissionNumber}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = st.classId,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Tab Row
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Profile") },
                        icon = { Icon(Icons.Default.Person, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Attendance") },
                        icon = { Icon(Icons.Default.FactCheck, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Results") },
                        icon = { Icon(Icons.Default.Grade, contentDescription = null) }
                    )
                }

                when (selectedTab) {
                    0 -> StudentProfileTab(st)
                    1 -> StudentAttendanceTab(attendanceList)
                    2 -> StudentResultsTab(marksList)
                }
            }
        }
    }
}

@Composable
private fun StudentProfileTab(student: Student) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Official Academic Registration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    DetailRow(label = "School", value = "GGPS Sarmast Mira Khel Bannu")
                    DetailRow(label = "Enrolled Class", value = student.classId)
                    DetailRow(label = "Admission Number", value = student.admissionNumber)
                    DetailRow(label = "Admission Date", value = DateUtils.formatDisplayDate(student.admissionDate))
                    DetailRow(label = "Status", value = student.status.replaceFirstChar { it.uppercase() })
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Personal & Guardian Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    DetailRow(label = "Father / Guardian", value = student.fatherName)
                    DetailRow(label = "Date of Birth", value = DateUtils.formatDisplayDate(student.dob))
                    DetailRow(label = "Guardian Phone", value = student.phone.ifEmpty { "Not registered" })
                    DetailRow(label = "Tuition Fee", value = "Free (Government School)")
                }
            }
        }
    }
}

@Composable
private fun StudentAttendanceTab(attendanceList: List<AttendanceRecord>) {
    val total = attendanceList.size
    val present = attendanceList.count { it.status == "Present" }
    val absent = attendanceList.count { it.status == "Absent" }
    val leave = attendanceList.count { it.status == "Leave" }
    val rate = if (total > 0) ((present.toDouble() / total) * 100).toInt() else 100

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    AttendanceStat(label = "Present", count = "$present", color = StatusPresent)
                    AttendanceStat(label = "Absent", count = "$absent", color = StatusAbsent)
                    AttendanceStat(label = "Leave", count = "$leave", color = StatusLeave)
                    AttendanceStat(label = "Attendance %", count = "$rate%", color = SchoolNavyPrimary)
                }
            }
        }

        if (attendanceList.isEmpty()) {
            item {
                EmptyStateView(
                    message = "No attendance records recorded for this student yet.",
                    icon = Icons.Default.FactCheck
                )
            }
        } else {
            items(attendanceList) { record ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = DateUtils.formatDisplayDate(record.date),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (record.status) {
                                "Present" -> StatusPresent.copy(alpha = 0.15f)
                                "Absent" -> StatusAbsent.copy(alpha = 0.15f)
                                else -> StatusLeave.copy(alpha = 0.15f)
                            }
                        ) {
                            Text(
                                text = record.status,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = when (record.status) {
                                    "Present" -> StatusPresent
                                    "Absent" -> StatusAbsent
                                    else -> StatusLeave
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentResultsTab(marksList: List<MarkRecord>) {
    if (marksList.isEmpty()) {
        EmptyStateView(
            message = "No exam marks recorded for this student yet.",
            icon = Icons.Default.Grade
        )
    } else {
        val totalObtained = marksList.sumOf { it.obtainedMarks }
        val totalMax = marksList.sumOf { it.totalMarks }
        val overallPct = ResultCalculator.calculatePercentage(totalObtained, totalMax)
        val overallGrade = ResultCalculator.calculateGrade(overallPct)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Cumulative Performance",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Total: $totalObtained / $totalMax ($overallPct%)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (overallGrade == "F") StatusAbsent else StatusPresent
                        ) {
                            Text(
                                text = "Grade $overallGrade",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            items(marksList) { mark ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mark.subjectName.ifEmpty { "General Subject" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Marks: ${mark.obtainedMarks} / ${mark.totalMarks} (${mark.percentage}%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            if (mark.remarks.isNotEmpty()) {
                                Text(
                                    text = "Remarks: ${mark.remarks}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (mark.grade == "F") StatusAbsent.copy(alpha = 0.15f) else StatusPresent.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = mark.grade,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (mark.grade == "F") StatusAbsent else StatusPresent
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AttendanceStat(label: String, count: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = count, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
    }
}
