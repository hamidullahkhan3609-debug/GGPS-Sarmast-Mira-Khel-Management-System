package com.example.ui.students

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.SCHOOL_CLASSES
import com.example.data.model.Student
import com.example.data.repository.SchoolRepository
import com.example.utils.DateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditStudentScreen(
    studentId: String?,
    schoolRepository: SchoolRepository,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val isEdit = studentId != null && studentId.isNotEmpty()

    var name by remember { mutableStateOf("") }
    var fatherName by remember { mutableStateOf("") }
    var admissionNumber by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf(SCHOOL_CLASSES.first()) }
    var classMenuExpanded by remember { mutableStateOf(false) }
    var dob by remember { mutableStateOf("2019-01-01") }
    var phone by remember { mutableStateOf("0300-1234567") }
    var admissionDate by remember { mutableStateOf(DateUtils.todayString()) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successSnackbar by remember { mutableStateOf(false) }

    LaunchedEffect(studentId) {
        if (isEdit && studentId != null) {
            val student = schoolRepository.getStudent(studentId)
            if (student != null) {
                name = student.name
                fatherName = student.fatherName
                admissionNumber = student.admissionNumber
                selectedClass = if (SCHOOL_CLASSES.contains(student.classId)) student.classId else SCHOOL_CLASSES.first()
                dob = student.dob
                phone = student.phone
                admissionDate = student.admissionDate
            }
        } else {
            // Generate recommended admission number
            admissionNumber = "GGPS-2026-${(100..999).random()}"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit Student Profile" else "New Student Admission", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            Text(
                text = "Primary School Enrollment Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "All fields are preserved in the permanent school register",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Student Full Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; errorMessage = null },
                label = { Text("Student Name *") },
                placeholder = { Text("e.g. Fatima Khan") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("student_name_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Father Name
            OutlinedTextField(
                value = fatherName,
                onValueChange = { fatherName = it; errorMessage = null },
                label = { Text("Father's / Guardian's Name *") },
                placeholder = { Text("e.g. Muhammad Tariq Khan") },
                leadingIcon = { Icon(Icons.Default.SupervisorAccount, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("father_name_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Admission Number
            OutlinedTextField(
                value = admissionNumber,
                onValueChange = { admissionNumber = it; errorMessage = null },
                label = { Text("Admission Number *") },
                placeholder = { Text("e.g. GGPS-2026-042") },
                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admission_number_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Class Selection Dropdown (Class 1 to Class 5)
            ExposedDropdownMenuBox(
                expanded = classMenuExpanded,
                onExpandedChange = { classMenuExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedClass,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Class Selection * (Class 1 - 5)") },
                    leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classMenuExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = classMenuExpanded,
                    onDismissRequest = { classMenuExpanded = false }
                ) {
                    SCHOOL_CLASSES.forEach { className ->
                        DropdownMenuItem(
                            text = { Text(className, fontWeight = FontWeight.SemiBold) },
                            onClick = {
                                selectedClass = className
                                classMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Date of Birth
            OutlinedTextField(
                value = dob,
                onValueChange = { dob = it },
                label = { Text("Date of Birth (YYYY-MM-DD)") },
                placeholder = { Text("2019-04-12") },
                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Parent Contact Phone
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Guardian Phone Number") },
                placeholder = { Text("0333-1234567") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Admission Date
            OutlinedTextField(
                value = admissionDate,
                onValueChange = { admissionDate = it },
                label = { Text("Admission Date") },
                leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Save Button
            Button(
                onClick = {
                    if (name.trim().isEmpty()) {
                        errorMessage = "Student Name cannot be empty."
                        return@Button
                    }
                    if (fatherName.trim().isEmpty()) {
                        errorMessage = "Father's Name cannot be empty."
                        return@Button
                    }
                    if (admissionNumber.trim().isEmpty()) {
                        errorMessage = "Admission Number is required."
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null

                    coroutineScope.launch {
                        val student = Student(
                            studentId = studentId ?: "",
                            name = name.trim(),
                            fatherName = fatherName.trim(),
                            admissionNumber = admissionNumber.trim().uppercase(),
                            classId = selectedClass,
                            className = selectedClass,
                            dob = dob.trim(),
                            phone = phone.trim(),
                            admissionDate = admissionDate.trim(),
                            status = "active"
                        )

                        val result = schoolRepository.saveStudent(student)
                        isLoading = false
                        result.onSuccess {
                            onNavigateBack()
                        }.onFailure {
                            errorMessage = it.localizedMessage ?: "Failed to save student record."
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_student_button"),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isEdit) "Update Student Record" else "Save Student Record", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
