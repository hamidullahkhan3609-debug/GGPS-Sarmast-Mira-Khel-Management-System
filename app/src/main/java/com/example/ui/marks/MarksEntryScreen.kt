package com.example.ui.marks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import com.example.data.repository.SchoolRepository
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.SchoolNavyPrimary
import com.example.ui.theme.StatusAbsent
import com.example.ui.theme.StatusPresent
import com.example.utils.ResultCalculator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarksEntryScreen(
    initialExamId: String?,
    currentUser: SchoolUser,
    schoolRepository: SchoolRepository,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    val exams by schoolRepository.getExamsFlow().collectAsState(initial = emptyList())
    val subjects by schoolRepository.getSubjectsFlow().collectAsState(initial = emptyList())

    var selectedExamId by remember { mutableStateOf(initialExamId ?: "") }
    var selectedClass by remember { mutableStateOf(SCHOOL_CLASSES.first()) }
    var selectedSubjectId by remember { mutableStateOf("") }

    val studentsInClass by schoolRepository.getStudentsFlow(selectedClass).collectAsState(initial = emptyList())

    // When exams load, select first if not set
    LaunchedEffect(exams) {
        if (selectedExamId.isEmpty() && exams.isNotEmpty()) {
            selectedExamId = exams.first().examId
        }
    }

    // When subjects load, select first if not set
    LaunchedEffect(subjects) {
        if (selectedSubjectId.isEmpty() && subjects.isNotEmpty()) {
            selectedSubjectId = subjects.first().subjectId
        }
    }

    // State for marks input per studentId: obtainedMarks, totalMarks, remarks
    val existingMarks by schoolRepository.getMarksFlow(
        examId = selectedExamId,
        classId = selectedClass,
        subjectId = selectedSubjectId
    ).collectAsState(initial = emptyList())

    var marksMap by remember { mutableStateOf<Map<String, Pair<String, String>>>(emptyMap()) }
    var isSaving by remember { mutableStateOf(false) }
    var saveFeedback by remember { mutableStateOf<String?>(null) }

    // Sync loaded existing marks into editable map
    LaunchedEffect(existingMarks, studentsInClass) {
        val map = mutableMapOf<String, Pair<String, String>>()
        for (st in studentsInClass) {
            val mark = existingMarks.find { it.studentId == st.studentId }
            val obtained = mark?.obtainedMarks?.toString() ?: ""
            val total = mark?.totalMarks?.toString() ?: "100"
            map[st.studentId] = Pair(obtained, total)
        }
        marksMap = map
    }

    val currentExam = exams.find { it.examId == selectedExamId }
    val currentSubject = subjects.find { it.subjectId == selectedSubjectId }

    val canEdit = currentUser.userRole == UserRole.SUPER_ADMIN ||
                  currentUser.userRole == UserRole.ADMIN ||
                  currentUser.userRole == UserRole.TEACHER

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Student Marks & Grades Entry", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (canEdit && studentsInClass.isNotEmpty()) {
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
                        if (saveFeedback != null) {
                            Text(
                                text = saveFeedback ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusPresent,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = "${studentsInClass.size} Students in $selectedClass",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }

                        Button(
                            onClick = {
                                isSaving = true
                                saveFeedback = null
                                coroutineScope.launch {
                                    val records = studentsInClass.mapNotNull { st ->
                                        val pair = marksMap[st.studentId] ?: Pair("0", "100")
                                        val obtained = pair.first.toDoubleOrNull() ?: return@mapNotNull null
                                        val total = pair.second.toDoubleOrNull() ?: 100.0
                                        val pct = ResultCalculator.calculatePercentage(obtained, total)
                                        val grade = ResultCalculator.calculateGrade(pct)

                                        MarkRecord(
                                            markId = "${selectedExamId}_${st.studentId}_${selectedSubjectId}",
                                            examId = selectedExamId,
                                            studentId = st.studentId,
                                            studentName = st.name,
                                            admissionNumber = st.admissionNumber,
                                            classId = selectedClass,
                                            subjectId = selectedSubjectId,
                                            subjectName = currentSubject?.name ?: "Subject",
                                            obtainedMarks = obtained,
                                            totalMarks = total,
                                            percentage = pct,
                                            grade = grade,
                                            remarks = if (ResultCalculator.isPassed(grade)) "Passed" else "Needs Improvement",
                                            markedBy = currentUser.name
                                        )
                                    }

                                    schoolRepository.saveBatchMarks(records)
                                    isSaving = false
                                    saveFeedback = "Marks saved successfully to school database!"
                                }
                            },
                            enabled = !isSaving,
                            modifier = Modifier.testTag("save_marks_button")
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save All Marks")
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
            // Dropdown Selectors Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Exam Dropdown
                    var examMenuExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = examMenuExpanded,
                        onExpandedChange = { examMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = currentExam?.name ?: "Select Exam",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Examination") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = examMenuExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = examMenuExpanded,
                            onDismissRequest = { examMenuExpanded = false }
                        ) {
                            exams.forEach { ex ->
                                DropdownMenuItem(
                                    text = { Text("${ex.name} (${ex.type})") },
                                    onClick = {
                                        selectedExamId = ex.examId
                                        examMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

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

                        // Subject Dropdown
                        var subjectMenuExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = subjectMenuExpanded,
                            onExpandedChange = { subjectMenuExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = currentSubject?.name ?: "Select Subject",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Subject") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectMenuExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                singleLine = true
                            )
                            ExposedDropdownMenu(
                                expanded = subjectMenuExpanded,
                                onDismissRequest = { subjectMenuExpanded = false }
                            ) {
                                subjects.forEach { sub ->
                                    DropdownMenuItem(
                                        text = { Text(sub.name) },
                                        onClick = {
                                            selectedSubjectId = sub.subjectId
                                            subjectMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (studentsInClass.isEmpty()) {
                EmptyStateView(
                    message = "No students enrolled in $selectedClass.",
                    icon = Icons.Default.People
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(studentsInClass, key = { it.studentId }) { student ->
                        val pair = marksMap[student.studentId] ?: Pair("", "100")
                        val obtainedVal = pair.first.toDoubleOrNull() ?: 0.0
                        val totalVal = pair.second.toDoubleOrNull() ?: 100.0
                        val pct = if (pair.first.isNotEmpty()) ResultCalculator.calculatePercentage(obtainedVal, totalVal) else null
                        val grade = pct?.let { ResultCalculator.calculateGrade(it) }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1.3f)) {
                                    Text(
                                        text = student.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = student.admissionNumber,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                OutlinedTextField(
                                    value = pair.first,
                                    onValueChange = { newVal ->
                                        if (newVal.isEmpty() || newVal.toDoubleOrNull() != null) {
                                            marksMap = marksMap + (student.studentId to Pair(newVal, pair.second))
                                            saveFeedback = null
                                        }
                                    },
                                    label = { Text("Marks") },
                                    placeholder = { Text("0") },
                                    modifier = Modifier.width(85.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    enabled = canEdit
                                )

                                Text(
                                    text = "/ 100",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(horizontal = 6.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )

                                if (grade != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (grade == "F") StatusAbsent.copy(alpha = 0.15f) else StatusPresent.copy(alpha = 0.15f),
                                        modifier = Modifier.width(44.dp)
                                    ) {
                                        Text(
                                            text = grade,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (grade == "F") StatusAbsent else StatusPresent,
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.width(44.dp))
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
