package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.data.repository.AuthRepository
import com.example.data.repository.SchoolRepository
import com.example.navigation.AppNavigation
import com.example.ui.theme.GgpsTheme
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var schoolRepository: SchoolRepository
    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (e: Exception) {
            Log.w("MainActivity", "FirebaseApp init note: ${e.message}")
        }

        schoolRepository = SchoolRepository(applicationContext)
        authRepository = AuthRepository(applicationContext, schoolRepository)

        // Seed initial school data if Firestore is empty without blocking UI launch
        lifecycleScope.launch {
            try {
                schoolRepository.seedInitialDataIfEmpty()
            } catch (e: Exception) {
                Log.w("MainActivity", "Seed data note: ${e.message}")
            }
        }

        setContent {
            GgpsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(
                        authRepository = authRepository,
                        schoolRepository = schoolRepository
                    )
                }
            }
        }
    }
}
