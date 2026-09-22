package com.example.ui.students

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.data.model.SCHOOL_CLASSES
import com.example.data.model.SchoolUser
import com.example.data.model.Student
import com.example.data.model.UserRole
import com.example.data.repository.SchoolRepository
import com.example.ui.components.ConfirmDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.SchoolNavyPrimary
import com.example.ui.theme.SchoolNavySecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentsScreen(
    currentUser: SchoolUser,
    schoolRepository: SchoolRepository,
    onNavigateBack: () -> Unit,
    onNavigateToAddStudent: () -> Unit,
    onNavigateToStudentDetails: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedClass by remember { mutableStateOf("All Classes") }
    var searchQuery by remember { mutableStateOf("") }
    var studentToDelete by remember { mutableStateOf<Student?>(null) }

    val studentsList by schoolRepository.getStudentsFlow(
        if (selectedClass == "All Classes") null else selectedClass
    ).collectAsState(initial = emptyList())

    val filteredStudents = remember(studentsList, searchQuery) {
        if (searchQuery.isBlank()) {
            studentsList
        } else {
            val q = searchQuery.trim().lowercase()
            studentsList.filter {
                it.name.lowercase().contains(q) ||
                it.fatherName.lowercase().contains(q) ||
                it.admissionNumber.lowercase().contains(q)
            }
        }
    }

    val canManage = currentUser.userRole == UserRole.SUPER_ADMIN || currentUser.userRole == UserRole.ADMIN

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Students Directory", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (canManage) {
                FloatingActionButton(
                    onClick = onNavigateToAddStudent,
                    containerColor = SchoolNavyPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_student_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Student")
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
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by name, father name, or admission #") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("student_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Class Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedClass == "All Classes",
                        onClick = { selectedClass = "All Classes" },
                        label = { Text("All Classes") }
                    )
                }
                items(SCHOOL_CLASSES) { className ->
                    FilterChip(
                        selected = selectedClass == className,
                        onClick = { selectedClass = className },
                        label = { Text(className) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Count header
            Text(
                text = "Showing ${filteredStudents.size} students",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredStudents.isEmpty()) {
                EmptyStateView(
                    message = if (searchQuery.isNotEmpty()) "No students match '$searchQuery'" else "No students enrolled in $selectedClass yet.",
                    icon = Icons.Default.School,
                    actionLabel = if (canManage) "+ Add New Student" else null,
                    onActionClick = if (canManage) onNavigateToAddStudent else null
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredStudents, key = { it.studentId }) { student ->
                        StudentItemCard(
                            student = student,
                            onClick = { onNavigateToStudentDetails(student.studentId) },
                            onDelete = if (canManage) { { studentToDelete = student } } else null
                        )
                    }
                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
    }

    if (studentToDelete != null) {
        ConfirmDialog(
            title = "Archive Student Record",
            message = "Are you sure you want to archive student ${studentToDelete?.name} (${studentToDelete?.admissionNumber})? This record will be safely preserved in school archives.",
            confirmButtonText = "Archive Student",
            onConfirm = {
                studentToDelete?.studentId?.let { id ->
                    coroutineScope.launch {
                        schoolRepository.deleteStudent(id, softDelete = true)
                        studentToDelete = null
                    }
                }
            },
            onDismiss = { studentToDelete = null }
        )
    }
}

@Composable
fun StudentItemCard(
    student: Student,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("student_card_${student.studentId}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = student.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "D/O ${student.fatherName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "Adm #: ${student.admissionNumber}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = student.classId,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                if (onDelete != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Archive Student",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
