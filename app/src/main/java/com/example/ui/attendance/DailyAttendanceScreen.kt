package com.example.ui.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AttendanceRecord
import com.example.data.model.SCHOOL_CLASSES
import com.example.data.model.SchoolUser
import com.example.data.model.UserRole
import com.example.data.repository.SchoolRepository
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.SchoolNavyPrimary
import com.example.ui.theme.StatusAbsent
import com.example.ui.theme.StatusLeave
import com.example.ui.theme.StatusPresent
import com.example.utils.DateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyAttendanceScreen(
    currentUser: SchoolUser,
    schoolRepository: SchoolRepository,
    onNavigateBack: () -> Unit,
    onNavigateToMonthly: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var selectedClass by remember { mutableStateOf(SCHOOL_CLASSES.first()) }
    var selectedDate by remember { mutableStateOf(DateUtils.todayString()) }

    val studentsInClass by schoolRepository.getStudentsFlow(selectedClass).collectAsState(initial = emptyList())
    val existingRecords by schoolRepository.getAttendanceFlow(selectedClass, selectedDate).collectAsState(initial = emptyList())

    // Map studentId -> "Present" | "Absent" | "Leave"
    var attendanceStatusMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isSaving by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    // When existing records or students change, initialize status map
    LaunchedEffect(existingRecords, studentsInClass) {
        val map = mutableMapOf<String, String>()
        for (st in studentsInClass) {
            val record = existingRecords.find { it.studentId == st.studentId }
            map[st.studentId] = record?.status ?: "Present" // Default to Present for efficiency
        }
        attendanceStatusMap = map
    }

    val canMark = currentUser.userRole == UserRole.SUPER_ADMIN ||
                  currentUser.userRole == UserRole.ADMIN ||
                  currentUser.userRole == UserRole.TEACHER

    val presentCount = attendanceStatusMap.values.count { it == "Present" }
    val absentCount = attendanceStatusMap.values.count { it == "Absent" }
    val leaveCount = attendanceStatusMap.values.count { it == "Leave" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Attendance Register", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToMonthly) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Monthly Attendance View")
                    }
                }
            )
        },
        bottomBar = {
            if (canMark && studentsInClass.isNotEmpty()) {
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = feedbackMessage ?: "P: $presentCount | A: $absentCount | L: $leaveCount",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (feedbackMessage != null) StatusPresent else MaterialTheme.colorScheme.primary
                            )
                        }

                        Button(
                            onClick = {
                                isSaving = true
                                feedbackMessage = null
                                coroutineScope.launch {
                                    val list = studentsInClass.map { st ->
                                        val status = attendanceStatusMap[st.studentId] ?: "Present"
                                        AttendanceRecord(
                                            attendanceId = "${selectedClass}_${selectedDate}_${st.studentId}",
                                            studentId = st.studentId,
                                            studentName = st.name,
                                            admissionNumber = st.admissionNumber,
                                            classId = selectedClass,
                                            date = selectedDate,
                                            status = status,
                                            markedBy = currentUser.name
                                        )
                                    }
                                    schoolRepository.saveAttendanceBatch(list)
                                    isSaving = false
                                    feedbackMessage = "Attendance saved successfully!"
                                }
                            },
                            enabled = !isSaving,
                            modifier = Modifier.testTag("save_attendance_button")
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save Attendance")
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Controls Card: Class selector + Date selector + Mark All Present
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Class Dropdown
                        var classMenuExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = classMenuExpanded,
                            onExpandedChange = { classMenuExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = selectedClass,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Class") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classMenuExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                singleLine = true
                            )
                            ExposedDropdownMenu(
                                expanded = classMenuExpanded,
                                onDismissRequest = { classMenuExpanded = false }
                            ) {
                                SCHOOL_CLASSES.forEach { cls ->
                                    DropdownMenuItem(
                                        text = { Text(cls) },
                                        onClick = {
                                            selectedClass = cls
                                            classMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Date field
                        OutlinedTextField(
                            value = selectedDate,
                            onValueChange = { selectedDate = it },
                            label = { Text("Date (YYYY-MM-DD)") },
                            modifier = Modifier.weight(1.2f),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${studentsInClass.size} Students Enrolled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )

                        if (canMark) {
                            TextButton(
                                onClick = {
                                    val map = studentsInClass.associate { it.studentId to "Present" }
                                    attendanceStatusMap = map
                                    feedbackMessage = null
                                }
                            ) {
                                Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Mark All Present")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (studentsInClass.isEmpty()) {
                EmptyStateView(
                    message = "No students enrolled in $selectedClass to mark attendance.",
                    icon = Icons.Default.FactCheck
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(studentsInClass, key = { it.studentId }) { student ->
                        val currentStatus = attendanceStatusMap[student.studentId] ?: "Present"

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = student.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = student.admissionNumber,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    AttendanceStatusChip(
                                        label = "P",
                                        fullLabel = "Present",
                                        isSelected = currentStatus == "Present",
                                        color = StatusPresent,
                                        enabled = canMark,
                                        onClick = {
                                            attendanceStatusMap = attendanceStatusMap + (student.studentId to "Present")
                                            feedbackMessage = null
                                        }
                                    )
                                    AttendanceStatusChip(
                                        label = "A",
                                        fullLabel = "Absent",
                                        isSelected = currentStatus == "Absent",
                                        color = StatusAbsent,
                                        enabled = canMark,
                                        onClick = {
                                            attendanceStatusMap = attendanceStatusMap + (student.studentId to "Absent")
                                            feedbackMessage = null
                                        }
                                    )
                                    AttendanceStatusChip(
                                        label = "L",
                                        fullLabel = "Leave",
                                        isSelected = currentStatus == "Leave",
                                        color = StatusLeave,
                                        enabled = canMark,
                                        onClick = {
                                            attendanceStatusMap = attendanceStatusMap + (student.studentId to "Leave")
                                            feedbackMessage = null
                                        }
                                    )
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
fun AttendanceStatusChip(
    label: String,
    fullLabel: String,
    isSelected: Boolean,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) color else color.copy(alpha = 0.12f),
        modifier = Modifier.size(width = 38.dp, height = 36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else color
            )
        }
    }
}
