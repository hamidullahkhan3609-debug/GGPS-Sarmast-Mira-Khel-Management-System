package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.*
import com.example.utils.DateUtils
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Locale

class SchoolRepository(private val context: Context) {

    private val firestore: FirebaseFirestore by lazy {
        try {
            val dbId = context.getString(R.string.firestore_database_id)
            if (dbId.isNotEmpty() && dbId != "default") {
                FirebaseFirestore.getInstance(FirebaseApp.getInstance(), dbId)
            } else {
                FirebaseFirestore.getInstance()
            }
        } catch (_: Exception) {
            FirebaseFirestore.getInstance()
        }.apply {
            try {
                firestoreSettings = FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build()
            } catch (e: Exception) {
                Log.w("SchoolRepository", "Persistence already initialized: ${e.message}")
            }
        }
    }

    // Collections
    private val usersRef get() = firestore.collection("users")
    private val studentsRef get() = firestore.collection("students")
    private val teachersRef get() = firestore.collection("teachers")
    private val subjectsRef get() = firestore.collection("subjects")
    private val examsRef get() = firestore.collection("exams")
    private val marksRef get() = firestore.collection("marks")
    private val attendanceRef get() = firestore.collection("attendance")
    private val timetablesRef get() = firestore.collection("timetables")
    private val noticesRef get() = firestore.collection("notices")
    private val settingsRef get() = firestore.collection("settings")

