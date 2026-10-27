package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.auth.AuthService
import com.example.data.local.VitalithDatabase
import com.example.data.repository.VitalithRepository
import com.example.ui.components.FloatingNavBar
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
  APPOINTMENT_PREP("Prep", Icons.AutoMirrored.Filled.EventNote),
  INGESTION("Ingest", Icons.Default.Sync),
  PRIVACY("Privacy", Icons.Default.Security),
  PROFILE("Profile", Icons.Default.AccountCircle),
  SETTINGS("Settings", Icons.Default.Settings),
  DATA_EXPORT_IMPORT("Data Export & Import", Icons.Default.ImportExport),
  ADD_RECORD("New Entry", Icons.Default.AddCircleOutline),
  APPEARANCE("Appearance & Themes", Icons.Default.Palette),
  SUBSCRIPTION("Membership & Subscription", Icons.Default.WorkspacePremium)
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

    val isDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
    com.example.ui.theme.ThemeManager.init(this, isDark)

    setContent {
      MyApplicationTheme {
        val currentUser by authService.currentUser.collectAsStateWithLifecycle()
        var hasCompletedOnboarding by rememberSaveable { mutableStateOf(authService.hasCompletedOnboarding()) }
        var initialAuthMode by rememberSaveable { mutableStateOf(AuthMode.LOGIN) }

        if (!hasCompletedOnboarding) {
          OnboardingScreen(
            onFinishOnboarding = { startRegisterMode ->
              authService.setCompletedOnboarding(true)
              hasCompletedOnboarding = true
              initialAuthMode = if (startRegisterMode) AuthMode.SIGN_UP else AuthMode.LOGIN
            }
          )
        } else if (currentUser == null) {
          AuthScreen(
            authService = authService,
            repository = repository,
            initialMode = initialAuthMode,
            onAuthSuccess = { /* Session established */ },
            onNavigateToOnboarding = {
              hasCompletedOnboarding = false
            }
          )
        } else {
          VitalithApp(
            repository = repository,
            authService = authService,
            onRevisitOnboarding = {
              hasCompletedOnboarding = false
            }
          )
        }
      }
    }
  }
}

@Composable
fun VitalithApp(
  repository: VitalithRepository,
  authService: AuthService? = null,
  onRevisitOnboarding: () -> Unit = {}
) {
  var currentScreen by rememberSaveable { mutableStateOf(AppScreen.HOME) }
  val screenStack = rememberSaveable(
    saver = listSaver(
      save = { it.map { screen -> screen.name } },
      restore = { restored ->
        mutableStateListOf<AppScreen>().apply {
          addAll(restored.map { AppScreen.valueOf(it) })
        }
      }
    )
  ) { mutableStateListOf<AppScreen>() }

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

  // Strict logout: clears screen backstack and purges active session
  fun handleLogout() {
    screenStack.clear()
    currentScreen = AppScreen.HOME
    authService?.signOut()
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

  val isPrimaryTab = currentScreen in primaryTabs

  Scaffold(
    bottomBar = {
      if (isPrimaryTab) {
        FloatingNavBar(
          currentScreen = currentScreen,
          onSelectScreen = { screen ->
            if (currentScreen != screen) {
              screenStack.clear()
              currentScreen = screen
            }
          },
          tabs = primaryTabs,
          modifier = Modifier.navigationBarsPadding()
        )
      }
    },
    contentWindowInsets = WindowInsets.statusBars
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(
          top = innerPadding.calculateTopPadding(),
          bottom = if (isPrimaryTab) innerPadding.calculateBottomPadding() else 0.dp
        )
        .consumeWindowInsets(innerPadding)
    ) {
      AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
          fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
        },
        label = "ScreenTransition"
      ) { targetScreen ->
        when (targetScreen) {
          AppScreen.HOME -> HomeScreen(
            repository = repository,
            onNavigateToTimeline = { navigateTo(AppScreen.TIMELINE) },
            onNavigateToAi = { navigateTo(AppScreen.AI_CHAT) },
            onNavigateToMeds = { navigateTo(AppScreen.MEDICATIONS) },
            onNavigateToSharing = { navigateTo(AppScreen.SHARING) },
            onNavigateToEmergency = { navigateTo(AppScreen.EMERGENCY) },
            onNavigateToPrep = { navigateTo(AppScreen.APPOINTMENT_PREP) },
            onNavigateToIngest = { navigateTo(AppScreen.INGESTION) },
            onNavigateToPrivacy = { navigateTo(AppScreen.PRIVACY) },
            onNavigateToProfile = { navigateTo(AppScreen.PROFILE) },
            onNavigateToSettings = { navigateTo(AppScreen.SETTINGS) },
            onNavigateToAddRecord = { navigateTo(AppScreen.ADD_RECORD) },
            onNavigateToSubscription = { navigateTo(AppScreen.SUBSCRIPTION) },
            onLogOut = { handleLogout() }
          )

          AppScreen.TIMELINE -> TimelineScreen(
            repository = repository,
            onNavigateToIngest = { navigateTo(AppScreen.INGESTION) },
            onNavigateToAddRecord = { navigateTo(AppScreen.ADD_RECORD) }
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

          AppScreen.PROFILE -> ProfileManagementScreen(
            repository = repository,
            onNavigateBack = { navigateBack() },
            onNavigateToPrivacy = { navigateTo(AppScreen.PRIVACY) },
            onNavigateToSubscription = { navigateTo(AppScreen.SUBSCRIPTION) },
            onLogOut = { handleLogout() }
          )

          AppScreen.SETTINGS -> SettingsScreen(
            repository = repository,
            authService = authService,
            onNavigateBack = { navigateBack() },
            onNavigateToPrivacy = { navigateTo(AppScreen.PRIVACY) },
            onNavigateToProfile = { navigateTo(AppScreen.PROFILE) },
            onNavigateToEmergency = { navigateTo(AppScreen.EMERGENCY) },
            onNavigateToExportImport = { navigateTo(AppScreen.DATA_EXPORT_IMPORT) },
            onNavigateToAppearance = { navigateTo(AppScreen.APPEARANCE) },
            onNavigateToSubscription = { navigateTo(AppScreen.SUBSCRIPTION) },
            onLogOut = { handleLogout() },
            onRevisitTour = onRevisitOnboarding
          )

          AppScreen.DATA_EXPORT_IMPORT -> DataExportImportScreen(
            repository = repository,
            onNavigateBack = { navigateBack() }
          )

          AppScreen.ADD_RECORD -> AddHealthRecordScreen(
            repository = repository,
            onNavigateBack = { navigateBack() }
          )

          AppScreen.APPEARANCE -> AppearanceSettingsScreen(
            onNavigateBack = { navigateBack() }
          )

          AppScreen.SUBSCRIPTION -> SubscriptionScreen(
            repository = repository,
            onNavigateBack = { navigateBack() }
          )
        }
      }
    }
  }
}
