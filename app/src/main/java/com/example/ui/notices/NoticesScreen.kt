package com.example.ui.notices

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
import com.example.data.model.Notice
import com.example.data.model.SchoolUser
import com.example.data.model.UserRole
import com.example.data.repository.SchoolRepository
import com.example.ui.components.ConfirmDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.SchoolNavyPrimary
import com.example.ui.theme.StatusAbsent
import com.example.ui.theme.StatusPresent
import com.example.utils.DateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticesScreen(
    currentUser: SchoolUser,
    schoolRepository: SchoolRepository,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var noticeToEdit by remember { mutableStateOf<Notice?>(null) }
    var noticeToDelete by remember { mutableStateOf<Notice?>(null) }

    val noticesList by schoolRepository.getNoticesFlow().collectAsState(initial = emptyList())
    val canManage = currentUser.userRole == UserRole.SUPER_ADMIN || currentUser.userRole == UserRole.ADMIN

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("School Notices & Circulars", fontWeight = FontWeight.Bold) },
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
                        noticeToEdit = null
                        showDialog = true
                    },
                    containerColor = SchoolNavyPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_notice_fab")
                ) {
                    Icon(Icons.Default.AddComment, contentDescription = "Post Notice")
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
                text = "Official Announcements from Head Mistress & Admin",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            if (noticesList.isEmpty()) {
                EmptyStateView(
                    message = "No school circulars or notices posted yet.",
                    icon = Icons.Default.Campaign,
                    actionLabel = if (canManage) "+ Post Notice" else null,
                    onActionClick = if (canManage) { { showDialog = true } } else null
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(noticesList, key = { it.noticeId }) { notice ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = notice.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (notice.priority == "High") StatusAbsent.copy(alpha = 0.15f) else StatusPresent.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = notice.priority,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (notice.priority == "High") StatusAbsent else StatusPresent
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = notice.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Issued by: ${notice.author}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Date: ${DateUtils.formatDisplayDate(notice.date)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }

                                    if (canManage) {
                                        Row {
                                            IconButton(onClick = {
                                                noticeToEdit = notice
                                                showDialog = true
                                            }) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                                            }
                                            IconButton(onClick = { noticeToDelete = notice }) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
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
        var title by remember { mutableStateOf(noticeToEdit?.title ?: "") }
        var description by remember { mutableStateOf(noticeToEdit?.description ?: "") }
        var priority by remember { mutableStateOf(noticeToEdit?.priority ?: "Normal") }
        var author by remember { mutableStateOf(noticeToEdit?.author ?: currentUser.name) }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (noticeToEdit != null) "Edit Circular" else "Post New Notice", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (error != null) {
                        Text(error ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it; error = null },
                        label = { Text("Notice Title *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it; error = null },
                        label = { Text("Notice Body / Message *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Normal", "Medium", "High").forEach { p ->
                            FilterChip(
                                selected = priority == p,
                                onClick = { priority = p },
                                label = { Text(p) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = author,
                        onValueChange = { author = it },
                        label = { Text("Author / Department") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.trim().isEmpty() || description.trim().isEmpty()) {
                            error = "Title and Description cannot be empty."
                            return@Button
                        }
                        coroutineScope.launch {
                            val item = (noticeToEdit ?: Notice(noticeId = "not_" + System.currentTimeMillis())).copy(
                                title = title.trim(),
                                description = description.trim(),
                                priority = priority,
                                author = author.trim(),
                                date = DateUtils.todayString(),
                                status = "published"
                            )
                            schoolRepository.saveNotice(item)
                            showDialog = false
                        }
                    }
                ) {
                    Text("Publish Notice")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (noticeToDelete != null) {
        ConfirmDialog(
            title = "Delete Notice",
            message = "Are you sure you want to remove '${noticeToDelete?.title}'?",
            confirmButtonText = "Delete",
            onConfirm = {
                noticeToDelete?.noticeId?.let { id ->
                    coroutineScope.launch {
                        schoolRepository.deleteNotice(id)
                        noticeToDelete = null
                    }
                }
            },
            onDismiss = { noticeToDelete = null }
        )
    }
}
