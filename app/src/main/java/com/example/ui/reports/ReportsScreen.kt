package com.example.ui.reports

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.SCHOOL_CLASSES
import com.example.data.model.SchoolUser
import com.example.data.repository.SchoolRepository
import com.example.ui.components.SchoolLogoImage
import com.example.ui.theme.SchoolNavyPrimary
import com.example.utils.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    currentUser: SchoolUser,
    schoolRepository: SchoolRepository,
    onNavigateBack: () -> Unit
) {
    val reportTypes = listOf(
        "Student Enrollment List",
        "Staff Directory Register",
        "Class 1-5 Summary"
    )

    var selectedReport by remember { mutableStateOf(reportTypes.first()) }
    var selectedClass by remember { mutableStateOf("All Classes") }

    val students by schoolRepository.getStudentsFlow(
        if (selectedClass == "All Classes") null else selectedClass
    ).collectAsState(initial = emptyList())

    val teachers by schoolRepository.getTeachersFlow().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Official School Reports", fontWeight = FontWeight.Bold) },
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
                .padding(horizontal = 16.dp)
        ) {
            // Select Report type chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                reportTypes.forEach { report ->
                    FilterChip(
                        selected = selectedReport == report,
                        onClick = { selectedReport = report },
                        label = { Text(report) }
                    )
                }
            }

            if (selectedReport == "Student Enrollment List") {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val filterClasses = listOf("All Classes") + SCHOOL_CLASSES
                    filterClasses.forEach { cls ->
                        FilterChip(
                            selected = selectedClass == cls,
                            onClick = { selectedClass = cls },
                            label = { Text(cls) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Printable Document Container
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Official Letterhead
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            SchoolLogoImage(size = 64)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "GOVERNMENT GIRLS PRIMARY SCHOOL",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = SchoolNavyPrimary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "SARMAST MIRA KHEL, BANNU, KP",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "\"Educate Today, Empower Tomorrow.\"",
                                style = MaterialTheme.typography.labelSmall,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Head Mistress: Miss Sheeba Khan • Academic Session 2026-2027",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "$selectedReport ($selectedClass)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Generated Date: ${DateUtils.formatDisplayDate(DateUtils.todayString())}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                    if (selectedReport == "Student Enrollment List") {
                        item {
                            // Table Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                                    .padding(vertical = 8.dp, horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("#", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp))
                                Text("Student Name", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                                Text("Father Name", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                                Text("Class", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(60.dp))
                                Text("Adm #", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp))
                            }
                        }

                        itemsIndexed(students, key = { _, s -> s.studentId }) { index, student ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp, horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${index + 1}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(28.dp))
                                Text(student.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.5f))
                                Text(student.fatherName, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1.5f))
                                Text(student.classId, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(60.dp))
                                Text(student.admissionNumber, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(80.dp))
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        }

                        item {
                            Text(
                                text = "Total Students Enrolled: ${students.size}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                        }
                    } else if (selectedReport == "Staff Directory Register") {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                                    .padding(vertical = 8.dp, horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("#", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp))
                                Text("Teacher Name", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                                Text("Designation", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                                Text("Phone", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                            }
                        }

                        itemsIndexed(teachers, key = { _, t -> t.teacherId }) { index, teacher ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp, horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${index + 1}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(28.dp))
                                Text(teacher.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.5f))
                                Text(teacher.designation, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1.5f))
                                Text(teacher.phone, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1.2f))
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        }

                        item {
                            Text(
                                text = "Total Active Staff: ${teachers.size}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                        }
                    } else {
                        // Class 1-5 Summary
                        itemsIndexed(SCHOOL_CLASSES) { _, cls ->
                            val count = students.count { it.classId == cls }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(cls, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text("Primary Level", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text("$count Students", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = SchoolNavyPrimary)
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("___________________", style = MaterialTheme.typography.bodySmall)
                                Text("Exam Incharge", style = MaterialTheme.typography.labelSmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Miss Sheeba Khan", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text("Head Mistress (GGPS)", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
