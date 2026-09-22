package com.example.ui.subjects

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.SCHOOL_CLASSES
import com.example.data.model.SchoolUser
import com.example.data.model.Subject
import com.example.data.model.UserRole
import com.example.data.repository.SchoolRepository
import com.example.ui.components.ConfirmDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.SchoolNavyPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectsScreen(
    currentUser: SchoolUser,
    schoolRepository: SchoolRepository,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var subjectToEdit by remember { mutableStateOf<Subject?>(null) }
    var subjectToDelete by remember { mutableStateOf<Subject?>(null) }

    val subjectsList by schoolRepository.getSubjectsFlow().collectAsState(initial = emptyList())
    val canManage = currentUser.userRole == UserRole.SUPER_ADMIN || currentUser.userRole == UserRole.ADMIN

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("School Curriculum & Subjects", fontWeight = FontWeight.Bold) },
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
                        subjectToEdit = null
                        showDialog = true
                    },
                    containerColor = SchoolNavyPrimary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Subject")
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
            Text(
                text = "Primary School Curriculum (Classes 1 - 5)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            if (subjectsList.isEmpty()) {
                EmptyStateView(
                    message = "No subjects added to curriculum yet.",
                    icon = Icons.Default.Book,
                    actionLabel = if (canManage) "+ Add Subject" else null,
                    onActionClick = if (canManage) { { showDialog = true } } else null
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(subjectsList, key = { it.subjectId }) { subject ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = subject.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Taught in: ${if (subject.classIds.isEmpty()) "All Classes" else subject.classIds.joinToString(", ")}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }

                                if (canManage) {
                                    IconButton(onClick = {
                                        subjectToEdit = subject
                                        showDialog = true
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { subjectToDelete = subject }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
    }

    if (showDialog) {
        var name by remember { mutableStateOf(subjectToEdit?.name ?: "") }
        var selectedClasses by remember { mutableStateOf(subjectToEdit?.classIds?.toSet() ?: SCHOOL_CLASSES.toSet()) }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (subjectToEdit != null) "Edit Subject" else "Add New Subject", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Subject Name *") },
                        placeholder = { Text("e.g. General Science") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Applies to Classes:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)

                    Spacer(modifier = Modifier.height(6.dp))

                    SCHOOL_CLASSES.forEach { cls ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = selectedClasses.contains(cls),
                                onCheckedChange = { checked ->
                                    selectedClasses = if (checked) selectedClasses + cls else selectedClasses - cls
                                }
                            )
                            Text(cls, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.trim().isNotEmpty()) {
                            coroutineScope.launch {
                                val item = (subjectToEdit ?: Subject(subjectId = "sub_" + System.currentTimeMillis())).copy(
                                    name = name.trim(),
                                    classIds = selectedClasses.toList()
                                )
                                schoolRepository.saveSubject(item)
                                showDialog = false
                            }
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (subjectToDelete != null) {
        ConfirmDialog(
            title = "Delete Subject",
            message = "Are you sure you want to remove ${subjectToDelete?.name} from curriculum?",
            confirmButtonText = "Delete",
            onConfirm = {
                subjectToDelete?.subjectId?.let { id ->
                    coroutineScope.launch {
                        schoolRepository.deleteSubject(id)
                        subjectToDelete = null
                    }
                }
            },
            onDismiss = { subjectToDelete = null }
        )
    }
}
