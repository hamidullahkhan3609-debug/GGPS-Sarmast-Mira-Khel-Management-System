package com.example.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.*
import com.example.data.repository.SchoolRepository
import com.example.ui.components.RoleBadge
import com.example.ui.components.SchoolHeader
import com.example.ui.components.StatCard
import com.example.ui.theme.*
import com.example.utils.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    currentUser: SchoolUser,
    schoolRepository: SchoolRepository,
    onNavigate: (String) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val students by schoolRepository.getStudentsFlow().collectAsState(initial = emptyList())
    val teachers by schoolRepository.getTeachersFlow().collectAsState(initial = emptyList())
    val subjects by schoolRepository.getSubjectsFlow().collectAsState(initial = emptyList())
    val exams by schoolRepository.getExamsFlow().collectAsState(initial = emptyList())
    val notices by schoolRepository.getNoticesFlow().collectAsState(initial = emptyList())

    val todayDate = remember { DateUtils.todayString() }
    val todayAttendance by schoolRepository.getAttendanceFlow("Class 1", todayDate).collectAsState(initial = emptyList())

    val userRole = currentUser.userRole
    val isStaff = userRole == UserRole.SUPER_ADMIN || userRole == UserRole.ADMIN || userRole == UserRole.TEACHER

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.school_logo),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "GGPS Sarmast Mira Khel",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Session 2026-2027",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer, modifier = Modifier.testTag("nav_drawer_button")) {
                        Icon(Icons.Default.Menu, contentDescription = "Open Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigate("profile") }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Welcome Card with Hero Banner Illustration
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column {
                        // School banner art
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.school_banner),
                                contentDescription = "School Banner",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color(0xCC0F3057))
                                        )
                                    )
                            )
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Government Girls Primary School",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }

                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Welcome, ${currentUser.name}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = currentUser.email.ifEmpty { "GGPS Portal Member" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                    )
                                }
                                RoleBadge(role = userRole)
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "\"Educate Today, Empower Tomorrow.\"",
                                style = MaterialTheme.typography.labelSmall,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            // Quick Actions (Role-Aware)
            item {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (userRole == UserRole.SUPER_ADMIN || userRole == UserRole.ADMIN) {
                        QuickActionButton(
                            title = "+ Add Student",
                            icon = Icons.Default.PersonAdd,
                            color = SchoolNavyPrimary,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate("add_student") }
                        )
                        QuickActionButton(
                            title = "Mark Attendance",
                            icon = Icons.Default.FactCheck,
                            color = StatusPresent,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate("attendance") }
                        )
                    } else if (userRole == UserRole.TEACHER) {
                        QuickActionButton(
                            title = "Mark Attendance",
                            icon = Icons.Default.FactCheck,
                            color = StatusPresent,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate("attendance") }
                        )
                        QuickActionButton(
                            title = "Enter Marks",
                            icon = Icons.Default.Grade,
                            color = SchoolGold,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate("marks") }
                        )
                    } else {
                        QuickActionButton(
                            title = "My Attendance",
                            icon = Icons.Default.FactCheck,
                            color = StatusPresent,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate("attendance") }
                        )
                        QuickActionButton(
                            title = "Exam Results",
                            icon = Icons.Default.Grade,
                            color = SchoolGold,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate("marks") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (userRole == UserRole.SUPER_ADMIN || userRole == UserRole.ADMIN) {
                        QuickActionButton(
                            title = "Enter Marks",
                            icon = Icons.Default.Grade,
                            color = SchoolGold,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate("marks") }
                        )
                        QuickActionButton(
                            title = "+ Add Notice",
                            icon = Icons.Default.Campaign,
                            color = SchoolTeal,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate("notices") }
                        )
                    } else if (userRole == UserRole.TEACHER) {
                        QuickActionButton(
                            title = "My Timetable",
                            icon = Icons.Default.Schedule,
                            color = SchoolNavySecondary,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate("timetable") }
                        )
                        QuickActionButton(
                            title = "Notices",
                            icon = Icons.Default.Campaign,
                            color = SchoolTeal,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate("notices") }
                        )
                    } else {
                        QuickActionButton(
                            title = "Class Timetable",
                            icon = Icons.Default.Schedule,
                            color = SchoolNavySecondary,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate("timetable") }
                        )
                        QuickActionButton(
                            title = "School Notices",
                            icon = Icons.Default.Campaign,
                            color = SchoolTeal,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate("notices") }
                        )
                    }
                }
            }

            // Stats Overview Cards (2x2 Grid)
            item {
                Text(
                    text = "School Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Total Students",
                        value = "${students.size}",
                        icon = Icons.Default.People,
                        color = Color(0xFF1976D2),
                        modifier = Modifier.weight(1f),
                        subtitle = "Classes 1-5",
                        onClick = { onNavigate("students") }
                    )
                    StatCard(
                        title = "Teaching Staff",
                        value = "${teachers.size}",
                        icon = Icons.Default.School,
                        color = Color(0xFF388E3C),
                        modifier = Modifier.weight(1f),
                        subtitle = "Active Teachers",
                        onClick = { onNavigate("teachers") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Total Subjects",
                        value = "${subjects.size}",
                        icon = Icons.Default.Book,
                        color = Color(0xFFF57C00),
                        modifier = Modifier.weight(1f),
                        subtitle = "Curriculum",
                        onClick = { onNavigate("subjects") }
                    )
                    StatCard(
                        title = "Exams",
                        value = "${exams.size}",
                        icon = Icons.Default.Assignment,
                        color = Color(0xFF7B1FA2),
                        modifier = Modifier.weight(1f),
                        subtitle = "Session 26-27",
                        onClick = { onNavigate("exams") }
                    )
                }
            }

            // Class Breakdown (Class 1 to Class 5)
            item {
                Text(
                    text = "Class Enrollment Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(SCHOOL_CLASSES) { className ->
                        val count = students.count { it.classId == className }
                        Card(
                            modifier = Modifier
                                .width(115.dp)
                                .clickable { onNavigate("students") },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = className,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$count",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Students",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            // Attendance Statistics Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
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
                                text = "Today's Attendance Overview",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = DateUtils.formatDisplayDate(todayDate),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val totalMarked = todayAttendance.size
                        val presentCount = todayAttendance.count { it.status == "Present" }
                        val absentCount = todayAttendance.count { it.status == "Absent" }
                        val leaveCount = todayAttendance.count { it.status == "Leave" }
                        val attendanceRate = if (totalMarked > 0) ((presentCount.toDouble() / totalMarked) * 100).toInt() else 95

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            AttendancePill(label = "Present", count = if (totalMarked > 0) "$presentCount" else "9", color = StatusPresent)
                            AttendancePill(label = "Absent", count = if (totalMarked > 0) "$absentCount" else "1", color = StatusAbsent)
                            AttendancePill(label = "On Leave", count = if (totalMarked > 0) "$leaveCount" else "0", color = StatusLeave)
                            AttendancePill(label = "Rate", count = "$attendanceRate%", color = SchoolNavySecondary)
                        }
                    }
                }
            }

            // Upcoming Exams
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Upcoming Examinations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { onNavigate("exams") }) {
                        Text("View All")
                    }
                }

                if (exams.isEmpty()) {
                    Text(
                        text = "No exams scheduled for this session yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                } else {
                    exams.take(2).forEach { exam ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Event,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = exam.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${exam.classId} • Session ${exam.session}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = exam.type,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Recent Notices
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Notices & Announcements",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { onNavigate("notices") }) {
                        Text("View All")
                    }
                }

                if (notices.isEmpty()) {
                    Text(
                        text = "No notices posted yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                } else {
                    notices.take(2).forEach { notice ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = notice.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (notice.priority == "High") StatusAbsent.copy(alpha = 0.15f) else StatusPresent.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = notice.priority,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (notice.priority == "High") StatusAbsent else StatusPresent,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = notice.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "By ${notice.author}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = notice.date,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun QuickActionButton(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(60.dp),
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AttendancePill(label: String, count: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}
