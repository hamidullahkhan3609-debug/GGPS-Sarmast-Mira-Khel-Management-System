package com.example.ui.users

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
import com.example.data.model.SchoolUser
import com.example.data.model.UserRole
import com.example.data.repository.SchoolRepository
import com.example.ui.components.EmptyStateView
import com.example.ui.components.RoleBadge
import com.example.ui.theme.SchoolNavyPrimary
import com.example.ui.theme.StatusAbsent
import com.example.ui.theme.StatusPresent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    currentUser: SchoolUser,
    schoolRepository: SchoolRepository,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }

    val usersList by schoolRepository.getUsersFlow().collectAsState(initial = emptyList())
    val isSuperAdmin = currentUser.userRole == UserRole.SUPER_ADMIN

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User & Role Management", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (isSuperAdmin) {
                FloatingActionButton(
                    onClick = { showDialog = true },
                    containerColor = SchoolNavyPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_user_fab")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add User")
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
                text = "Manage staff, teacher, student and parent school portal access",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            if (!isSuperAdmin) {
                EmptyStateView(
                    message = "Access restricted. Only the Super Admin (Head Mistress) can manage school accounts.",
                    icon = Icons.Default.Security
                )
            } else if (usersList.isEmpty()) {
                EmptyStateView(
                    message = "No school portal accounts found.",
                    icon = Icons.Default.People
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(usersList, key = { it.id }) { user ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
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
                                            text = user.name.take(1).uppercase(),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = user.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = user.email.ifEmpty { "No email registered" },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }

                                    RoleBadge(role = user.userRole)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (user.status == "active") StatusPresent.copy(alpha = 0.15f) else StatusAbsent.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = user.status.uppercase(),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (user.status == "active") StatusPresent else StatusAbsent
                                        )
                                    }

                                    Row {
                                        // Change Role
                                        var roleMenuExpanded by remember { mutableStateOf(false) }
                                        Box {
                                            TextButton(onClick = { roleMenuExpanded = true }) {
                                                Text("Change Role")
                                            }
                                            DropdownMenu(
                                                expanded = roleMenuExpanded,
                                                onDismissRequest = { roleMenuExpanded = false }
                                            ) {
                                                UserRole.values().forEach { r ->
                                                    DropdownMenuItem(
                                                        text = { Text(r.displayName) },
                                                        onClick = {
                                                            roleMenuExpanded = false
                                                            coroutineScope.launch {
                                                                schoolRepository.saveUser(user.copy(role = r.name))
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        // Toggle status
                                        TextButton(
                                            onClick = {
                                                val nextStatus = if (user.status == "active") "disabled" else "active"
                                                coroutineScope.launch {
                                                    schoolRepository.updateUserStatus(user.id, nextStatus)
                                                }
                                            }
                                        ) {
                                            Text(if (user.status == "active") "Disable" else "Enable")
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
        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var role by remember { mutableStateOf(UserRole.TEACHER) }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Create School Portal Account", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Select Role:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    var roleExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = roleExpanded,
                        onExpandedChange = { roleExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = role.displayName,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = roleExpanded,
                            onDismissRequest = { roleExpanded = false }
                        ) {
                            UserRole.values().forEach { r ->
                                DropdownMenuItem(
                                    text = { Text(r.displayName) },
                                    onClick = {
                                        role = r
                                        roleExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && email.isNotBlank()) {
                            coroutineScope.launch {
                                val newUser = SchoolUser(
                                    id = "user_" + System.currentTimeMillis(),
                                    name = name.trim(),
                                    email = email.trim(),
                                    role = role.name,
                                    status = "active"
                                )
                                schoolRepository.saveUser(newUser)
                                showDialog = false
                            }
                        }
                    }
                ) {
                    Text("Create Account")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