    // Users
    fun getUsersFlow(): Flow<List<SchoolUser>> = callbackFlow {
        val listener = usersRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(SchoolUser::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun getUser(id: String): SchoolUser? {
        return try {
            val doc = usersRef.document(id).get().await()
            doc.toObject(SchoolUser::class.java)?.copy(id = doc.id)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getUserByEmail(email: String): SchoolUser? {
        return try {
            val query = usersRef.whereEqualTo("email", email.trim().lowercase(Locale.ROOT)).get().await()
            query.documents.firstOrNull()?.let { doc ->
                doc.toObject(SchoolUser::class.java)?.copy(id = doc.id)
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveUser(user: SchoolUser): Result<Unit> {
        return try {
            val id = if (user.id.isNotEmpty()) user.id else usersRef.document().id
            val toSave = user.copy(id = id, email = user.email.trim().lowercase(Locale.ROOT))
            usersRef.document(id).set(toSave, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserStatus(userId: String, status: String): Result<Unit> {
        return try {
            usersRef.document(userId).update("status", status).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Students
    fun getStudentsFlow(classId: String? = null): Flow<List<Student>> = callbackFlow {
        val query = if (classId.isNullOrEmpty() || classId == "All Classes") {
            studentsRef
        } else {
            studentsRef.whereEqualTo("classId", classId)
        }
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Student::class.java)?.copy(studentId = doc.id)
            }?.filter { it.status != "archived" } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun getStudent(id: String): Student? {
        return try {
            studentsRef.document(id).get().await().toObject(Student::class.java)?.copy(studentId = id)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveStudent(student: Student): Result<Unit> {
        return try {
            val id = if (student.studentId.isNotEmpty()) student.studentId else studentsRef.document().id
            val toSave = student.copy(studentId = id, updatedAt = System.currentTimeMillis())
            studentsRef.document(id).set(toSave, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteStudent(id: String, softDelete: Boolean = true): Result<Unit> {
        return try {
            if (softDelete) {
                studentsRef.document(id).update("status", "archived", "updatedAt", System.currentTimeMillis()).await()
            } else {
                studentsRef.document(id).delete().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Teachers
    fun getTeachersFlow(): Flow<List<Teacher>> = callbackFlow {
        val listener = teachersRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Teacher::class.java)?.copy(teacherId = doc.id)
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun saveTeacher(teacher: Teacher): Result<Unit> {
        return try {
            val id = if (teacher.teacherId.isNotEmpty()) teacher.teacherId else teachersRef.document().id
            val toSave = teacher.copy(teacherId = id, updatedAt = System.currentTimeMillis())
            teachersRef.document(id).set(toSave, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTeacher(id: String): Result<Unit> {
        return try {
            teachersRef.document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Subjects
    fun getSubjectsFlow(): Flow<List<Subject>> = callbackFlow {
        val listener = subjectsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Subject::class.java)?.copy(subjectId = doc.id)
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun saveSubject(subject: Subject): Result<Unit> {
        return try {
            val id = if (subject.subjectId.isNotEmpty()) subject.subjectId else subjectsRef.document().id
            val toSave = subject.copy(subjectId = id)
            subjectsRef.document(id).set(toSave, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSubject(id: String): Result<Unit> {
        return try {
            subjectsRef.document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Exams
    fun getExamsFlow(): Flow<List<Exam>> = callbackFlow {
        val listener = examsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Exam::class.java)?.copy(examId = doc.id)
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun saveExam(exam: Exam): Result<Unit> {
        return try {
            val id = if (exam.examId.isNotEmpty()) exam.examId else examsRef.document().id
            val toSave = exam.copy(examId = id)
            examsRef.document(id).set(toSave, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteExam(id: String): Result<Unit> {
        return try {
            examsRef.document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Marks
    fun getMarksFlow(examId: String, classId: String, subjectId: String): Flow<List<MarkRecord>> = callbackFlow {
        val query = marksRef
            .whereEqualTo("examId", examId)
            .whereEqualTo("classId", classId)
            .whereEqualTo("subjectId", subjectId)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(MarkRecord::class.java)?.copy(markId = doc.id)
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    fun getStudentMarksFlow(studentId: String): Flow<List<MarkRecord>> = callbackFlow {
        val query = marksRef.whereEqualTo("studentId", studentId)
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(MarkRecord::class.java)?.copy(markId = doc.id)
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun saveMark(mark: MarkRecord): Result<Unit> {
        return try {
            val docId = if (mark.markId.isNotEmpty()) {
                mark.markId
            } else {
                "${mark.examId}_${mark.studentId}_${mark.subjectId}"
            }
            val toSave = mark.copy(markId = docId, updatedAt = System.currentTimeMillis())
            marksRef.document(docId).set(toSave, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveBatchMarks(marksList: List<MarkRecord>): Result<Unit> {
        return try {
            val batch = firestore.batch()
            for (mark in marksList) {
                val docId = if (mark.markId.isNotEmpty()) mark.markId else "${mark.examId}_${mark.studentId}_${mark.subjectId}"
                val docRef = marksRef.document(docId)
                batch.set(docRef, mark.copy(markId = docId, updatedAt = System.currentTimeMillis()), SetOptions.merge())
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Attendance
    fun getAttendanceFlow(classId: String, date: String): Flow<List<AttendanceRecord>> = callbackFlow {
        val query = attendanceRef
            .whereEqualTo("classId", classId)
            .whereEqualTo("date", date)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(AttendanceRecord::class.java)?.copy(attendanceId = doc.id)
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    fun getStudentAttendanceFlow(studentId: String): Flow<List<AttendanceRecord>> = callbackFlow {
        val query = attendanceRef.whereEqualTo("studentId", studentId)
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(AttendanceRecord::class.java)?.copy(attendanceId = doc.id)
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun getMonthlyAttendanceRecords(classId: String, yearMonthPrefix: String): List<AttendanceRecord> {
        return try {
            val snapshot = attendanceRef
                .whereEqualTo("classId", classId)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(AttendanceRecord::class.java)?.copy(attendanceId = doc.id)
            }.filter { it.date.startsWith(yearMonthPrefix) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun saveAttendanceBatch(records: List<AttendanceRecord>): Result<Unit> {
        return try {
            val batch = firestore.batch()
            for (rec in records) {
                val docId = if (rec.attendanceId.isNotEmpty()) {
                    rec.attendanceId
                } else {
                    "${rec.classId}_${rec.date}_${rec.studentId}"
                }
                val docRef = attendanceRef.document(docId)
                batch.set(docRef, rec.copy(attendanceId = docId, timestamp = System.currentTimeMillis()), SetOptions.merge())
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Timetable
    fun getTimetableFlow(classId: String): Flow<List<TimetableEntry>> = callbackFlow {
        val query = timetablesRef.whereEqualTo("classId", classId)
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(TimetableEntry::class.java)?.copy(timetableId = doc.id)
            }?.sortedWith(compareBy({ it.day }, { it.period })) ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun saveTimetableEntry(entry: TimetableEntry): Result<Unit> {
        return try {
            val id = if (entry.timetableId.isNotEmpty()) entry.timetableId else timetablesRef.document().id
            timetablesRef.document(id).set(entry.copy(timetableId = id), SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTimetableEntry(id: String): Result<Unit> {
        return try {
            timetablesRef.document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Notices
    fun getNoticesFlow(): Flow<List<Notice>> = callbackFlow {
        val listener = noticesRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Notice::class.java)?.copy(noticeId = doc.id)
            }?.sortedByDescending { it.createdAt } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun saveNotice(notice: Notice): Result<Unit> {
        return try {
            val id = if (notice.noticeId.isNotEmpty()) notice.noticeId else noticesRef.document().id
            noticesRef.document(id).set(notice.copy(noticeId = id), SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteNotice(id: String): Result<Unit> {
        return try {
            noticesRef.document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Settings
    suspend fun getSettings(): SchoolSettings {
        return try {
            val doc = settingsRef.document("school_config").get().await()
            doc.toObject(SchoolSettings::class.java) ?: SchoolSettings()
        } catch (_: Exception) {
            SchoolSettings()
        }
    }

    suspend fun saveSettings(settings: SchoolSettings): Result<Unit> {
        return try {
            settingsRef.document("school_config").set(settings, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Initial school seed data if Firestore is empty
    suspend fun seedInitialDataIfEmpty() {
        try {
            val snap = studentsRef.limit(1).get().await()
            if (!snap.isEmpty) return // already has data

            Log.d("SchoolRepository", "Seeding initial GGPS Sarmast Mira Khel school records to Firestore...")

            // Initial Settings
            settingsRef.document("school_config").set(SchoolSettings()).await()

            // Initial Teachers
            val teachers = listOf(
                Teacher(
                    teacherId = "t_sheeba",
                    name = "Miss Sheeba Khan",
                    fatherName = "Akhtar Zaman Khan",
                    phone = "+92 300 1234567",
                    designation = "Head Mistress",
                    subjects = listOf("English", "General Science"),
                    classes = listOf("Class 4", "Class 5")
                ),
                Teacher(
                    teacherId = "t_zainab",
                    name = "Zainab Bibi",
                    fatherName = "Gul Nawaz",
                    phone = "+92 301 2345678",
                    designation = "Primary School Teacher (PST)",
                    subjects = listOf("Urdu", "Islamiat"),
                    classes = listOf("Class 1", "Class 2")
                ),
                Teacher(
                    teacherId = "t_farzana",
                    name = "Farzana Begum",
                    fatherName = "Muhammad Rafiq",
                    phone = "+92 302 3456789",
                    designation = "Primary School Teacher (PST)",
                    subjects = listOf("Mathematics", "Social Studies"),
                    classes = listOf("Class 3", "Class 4")
                )
            )
            for (t in teachers) teachersRef.document(t.teacherId).set(t).await()

            // Initial Subjects
            val subjects = listOf(
                Subject("sub_eng", "English", listOf("Class 1", "Class 2", "Class 3", "Class 4", "Class 5"), listOf("t_sheeba")),
                Subject("sub_urdu", "Urdu", listOf("Class 1", "Class 2", "Class 3", "Class 4", "Class 5"), listOf("t_zainab")),
                Subject("sub_math", "Mathematics", listOf("Class 1", "Class 2", "Class 3", "Class 4", "Class 5"), listOf("t_farzana")),
                Subject("sub_sci", "General Science", listOf("Class 3", "Class 4", "Class 5"), listOf("t_sheeba")),
                Subject("sub_isl", "Islamiat", listOf("Class 1", "Class 2", "Class 3", "Class 4", "Class 5"), listOf("t_zainab")),
                Subject("sub_soc", "Social Studies", listOf("Class 4", "Class 5"), listOf("t_farzana"))
            )
            for (s in subjects) subjectsRef.document(s.subjectId).set(s).await()

            // Initial Students for Classes 1 to 5
            val initialStudents = listOf(
                Student("s_101", "Ayesha Khan", "Mirza Tariq Khan", "GGPS-2026-001", "Class 1", "Class 1", "2020-04-12", "0333-1122334", "2026-03-01"),
                Student("s_102", "Fatima Noor", "Sher Muhammad", "GGPS-2026-002", "Class 1", "Class 1", "2020-06-19", "0333-2233445", "2026-03-01"),
                Student("s_103", "Khadija Bibi", "Abdul Qadir", "GGPS-2026-003", "Class 2", "Class 2", "2019-02-14", "0333-3344556", "2025-03-01"),
                Student("s_104", "Maryam Gul", "Zahid Ullah", "GGPS-2026-004", "Class 2", "Class 2", "2019-09-08", "0333-4455667", "2025-03-01"),
                Student("s_105", "Zainab Shah", "Syed Kamal Shah", "GGPS-2026-005", "Class 3", "Class 3", "2018-01-22", "0333-5566778", "2024-03-01"),
                Student("s_106", "Laiba Rehman", "Atta Ur Rehman", "GGPS-2026-006", "Class 3", "Class 3", "2018-08-30", "0333-6677889", "2024-03-01"),
                Student("s_107", "Hira Wazir", "Muhammad Younas", "GGPS-2026-007", "Class 4", "Class 4", "2017-05-11", "0333-7788990", "2023-03-01"),
                Student("s_108", "Sadia Mir", "Mir Ahmad Khan", "GGPS-2026-008", "Class 4", "Class 4", "2017-11-25", "0333-8899001", "2023-03-01"),
                Student("s_109", "Marwa Bannu", "Haji Noor Zaman", "GGPS-2026-009", "Class 5", "Class 5", "2016-03-15", "0333-9900112", "2022-03-01"),
                Student("s_110", "Iqra Khattak", "Said Badshah", "GGPS-2026-010", "Class 5", "Class 5", "2016-10-04", "0333-0011223", "2022-03-01")
            )
            for (st in initialStudents) studentsRef.document(st.studentId).set(st).await()

            // Initial Exam
            val exam = Exam(
                examId = "exam_mid_2026",
                name = "Mid-Term Examination 2026",
                type = "Mid-Term",
                session = "2026-2027",
                classId = "All Classes",
                startDate = "2026-10-15",
                endDate = "2026-10-25",
                status = "active",
                subjects = listOf("English", "Urdu", "Mathematics", "General Science", "Islamiat")
            )
            examsRef.document(exam.examId).set(exam).await()

            // Initial Notice
            val notice1 = Notice(
                noticeId = "not_01",
                title = "Welcome to Academic Session 2026-2027",
                description = "All respected teachers, parents, and students are welcomed to the new academic term at GGPS Sarmast Mira Khel Bannu. Daily classes start punctually at 8:00 AM.",
                date = DateUtils.todayString(),
                author = "Head Mistress Sheeba Khan",
                priority = "High",
                status = "published"
            )
            val notice2 = Notice(
                noticeId = "not_02",
                title = "Mid-Term Examination Schedule Announced",
                description = "The mid-term assessments for Classes 1 to 5 will commence from 15th October 2026. Parents are requested to ensure regular attendance and revision at home.",
                date = DateUtils.todayString(),
                author = "Examination Committee",
                priority = "Medium",
                status = "published"
            )
            noticesRef.document(notice1.noticeId).set(notice1).await()
            noticesRef.document(notice2.noticeId).set(notice2).await()

            // Initial Timetable sample
            val timetable = listOf(
                TimetableEntry("tt_1", "Class 5", "Monday", 1, "English", "Miss Sheeba Khan", "08:00 AM", "08:45 AM"),
                TimetableEntry("tt_2", "Class 5", "Monday", 2, "Urdu", "Zainab Bibi", "08:45 AM", "09:30 AM"),
                TimetableEntry("tt_3", "Class 5", "Monday", 3, "Mathematics", "Farzana Begum", "09:30 AM", "10:15 AM"),
                TimetableEntry("tt_4", "Class 5", "Monday", 4, "General Science", "Miss Sheeba Khan", "10:30 AM", "11:15 AM"),
                TimetableEntry("tt_5", "Class 5", "Monday", 5, "Islamiat", "Zainab Bibi", "11:15 AM", "12:00 PM")
            )
            for (tt in timetable) timetablesRef.document(tt.timetableId).set(tt).await()

            // Initial Users
            val adminUser = SchoolUser(
                id = "admin_super",
                name = "Miss Sheeba Khan (Head Mistress)",
                email = "admin@ggpssarmast.edu.pk",
                role = UserRole.SUPER_ADMIN.name,
                phone = "+92 300 1234567"
            )
            usersRef.document(adminUser.id).set(adminUser).await()

            Log.d("SchoolRepository", "Seeding completed successfully.")
        } catch (e: Exception) {
            Log.e("SchoolRepository", "Error seeding data: ${e.message}", e)
        }
    }
}
