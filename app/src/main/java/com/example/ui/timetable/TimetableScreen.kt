package com.example.ui.timetable

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import com.example.data.repository.SchoolRepository
import com.example.ui.components.ConfirmDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.SchoolNavyPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    currentUser: SchoolUser,
    schoolRepository: SchoolRepository,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedClass by remember { mutableStateOf(SCHOOL_CLASSES.first()) }
    var selectedDay by remember { mutableStateOf(SCHOOL_DAYS.first()) }

    var showDialog by remember { mutableStateOf(false) }
    var entryToEdit by remember { mutableStateOf<TimetableEntry?>(null) }
    var entryToDelete by remember { mutableStateOf<TimetableEntry?>(null) }

    val fullTimetable by schoolRepository.getTimetableFlow(selectedClass).collectAsState(initial = emptyList())
    val filteredEntries = fullTimetable.filter { it.day.equals(selectedDay, ignoreCase = true) }
        .sortedBy { it.period }

    val canManage = currentUser.userRole == UserRole.SUPER_ADMIN || currentUser.userRole == UserRole.ADMIN

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("School Class Timetable", fontWeight = FontWeight.Bold) },
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
                        entryToEdit = null
                        showDialog = true
                    },
                    containerColor = SchoolNavyPrimary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Period Slot")
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
            // Class selector chips
            Text("Select Class:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SCHOOL_CLASSES) { cls ->
                    FilterChip(
                        selected = selectedClass == cls,
                        onClick = { selectedClass = cls },
                        label = { Text(cls) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Day selector chips
            Text("Select Day:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SCHOOL_DAYS) { day ->
                    FilterChip(
                        selected = selectedDay == day,
                        onClick = { selectedDay = day },
                        label = { Text(day) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (filteredEntries.isEmpty()) {
                EmptyStateView(
                    message = "No timetable periods scheduled for $selectedClass on $selectedDay.",
                    icon = Icons.Default.Schedule,
                    actionLabel = if (canManage) "+ Add Period" else null,
                    onActionClick = if (canManage) { { showDialog = true } } else null
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredEntries, key = { it.timetableId }) { entry ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
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
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "P${entry.period}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.subject,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Teacher: ${entry.teacher}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${entry.startTime} - ${entry.endTime}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                    )
                                }

                                if (canManage) {
                                    IconButton(onClick = {
                                        entryToEdit = entry
                                        showDialog = true
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { entryToDelete = entry }) {
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
        var periodStr by remember { mutableStateOf(entryToEdit?.period?.toString() ?: "1") }
        var subject by remember { mutableStateOf(entryToEdit?.subject ?: "English") }
        var teacher by remember { mutableStateOf(entryToEdit?.teacher ?: "Miss Sheeba Khan") }
        var startTime by remember { mutableStateOf(entryToEdit?.startTime ?: "08:00 AM") }
        var endTime by remember { mutableStateOf(entryToEdit?.endTime ?: "08:45 AM") }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (entryToEdit != null) "Edit Period Slot" else "Add Period Slot", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = periodStr,
                        onValueChange = { periodStr = it },
                        label = { Text("Period Number (1 - 6)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = teacher,
                        onValueChange = { teacher = it },
                        label = { Text("Teacher Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time (e.g. 08:00 AM)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End Time (e.g. 08:45 AM)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = periodStr.toIntOrNull() ?: 1
                        coroutineScope.launch {
                            val item = (entryToEdit ?: TimetableEntry(timetableId = "tt_" + System.currentTimeMillis())).copy(
                                classId = selectedClass,
                                day = selectedDay,
                                period = p,
                                subject = subject.trim(),
                                teacher = teacher.trim(),
                                startTime = startTime.trim(),
                                endTime = endTime.trim()
                            )
                            schoolRepository.saveTimetableEntry(item)
                            showDialog = false
                        }
                    }
                ) {
                    Text("Save Period")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (entryToDelete != null) {
        ConfirmDialog(
            title = "Delete Timetable Slot",
            message = "Are you sure you want to remove Period ${entryToDelete?.period} (${entryToDelete?.subject})?",
            confirmButtonText = "Delete",
            onConfirm = {
                entryToDelete?.timetableId?.let { id ->
                    coroutineScope.launch {
                        schoolRepository.deleteTimetableEntry(id)
                        entryToDelete = null
                    }
                }
            },
            onDismiss = { entryToDelete = null }
        )
    }
}
