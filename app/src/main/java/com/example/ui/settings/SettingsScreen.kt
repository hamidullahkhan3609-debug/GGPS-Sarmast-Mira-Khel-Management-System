package com.example.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.SchoolSettings
import com.example.data.model.SchoolUser
import com.example.data.model.UserRole
import com.example.data.repository.SchoolRepository
import com.example.ui.components.LoadingStateView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentUser: SchoolUser,
    schoolRepository: SchoolRepository,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var settings by remember { mutableStateOf<SchoolSettings?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var saveSuccess by remember { mutableStateOf(false) }

    var schoolName by remember { mutableStateOf("") }
    var shortName by remember { mutableStateOf("") }
    var headMistress by remember { mutableStateOf("") }
    var motto by remember { mutableStateOf("") }
    var session by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    val canEdit = currentUser.userRole == UserRole.SUPER_ADMIN || currentUser.userRole == UserRole.ADMIN

    LaunchedEffect(Unit) {
        val loaded = schoolRepository.getSettings()
        settings = loaded
        schoolName = loaded.schoolName
        shortName = loaded.shortName
        headMistress = loaded.headMistress
        motto = loaded.motto
        session = loaded.currentSession
        location = loaded.location
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("School Settings & Configuration", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            LoadingStateView(message = "Loading school settings...", modifier = Modifier.padding(padding))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                if (saveSuccess) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Settings saved successfully to school database.",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Institution Identity & Administration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = schoolName,
                            onValueChange = { schoolName = it; saveSuccess = false },
                            label = { Text("School Full Name") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = canEdit
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = shortName,
                            onValueChange = { shortName = it; saveSuccess = false },
                            label = { Text("Short Name") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = canEdit
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = headMistress,
                            onValueChange = { headMistress = it; saveSuccess = false },
                            label = { Text("Head Mistress") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = canEdit
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = motto,
                            onValueChange = { motto = it; saveSuccess = false },
                            label = { Text("School Motto") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = canEdit
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = session,
                            onValueChange = { session = it; saveSuccess = false },
                            label = { Text("Academic Session") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = canEdit
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it; saveSuccess = false },
                            label = { Text("School Location") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = canEdit
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Grading Scale Preview
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Primary School Grading Rules",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "A+ : 90% - 100% (Outstanding)\n" +
                                   "A  : 80% - 89.99% (Excellent)\n" +
                                   "B  : 70% - 79.99% (Very Good)\n" +
                                   "C  : 60% - 69.99% (Good)\n" +
                                   "D  : 50% - 59.99% (Satisfactory / Pass)\n" +
                                   "F  : 0% - 49.99% (Fail / Needs Improvement)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }

                if (canEdit) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            isSaving = true
                            coroutineScope.launch {
                                val updated = (settings ?: SchoolSettings()).copy(
                                    schoolName = schoolName.trim(),
                                    shortName = shortName.trim(),
                                    headMistress = headMistress.trim(),
                                    motto = motto.trim(),
                                    currentSession = session.trim(),
                                    location = location.trim()
                                )
                                schoolRepository.saveSettings(updated)
                                isSaving = false
                                saveSuccess = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_settings_button"),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save School Configuration", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
