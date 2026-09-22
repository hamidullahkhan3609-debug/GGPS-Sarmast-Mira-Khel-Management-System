package com.example.ui.teachers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.data.model.*
import com.example.data.repository.SchoolRepository
import com.example.ui.components.ConfirmDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.SchoolNavyPrimary
import com.example.ui.theme.StatusAbsent
import com.example.ui.theme.StatusPresent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeachersScreen(
    currentUser: SchoolUser,
    schoolRepository: SchoolRepository,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var teacherToEdit by remember { mutableStateOf<Teacher?>(null) }
    var teacherToDelete by remember { mutableStateOf<Teacher?>(null) }

    val teachersList by schoolRepository.getTeachersFlow().collectAsState(initial = emptyList())

    val filteredTeachers = remember(teachersList, searchQuery) {
        if (searchQuery.isBlank()) {
            teachersList
        } else {
            val q = searchQuery.trim().lowercase()
            teachersList.filter {
                it.name.lowercase().contains(q) ||
                it.designation.lowercase().contains(q) ||
                it.subjects.any { s -> s.lowercase().contains(q) } ||
                it.classes.any { c -> c.lowercase().contains(q) }
            }
        }
    }

    val canManage = currentUser.userRole == UserRole.SUPER_ADMIN || currentUser.userRole == UserRole.ADMIN

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Teaching Staff Directory", fontWeight = FontWeight.Bold) },
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
                    onClick = {
                        teacherToEdit = null
                        showAddDialog = true
                    },
                    containerColor = SchoolNavyPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_teacher_fab")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add Teacher")
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
                label = { Text("Search teacher by name, designation, subject") },
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
                    .testTag("teacher_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Total Staff: ${filteredTeachers.size} teachers",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredTeachers.isEmpty()) {
                EmptyStateView(
                    message = if (searchQuery.isNotEmpty()) "No teachers match '$searchQuery'" else "No teachers registered yet.",
                    icon = Icons.Default.School,
                    actionLabel = if (canManage) "+ Add New Teacher" else null,
                    onActionClick = if (canManage) { { showAddDialog = true } } else null
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredTeachers, key = { it.teacherId }) { teacher ->
                        TeacherCard(
                            teacher = teacher,
                            canManage = canManage,
                            onEdit = {
                                teacherToEdit = teacher
                                showAddDialog = true
                            },
                            onToggleStatus = {
                                coroutineScope.launch {
                                    val newStatus = if (teacher.status == "active") "inactive" else "active"
                                    schoolRepository.saveTeacher(teacher.copy(status = newStatus))
                                }
                            },
                            onDelete = { teacherToDelete = teacher }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditTeacherDialog(
            teacher = teacherToEdit,
            onDismiss = { showAddDialog = false },
            onSave = { teacher ->
                coroutineScope.launch {
                    schoolRepository.saveTeacher(teacher)
                    showAddDialog = false
                }
            }
        )
    }

    if (teacherToDelete != null) {
        ConfirmDialog(
            title = "Remove Teacher Record",
            message = "Are you sure you want to remove ${teacherToDelete?.name} from staff directory?",
            confirmButtonText = "Delete",
            onConfirm = {
                teacherToDelete?.teacherId?.let { id ->
                    coroutineScope.launch {
                        schoolRepository.deleteTeacher(id)
                        teacherToDelete = null
                    }
                }
            },
            onDismiss = { teacherToDelete = null }
        )
    }
}

@Composable
fun TeacherCard(
    teacher: Teacher,
    canManage: Boolean,
    onEdit: () -> Unit,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = teacher.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = teacher.designation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (teacher.fatherName.isNotEmpty()) {
                        Text(
                            text = "Relation: ${teacher.fatherName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (teacher.status == "active") StatusPresent.copy(alpha = 0.15f) else StatusAbsent.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = teacher.status.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (teacher.status == "active") StatusPresent else StatusAbsent
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (teacher.classes.isNotEmpty()) {
                Text(
                    text = "Classes: ${teacher.classes.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }

            if (teacher.subjects.isNotEmpty()) {
                Text(
                    text = "Subjects: ${teacher.subjects.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }

            if (teacher.phone.isNotEmpty()) {
                Text(
                    text = "Phone: ${teacher.phone}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            if (canManage) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onToggleStatus) {
                        Text(if (teacher.status == "active") "Deactivate" else "Activate")
                    }
                    TextButton(onClick = onEdit) {
                        Text("Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Delete Teacher",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditTeacherDialog(
    teacher: Teacher?,
    onDismiss: () -> Unit,
    onSave: (Teacher) -> Unit
) {
    var name by remember { mutableStateOf(teacher?.name ?: "") }
    var fatherName by remember { mutableStateOf(teacher?.fatherName ?: "") }
    var designation by remember { mutableStateOf(teacher?.designation ?: "Primary School Teacher (PST)") }
    var phone by remember { mutableStateOf(teacher?.phone ?: "") }
    var subjectsStr by remember { mutableStateOf(teacher?.subjects?.joinToString(", ") ?: "English, Urdu") }
    var classesStr by remember { mutableStateOf(teacher?.classes?.joinToString(", ") ?: "Class 1, Class 2") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (teacher != null) "Edit Teacher Details" else "Add New Teacher", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (error != null) {
                    Text(error ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("Teacher Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = fatherName,
                    onValueChange = { fatherName = it },
                    label = { Text("Father's / Husband's Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = designation,
                    onValueChange = { designation = it },
                    label = { Text("Designation (e.g. PST, Head Mistress)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Contact Phone") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = classesStr,
                    onValueChange = { classesStr = it },
                    label = { Text("Assigned Classes (comma separated)") },
                    placeholder = { Text("Class 1, Class 2") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = subjectsStr,
                    onValueChange = { subjectsStr = it },
                    label = { Text("Assigned Subjects (comma separated)") },
                    placeholder = { Text("English, Urdu, Mathematics") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isEmpty()) {
                        error = "Teacher Name cannot be empty."
                        return@Button
                    }
                    val updated = (teacher ?: Teacher(teacherId = "t_" + System.currentTimeMillis())).copy(
                        name = name.trim(),
                        fatherName = fatherName.trim(),
                        designation = designation.trim(),
                        phone = phone.trim(),
                        classes = classesStr.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                        subjects = subjectsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                        status = teacher?.status ?: "active"
                    )
                    onSave(updated)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
