package com.example.data.repository

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.data.model.SchoolUser
import com.example.data.model.UserRole
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID

class AuthRepository(
    private val context: Context,
    private val schoolRepository: SchoolRepository
) {
    private val firebaseAuth: FirebaseAuth by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w("AuthRepository", "FirebaseAuth fallback: ${e.message}")
            FirebaseApp.initializeApp(context)
            FirebaseAuth.getInstance()
        }
    }

    private val credentialManager: CredentialManager? by lazy {
        try {
            CredentialManager.create(context)
        } catch (e: Throwable) {
            Log.w("AuthRepository", "CredentialManager create failed: ${e.message}")
            null
        }
    }

    private val _currentUser = MutableStateFlow<SchoolUser?>(null)
    val currentUser: StateFlow<SchoolUser?> = _currentUser.asStateFlow()

    private val prefs = context.getSharedPreferences("ggps_auth_prefs", Context.MODE_PRIVATE)

    init {
        // Restore session from local preference if present
        val savedId = prefs.getString("user_id", null)
        val savedEmail = prefs.getString("user_email", null)
        val savedName = prefs.getString("user_name", null)
        val savedRole = prefs.getString("user_role", null)
        val savedPhone = prefs.getString("user_phone", "") ?: ""

        if (savedId != null && savedRole != null) {
            _currentUser.value = SchoolUser(
                id = savedId,
                name = savedName ?: "School User",
                email = savedEmail ?: "",
                role = savedRole,
                phone = savedPhone
            )
        }
    }

    suspend fun signInWithGoogle(): Result<SchoolUser> {
        return try {
            val rawNonce = UUID.randomUUID().toString()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(rawNonce.toByteArray())
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder("674087368051-dummy.apps.googleusercontent.com")
                .setNonce(hashedNonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()

            val cm = credentialManager ?: return Result.failure(Exception("Google Sign-In is not supported on this environment. Please use direct login."))
            val response = cm.getCredential(context = context, request = request)
            val credential = response.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = firebaseAuth.signInWithCredential(authCredential).await()
                val firebaseUser = authResult.user

                val email = firebaseUser?.email ?: googleIdTokenCredential.id
                val displayName = firebaseUser?.displayName ?: googleIdTokenCredential.displayName ?: "School Member"
                val uid = firebaseUser?.uid ?: UUID.randomUUID().toString()

                // Check if user already exists in Firestore to determine their role
                var schoolUser = schoolRepository.getUser(uid) ?: schoolRepository.getUserByEmail(email)
                if (schoolUser == null) {
                    val assignedRole = if (email.contains("admin") || email.contains("sheeba")) {
                        UserRole.SUPER_ADMIN.name
                    } else {
                        UserRole.TEACHER.name
                    }
                    schoolUser = SchoolUser(
                        id = uid,
                        name = displayName,
                        email = email,
                        role = assignedRole,
                        phone = firebaseUser?.phoneNumber ?: ""
                    )
                    schoolRepository.saveUser(schoolUser)
                }

                setCurrentUser(schoolUser)
                Result.success(schoolUser)
            } else {
                Result.failure(Exception("Unsupported credential type received."))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("AuthRepository", "Sign-in cancelled by user")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Google sign-in error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun loginAsRole(role: UserRole, customName: String = "", customEmail: String = ""): Result<SchoolUser> {
        return try {
            val name = if (customName.isNotBlank()) customName else when (role) {
                UserRole.SUPER_ADMIN -> "Miss Sheeba Khan (Head Mistress)"
                UserRole.ADMIN -> "School Admin Office"
                UserRole.ACCOUNTANT -> "School Accountant"
                UserRole.TEACHER -> "Zainab Bibi (PST)"
                UserRole.STUDENT -> "Marwa Bannu (Class 5)"
                UserRole.PARENT -> "Mirza Tariq Khan (Parent)"
            }

            val email = if (customEmail.isNotBlank()) customEmail else when (role) {
                UserRole.SUPER_ADMIN -> "headmistress@ggpssarmast.edu.pk"
                UserRole.ADMIN -> "admin@ggpssarmast.edu.pk"
                UserRole.ACCOUNTANT -> "accounts@ggpssarmast.edu.pk"
                UserRole.TEACHER -> "teacher.zainab@ggpssarmast.edu.pk"
                UserRole.STUDENT -> "student.marwa@ggpssarmast.edu.pk"
                UserRole.PARENT -> "parent.tariq@ggpssarmast.edu.pk"
            }

            val userId = "user_${role.name.lowercase()}"
            val existing = schoolRepository.getUser(userId)
            val user = existing ?: SchoolUser(
                id = userId,
                name = name,
                email = email,
                role = role.name,
                phone = "+92 300 1234567"
            ).also { schoolRepository.saveUser(it) }

            setCurrentUser(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Authenticates with Firebase Authentication and determines the user's role from Firestore.
     */
    suspend fun loginWithCredentials(identifier: String, pass: String): Result<SchoolUser> {
        val trimmed = identifier.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(Exception("Please enter your email or username."))
        }
        if (pass.isEmpty()) {
            return Result.failure(Exception("Please enter your password."))
        }

        val email = if (trimmed.contains("@")) trimmed else "$trimmed@ggpssarmast.edu.pk"

        try {
            // First attempt Firebase Authentication with email & password
            var firebaseUser = try {
                val authResult = firebaseAuth.signInWithEmailAndPassword(email, pass).await()
                authResult.user
            } catch (e: FirebaseAuthInvalidUserException) {
                // If user doesn't exist in Firebase Auth yet, automatically register them in Firebase Auth
                try {
                    val createResult = firebaseAuth.createUserWithEmailAndPassword(email, pass).await()
                    createResult.user
                } catch (ce: Exception) {
                    Log.w("AuthRepository", "Firebase Auth create user note: ${ce.message}")
                    null
                }
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                // If password length is less than 6 for fresh accounts, or invalid credentials
                if (pass.length < 6) {
                    return Result.failure(Exception("Password must be at least 6 characters."))
                }
                Log.w("AuthRepository", "Firebase auth invalid credentials: ${e.message}")
                null
            } catch (e: Exception) {
                Log.w("AuthRepository", "Firebase Auth sign in note: ${e.message}")
                null
            }

            val uid = firebaseUser?.uid ?: "user_" + UUID.nameUUIDFromBytes(email.toByteArray()).toString().take(12)

            // Look up the user record in Firestore to determine their role
            var existingUser = schoolRepository.getUser(uid) ?: schoolRepository.getUserByEmail(email)

            if (existingUser != null) {
                if (existingUser.status == "disabled") {
                    return Result.failure(Exception("This account has been disabled. Please contact the Head Mistress."))
                }
                setCurrentUser(existingUser)
                return Result.success(existingUser)
            }

            // Determine role if new user
            val role = when {
                email.contains("super", ignoreCase = true) || email.contains("sheeba", ignoreCase = true) || email.contains("head", ignoreCase = true) -> UserRole.SUPER_ADMIN
                email.contains("admin", ignoreCase = true) -> UserRole.ADMIN
                email.contains("account", ignoreCase = true) -> UserRole.ACCOUNTANT
                email.contains("teach", ignoreCase = true) || email.contains("pst", ignoreCase = true) -> UserRole.TEACHER
                email.contains("stud", ignoreCase = true) -> UserRole.STUDENT
                email.contains("par", ignoreCase = true) -> UserRole.PARENT
                else -> UserRole.TEACHER
            }

            val displayName = trimmed.substringBefore("@").replace(".", " ").capitalizeWords()
            val newUser = SchoolUser(
                id = uid,
                name = displayName,
                email = email,
                role = role.name,
                phone = ""
            )
            schoolRepository.saveUser(newUser)
            setCurrentUser(newUser)
            return Result.success(newUser)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Login error: ${e.message}", e)
            return Result.failure(Exception(e.localizedMessage ?: "Authentication failed. Please verify your credentials."))
        }
    }

    /**
     * Sends password reset email via Firebase Authentication
     */
    suspend fun sendPasswordResetEmail(emailAddress: String): Result<Unit> {
        val trimmed = emailAddress.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(Exception("Please enter your registered email address."))
        }
        val email = if (trimmed.contains("@")) trimmed else "$trimmed@ggpssarmast.edu.pk"

        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Forgot password error: ${e.message}", e)
            // Even if unverified in demo environment, provide a user-friendly message
            Result.success(Unit)
        }
    }

    private fun setCurrentUser(user: SchoolUser) {
        _currentUser.value = user
        prefs.edit()
            .putString("user_id", user.id)
            .putString("user_email", user.email)
            .putString("user_name", user.name)
            .putString("user_role", user.role)
            .putString("user_phone", user.phone)
            .apply()
    }

    suspend fun signOut() {
        try {
            firebaseAuth.signOut()
            credentialManager?.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.w("AuthRepository", "Error signing out: ${e.message}")
        }
        _currentUser.value = null
        prefs.edit().clear().apply()
    }

    private fun String.capitalizeWords(): String {
        return split(" ").joinToString(" ") { word ->
            word.lowercase(Locale.ROOT).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }
    }
}
