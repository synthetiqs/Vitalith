package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.auth.AuthService
import com.example.data.local.VitalithDatabase
import com.example.data.repository.VitalithRepository
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.VitalTealPrimary

enum class AppScreen(val label: String, val icon: ImageVector) {
  HOME("Home", Icons.Default.Home),
  TIMELINE("Timeline", Icons.Default.Timeline),
  AI_CHAT("Ask AI", Icons.Default.Psychology),
  MEDICATIONS("Meds", Icons.Default.Medication),
  SHARING("Sharing", Icons.Default.Share),
  EMERGENCY("Emergency", Icons.Default.HealthAndSafety),
  APPOINTMENT_PREP("Prep", Icons.Default.EventNote),
  INGESTION("Ingest", Icons.Default.Sync),
  PRIVACY("Privacy", Icons.Default.Security)
}

class MainActivity : ComponentActivity() {
  private lateinit var repository: VitalithRepository
  private lateinit var authService: AuthService

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val database = VitalithDatabase.getDatabase(this)
    repository = VitalithRepository(database)
    authService = AuthService(this)

    setContent {
      MyApplicationTheme {
        VitalithApp(repository = repository, authService = authService)
      }
    }
  }
}

@Composable
fun VitalithApp(
  repository: VitalithRepository,
  authService: AuthService? = null
) {
  var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
  val screenStack = remember { mutableStateListOf<AppScreen>() }

  fun navigateTo(screen: AppScreen) {
    if (currentScreen != screen) {
      screenStack.add(currentScreen)
      currentScreen = screen
    }
  }

  fun navigateBack() {
    if (screenStack.isNotEmpty()) {
      currentScreen = screenStack.removeAt(screenStack.size - 1)
    } else {
      currentScreen = AppScreen.HOME
    }
  }

  // Handle hardware back button
  BackHandler(enabled = currentScreen != AppScreen.HOME) {
    navigateBack()
  }

  val primaryTabs = listOf(
    AppScreen.HOME,
    AppScreen.TIMELINE,
    AppScreen.AI_CHAT,
    AppScreen.MEDICATIONS,
    AppScreen.SHARING
  )

  Scaffold(
    bottomBar = {
      NavigationBar(
        modifier = Modifier.testTag("vitalith_bottom_bar")
      ) {
        primaryTabs.forEach { screen ->
          val isSelected = currentScreen == screen
          NavigationBarItem(
            selected = isSelected,
            onClick = {
              if (currentScreen != screen) {
                screenStack.clear()
                currentScreen = screen
              }
            },
            icon = {
              Icon(
                imageVector = screen.icon,
                contentDescription = screen.label
              )
            },
            label = {
              Text(
                text = screen.label,
                fontSize = 11.sp
              )
            },
            modifier = Modifier.testTag("nav_tab_${screen.name.lowercase()}")
          )
        }
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      when (currentScreen) {
        AppScreen.HOME -> HomeScreen(
          repository = repository,
          onNavigateToTimeline = { navigateTo(AppScreen.TIMELINE) },
          onNavigateToAi = { navigateTo(AppScreen.AI_CHAT) },
          onNavigateToMeds = { navigateTo(AppScreen.MEDICATIONS) },
          onNavigateToSharing = { navigateTo(AppScreen.SHARING) },
          onNavigateToEmergency = { navigateTo(AppScreen.EMERGENCY) },
          onNavigateToPrep = { navigateTo(AppScreen.APPOINTMENT_PREP) },
          onNavigateToIngest = { navigateTo(AppScreen.INGESTION) },
          onNavigateToPrivacy = { navigateTo(AppScreen.PRIVACY) }
        )

        AppScreen.TIMELINE -> TimelineScreen(
          repository = repository,
          onNavigateToIngest = { navigateTo(AppScreen.INGESTION) }
        )

        AppScreen.AI_CHAT -> AiInterpreterScreen(
          repository = repository,
          onNavigateToPrep = { navigateTo(AppScreen.APPOINTMENT_PREP) }
        )

        AppScreen.MEDICATIONS -> MedicationsScreen(
          repository = repository
        )

        AppScreen.SHARING -> SharingScreen(
          repository = repository
        )

        AppScreen.EMERGENCY -> EmergencySnapshotScreen(
          repository = repository,
          onNavigateBack = { navigateBack() }
        )

        AppScreen.APPOINTMENT_PREP -> AppointmentPrepScreen(
          repository = repository,
          onNavigateBack = { navigateBack() },
          onShareWithDoctor = { navigateTo(AppScreen.SHARING) }
        )

        AppScreen.INGESTION -> IngestionScreen(
          repository = repository,
          onNavigateToTimeline = { navigateTo(AppScreen.TIMELINE) }
        )

        AppScreen.PRIVACY -> PrivacyAuditScreen(
          repository = repository,
          authService = authService
        )
      }
    }
  }
}
