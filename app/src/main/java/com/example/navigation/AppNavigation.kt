package com.example.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.R
import com.example.data.model.UserRole
import com.example.data.repository.AuthRepository
import com.example.data.repository.SchoolRepository
import com.example.ui.attendance.DailyAttendanceScreen
import com.example.ui.attendance.MonthlyAttendanceScreen
import com.example.ui.auth.LoginScreen
import com.example.ui.components.RoleBadge
import com.example.ui.components.SchoolHeader
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.exams.ExamsScreen
import com.example.ui.marks.MarksEntryScreen
import com.example.ui.notices.NoticesScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.reports.ReportsScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.students.AddEditStudentScreen
import com.example.ui.students.StudentDetailsScreen
import com.example.ui.students.StudentsScreen
import com.example.ui.subjects.SubjectsScreen
import com.example.ui.teachers.TeachersScreen
import com.example.ui.timetable.TimetableScreen
import com.example.ui.users.UserManagementScreen
import kotlinx.coroutines.launch

data class DrawerItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val requiredRoles: List<UserRole> = emptyList() // empty means all roles can access
)

@Composable
fun AppNavigation(
    authRepository: AuthRepository,
    schoolRepository: SchoolRepository
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val currentUser by authRepository.currentUser.collectAsState()

    val drawerItems = listOf(
        DrawerItem(Screen.Dashboard.route, "Dashboard", Icons.Default.Dashboard),
        DrawerItem(Screen.Students.route, "Students Directory", Icons.Default.People),
        DrawerItem(Screen.Teachers.route, "Teachers & Staff", Icons.Default.School),
        DrawerItem(Screen.Attendance.route, "Daily Attendance", Icons.Default.FactCheck),
        DrawerItem(Screen.MonthlyAttendance.route, "Monthly Attendance", Icons.Default.CalendarMonth),
        DrawerItem(Screen.Exams.route, "Examinations", Icons.Default.Assignment),
        DrawerItem(Screen.Marks.route, "Marks & Grades", Icons.Default.Grade),
        DrawerItem(Screen.Timetable.route, "Class Timetable", Icons.Default.Schedule),
        DrawerItem(Screen.Subjects.route, "Curriculum & Subjects", Icons.Default.Book),
        DrawerItem(Screen.Notices.route, "Notices & Circulars", Icons.Default.Campaign),
        DrawerItem(Screen.Reports.route, "Official Reports", Icons.Default.Assessment),
        DrawerItem(
            Screen.Users.route,
            "User Management",
            Icons.Default.AdminPanelSettings,
            requiredRoles = listOf(UserRole.SUPER_ADMIN, UserRole.ADMIN)
        ),
        DrawerItem(Screen.Settings.route, "School Settings", Icons.Default.Settings),
        DrawerItem(Screen.Profile.route, "My Profile", Icons.Default.Person)
    )

    if (currentUser == null) {
        LoginScreen(
            authRepository = authRepository,
            onLoginSuccess = {
                // Successful login updates the currentUser StateFlow in AuthRepository,
                // which automatically triggers recomposition to the authenticated NavHost & Dashboard.
            }
        )
    } else {
        val user = currentUser!!
        val currentBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = currentBackStackEntry?.destination?.route

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(310.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        // School Header in Drawer
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.school_logo),
                                contentDescription = "Logo",
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "GGPS Sarmast Mira Khel",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Bannu, KP",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // User profile card in drawer
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = user.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = user.email.ifEmpty { "School Account" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                RoleBadge(role = user.userRole)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(10.dp))

                        // Navigation Items
                        drawerItems.forEach { item ->
                            val canShow = item.requiredRoles.isEmpty() || item.requiredRoles.contains(user.userRole)
                            if (canShow) {
                                NavigationDrawerItem(
                                    icon = { Icon(item.icon, contentDescription = null) },
                                    label = { Text(item.title, fontWeight = FontWeight.Medium) },
                                    selected = currentRoute == item.route,
                                    onClick = {
                                        coroutineScope.launch { drawerState.close() }
                                        if (currentRoute != item.route) {
                                            navController.navigate(item.route) {
                                                popUpTo(Screen.Dashboard.route) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(10.dp))

                        // Sign Out Item
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            label = { Text("Sign Out", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) },
                            selected = false,
                            onClick = {
                                coroutineScope.launch {
                                    drawerState.close()
                                    authRepository.signOut()
                                }
                            },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route
            ) {
                composable(Screen.Dashboard.route) {
                    DashboardScreen(
                        currentUser = user,
                        schoolRepository = schoolRepository,
                        onNavigate = { route -> navController.navigate(route) },
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                    )
                }

                composable(Screen.Students.route) {
                    StudentsScreen(
                        currentUser = user,
                        schoolRepository = schoolRepository,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToAddStudent = { navController.navigate(Screen.AddStudent.route) },
                        onNavigateToStudentDetails = { id -> navController.navigate(Screen.StudentDetails.createRoute(id)) }
                    )
                }

                composable(
                    route = Screen.StudentDetails.route,
                    arguments = listOf(navArgument("studentId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val studentId = backStackEntry.arguments?.getString("studentId") ?: ""
                    StudentDetailsScreen(
                        studentId = studentId,
                        currentUser = user,
                        schoolRepository = schoolRepository,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToEditStudent = { id -> navController.navigate(Screen.EditStudent.createRoute(id)) }
                    )
                }

                composable(Screen.AddStudent.route) {
                    AddEditStudentScreen(
                        studentId = null,
                        schoolRepository = schoolRepository,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.EditStudent.route,
                    arguments = listOf(navArgument("studentId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val studentId = backStackEntry.arguments?.getString("studentId")
                    AddEditStudentScreen(
                        studentId = studentId,
                        schoolRepository = schoolRepository,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Teachers.route) {
                    TeachersScreen(
                        currentUser = user,
                        schoolRepository = schoolRepository,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Subjects.route) {
                    SubjectsScreen(
                        currentUser = user,
                        schoolRepository = schoolRepository,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Exams.route) {
                    ExamsScreen(
                        currentUser = user,
                        schoolRepository = schoolRepository,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToMarks = { examId -> navController.navigate(Screen.Marks.route) }
                    )
                }

                composable(Screen.Marks.route) {
                    MarksEntryScreen(
                        initialExamId = null,
                        currentUser = user,
                        schoolRepository = schoolRepository,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Attendance.route) {
                    DailyAttendanceScreen(
                        currentUser = user,
                        schoolRepository = schoolRepository,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToMonthly = { navController.navigate(Screen.MonthlyAttendance.route) }
                    )
                }

                composable(Screen.MonthlyAttendance.route) {
                    MonthlyAttendanceScreen(
                        schoolRepository = schoolRepository,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Timetable.route) {
                    TimetableScreen(
                        currentUser = user,
                        schoolRepository = schoolRepository,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Notices.route) {
                    NoticesScreen(
                        currentUser = user,
                        schoolRepository = schoolRepository,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Reports.route) {
                    ReportsScreen(
                        currentUser = user,
                        schoolRepository = schoolRepository,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Users.route) {
                    UserManagementScreen(
                        currentUser = user,
                        schoolRepository = schoolRepository,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Profile.route) {
                    ProfileScreen(
                        currentUser = user,
                        authRepository = authRepository,
                        onNavigateBack = { navController.popBackStack() },
                        onSignOut = {
                            // Sign out clears currentUser, automatically returning to the LoginScreen
                        }
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        currentUser = user,
                        schoolRepository = schoolRepository,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
