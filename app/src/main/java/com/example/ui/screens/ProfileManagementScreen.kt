package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PatientProfile
import com.example.data.model.SubscriptionTier
import com.example.data.model.UserProfile
import com.example.data.repository.VitalithRepository
import com.example.ui.components.PatientProfileDropdownSelector
import com.example.ui.theme.*
import com.example.util.PatientAgeCalculator
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileManagementScreen(
  repository: VitalithRepository,
  onNavigateBack: () -> Unit,
  onNavigateToPrivacy: () -> Unit = {},
  onNavigateToSubscription: () -> Unit = {},
  onLogOut: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val profiles by repository.profiles.collectAsState()
  val currentProfileId by repository.currentProfileId.collectAsState()
  val userProfile by repository.userProfile.collectAsState()
  val subscriptionTier by repository.subscriptionTier.collectAsState()

  var selectedTab by remember { mutableIntStateOf(0) } // 0 = Patient Profiles, 1 = User Account
  var showAddPatientDialog by remember { mutableStateOf(false) }
  var showTierLimitDialog by remember { mutableStateOf(false) }
  var profileToEdit by remember { mutableStateOf<PatientProfile?>(null) }
  var profileToDelete by remember { mutableStateOf<PatientProfile?>(null) }
  var showEditUserDialog by remember { mutableStateOf(false) }
  var showExportSuccessSnackbar by remember { mutableStateOf(false) }
  var showLogOutConfirm by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(
              onClick = onNavigateBack,
              modifier = Modifier.testTag("profile_back_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "Profiles & Account",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Primary account credentials & family clinical records",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = VitalTealPrimary,
            modifier = Modifier.fillMaxWidth()
          ) {
            Tab(
              selected = selectedTab == 0,
              onClick = { selectedTab = 0 },
              text = {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Icon(imageVector = Icons.Default.FamilyRestroom, contentDescription = null, modifier = Modifier.size(18.dp))
                  Text("Patient Profiles (${profiles.size})", fontWeight = FontWeight.SemiBold)
                }
              },
              modifier = Modifier.testTag("tab_patient_profiles")
            )
            Tab(
              selected = selectedTab == 1,
              onClick = { selectedTab = 1 },
              text = {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Icon(imageVector = Icons.Default.ManageAccounts, contentDescription = null, modifier = Modifier.size(18.dp))
                  Text("User Account", fontWeight = FontWeight.SemiBold)
                }
              },
              modifier = Modifier.testTag("tab_user_account")
            )
          }
        }
      }
    },
    modifier = modifier.fillMaxSize()
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(innerPadding)
    ) {
      if (selectedTab == 0) {
        // Patient Profiles Tab
        PatientProfilesTabContent(
          profiles = profiles,
          currentProfileId = currentProfileId,
          onSwitchProfile = { repository.switchProfile(it) },
          onAddPatient = {
            if (profiles.size >= subscriptionTier.maxProfiles) {
              showTierLimitDialog = true
            } else {
              showAddPatientDialog = true
            }
          },
          onEditProfile = { profileToEdit = it },
          onDeleteProfile = { profileToDelete = it }
        )
      } else {
        // User Account Tab
        UserAccountTabContent(
          userProfile = userProfile,
          subscriptionTier = subscriptionTier,
          onEditUser = { showEditUserDialog = true },
          onToggleBiometrics = { repository.setBiometricLock(it) },
          onToggleEmergencyMedicalId = { repository.setEmergencyMedicalIdUnlocked(it) },
          onTierChange = { repository.setSubscriptionTier(it) },
          onNavigateToPrivacy = onNavigateToPrivacy,
          onNavigateToSubscription = onNavigateToSubscription,
          onExportData = { showExportSuccessSnackbar = true },
          onLogOut = { showLogOutConfirm = true }
        )
      }

      if (showExportSuccessSnackbar) {
        Snackbar(
          action = {
            TextButton(onClick = { showExportSuccessSnackbar = false }) {
              Text("Dismiss", color = VitalTealPrimary)
            }
          },
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(16.dp)
        ) {
          Text("Health archive exported (SHA-256 verified FHIR bundle)")
        }
      }
    }
  }

  // Dialog: Tier Limit Upgrade Alert
  if (showTierLimitDialog) {
    AlertDialog(
      onDismissRequest = { showTierLimitDialog = false },
      icon = {
        Icon(
          imageVector = Icons.Default.WorkspacePremium,
          contentDescription = null,
          tint = VitalTealPrimary,
          modifier = Modifier.size(32.dp)
        )
      },
      title = {
        Text(
          text = "Profile Limit Reached",
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Your current ${subscriptionTier.displayName} tier supports up to ${subscriptionTier.maxProfiles} patient profile.",
            fontSize = 14.sp
          )
          Text(
            text = "Upgrade to Vitalith Plus or Concierge to manage family members, dependents, and shared clinical records with automatic SMART-on-FHIR sync.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showTierLimitDialog = false
            onNavigateToSubscription()
          },
          colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary),
          modifier = Modifier.testTag("tier_limit_upgrade_button")
        ) {
          Text("View Plans & Upgrade")
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showTierLimitDialog = false },
          modifier = Modifier.testTag("tier_limit_dismiss_button")
        ) {
          Text("Cancel")
        }
      }
    )
  }

  // Dialog: Add Patient Profile
  if (showAddPatientDialog) {
    PatientProfileFormDialog(
      title = "Add Patient Profile",
      initialProfile = null,
      onDismiss = { showAddPatientDialog = false },
      onConfirm = { newProfile ->
        repository.addPatientProfile(newProfile)
        showAddPatientDialog = false
      }
    )
  }

  // Dialog: Edit Patient Profile
  profileToEdit?.let { profile ->
    PatientProfileFormDialog(
      title = "Edit Patient Profile",
      initialProfile = profile,
      onDismiss = { profileToEdit = null },
      onConfirm = { updatedProfile ->
        repository.updatePatientProfile(updatedProfile)
        profileToEdit = null
      }
    )
  }

  // Dialog: Delete Patient Profile Confirmation
  profileToDelete?.let { profile ->
    AlertDialog(
      onDismissRequest = { profileToDelete = null },
      icon = {
        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = VitalAlertRed)
      },
      title = { Text("Delete Patient Profile?") },
      text = {
        Text("Are you sure you want to remove '${profile.name}' (${profile.relationship})? Any health records associated with this profile will remain archived.")
      },
      confirmButton = {
        Button(
          onClick = {
            repository.deletePatientProfile(profile.id)
            profileToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = VitalAlertRed),
          modifier = Modifier.testTag("confirm_delete_patient_button")
        ) {
          Text("Delete Profile")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { profileToDelete = null }) {
          Text("Cancel")
        }
      }
    )
  }

  // Dialog: Edit User Profile
  if (showEditUserDialog) {
    EditUserAccountDialog(
      userProfile = userProfile,
      onDismiss = { showEditUserDialog = false },
      onConfirm = { updated ->
        repository.updateUserProfile(updated)
        showEditUserDialog = false
      }
    )
  }

  // Dialog: Log Out Confirmation
  if (showLogOutConfirm) {
    AlertDialog(
      onDismissRequest = { showLogOutConfirm = false },
      title = { Text("Sign Out & Lock Vault?", fontWeight = FontWeight.Bold) },
      text = {
        Text(
          text = "Signing out will immediately terminate the authenticated session, purge in-memory credentials, and lock the hardware encryption keystore.",
          fontSize = 13.sp
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showLogOutConfirm = false
            onLogOut()
          },
          colors = ButtonDefaults.buttonColors(containerColor = VitalAlertRed),
          modifier = Modifier.testTag("confirm_profile_logout_button")
        ) {
          Text("Log Out", color = Color.White, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showLogOutConfirm = false }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
private fun PatientProfilesTabContent(
  profiles: List<PatientProfile>,
  currentProfileId: String,
  onSwitchProfile: (String) -> Unit,
  onAddPatient: () -> Unit,
  onEditProfile: (PatientProfile) -> Unit,
  onDeleteProfile: (PatientProfile) -> Unit
) {
  val activeProfile = profiles.firstOrNull { it.id == currentProfileId } ?: profiles.firstOrNull()

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Active Profile Status & Quick Switch Card
    if (activeProfile != null) {
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = VitalTealContainer.copy(alpha = 0.45f)),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(44.dp)
                  .clip(CircleShape)
                  .background(VitalTealPrimary),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = activeProfile.name.take(1),
                  color = Color.White,
                  fontSize = 20.sp,
                  fontWeight = FontWeight.Bold
                )
              }
              Spacer(modifier = Modifier.width(14.dp))
              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Text(
                    text = "ACTIVE PROFILE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = VitalTealPrimary
                  )
                  Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = VitalEmeraldContainer
                  ) {
                    Text(
                      text = "Live Sync",
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = VitalOnEmeraldContainer,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }
                Text(
                  text = activeProfile.name,
                  fontSize = 18.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                val activeAgeStr = remember(activeProfile.dob) {
                  val years = PatientAgeCalculator.calculateAgeYears(activeProfile.dob)
                  if (years != null) " • Age: $years yrs" else ""
                }
                Text(
                  text = "${activeProfile.relationship} • MRN: ${activeProfile.mrn}$activeAgeStr • Blood: ${activeProfile.bloodType}",
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
              text = "SWITCH PATIENT PROFILE:",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            PatientProfileDropdownSelector(
              profiles = profiles,
              currentProfileId = currentProfileId,
              onSwitchProfile = onSwitchProfile,
              isFullWidth = true,
              modifier = Modifier.testTag("profile_mgmt_dropdown_selector")
            )
          }
        }
      }
    }

    // Section Header + Add Button
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Managed Clinical Records",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Switch active patient or update clinical demographics",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Button(
          onClick = onAddPatient,
          colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary),
          shape = RoundedCornerShape(12.dp),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
          modifier = Modifier.testTag("add_patient_profile_button")
        ) {
          Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Add Patient", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
      }
    }

    // Profiles List
    items(profiles, key = { it.id }) { profile ->
      val isActive = profile.id == currentProfileId

      Card(
        colors = CardDefaults.cardColors(
          containerColor = if (isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (isActive) {
          androidx.compose.foundation.BorderStroke(2.dp, VitalTealPrimary)
        } else {
          androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        },
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 3.dp else 1.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("patient_card_${profile.id}")
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          // Top Row: Avatar, Name, Relationship, Active Pill
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            val avatarBg = when {
              profile.isPrimary -> VitalTealPrimary
              profile.relationship.contains("Child", ignoreCase = true) -> VitalCyanAccent
              profile.relationship.contains("Parent", ignoreCase = true) -> VitalNavySurface
              else -> VitalGoldAccent
            }

            Box(
              modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(avatarBg),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = profile.name.take(1),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
              )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                  text = profile.name,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                if (profile.isPrimary) {
                  Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = VitalTealContainer
                  ) {
                    Text(
                      text = "PRIMARY",
                      fontSize = 9.sp,
                      fontWeight = FontWeight.Bold,
                      color = VitalOnTealContainer,
                      modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                  }
                }
              }
              Text(
                text = profile.relationship,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            if (isActive) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = VitalEmeraldContainer
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = VitalEmeraldSuccess, modifier = Modifier.size(14.dp))
                  Text(
                    text = "Active",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = VitalOnEmeraldContainer
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
          Spacer(modifier = Modifier.height(12.dp))

          // Clinical Details Grid
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("DOB / Age", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              val displayDob = remember(profile.dob) {
                if (profile.dob.contains("yr")) {
                  profile.dob
                } else {
                  val years = PatientAgeCalculator.calculateAgeYears(profile.dob)
                  if (years != null) "${profile.dob} ($years yrs)" else profile.dob
                }
              }
              Text(displayDob, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Column(modifier = Modifier.weight(1f)) {
              Text("Blood Type", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(profile.bloodType, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VitalAlertRed)
            }
            Column(modifier = Modifier.weight(1f)) {
              Text("Medical ID (MRN)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(profile.mrn, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Physician & Emergency Contact
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(imageVector = Icons.Default.LocalHospital, contentDescription = null, modifier = Modifier.size(14.dp), tint = VitalTealPrimary)
              Text("Physician: ${profile.primaryPhysician}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(imageVector = Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
              Text("Emergency: ${profile.emergencyContact}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }

          // Allergies list
          if (profile.allergies.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text("Allergies:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VitalAlertRed)
              profile.allergies.forEach { allergy ->
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = VitalAlertContainer
                ) {
                  Text(
                    text = allergy,
                    fontSize = 11.sp,
                    color = VitalOnAlertContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }
            }
          }

          // Notes
          if (profile.notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = "Note: ${profile.notes}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(8.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Action Buttons: Switch / Edit / Delete
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
          ) {
            if (!isActive) {
              Button(
                onClick = { onSwitchProfile(profile.id) },
                colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("activate_profile_${profile.id}")
              ) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Set as Active", fontSize = 12.sp)
              }
              Spacer(modifier = Modifier.width(8.dp))
            }

            OutlinedButton(
              onClick = { onEditProfile(profile) },
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              modifier = Modifier.testTag("edit_profile_${profile.id}")
            ) {
              Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Edit", fontSize = 12.sp)
            }

            if (!profile.isPrimary) {
              Spacer(modifier = Modifier.width(8.dp))
              IconButton(
                onClick = { onDeleteProfile(profile) },
                modifier = Modifier
                  .size(36.dp)
                  .testTag("delete_profile_${profile.id}")
              ) {
                Icon(
                  imageVector = Icons.Default.DeleteOutline,
                  contentDescription = "Delete Profile",
                  tint = VitalAlertRed,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun UserAccountTabContent(
  userProfile: UserProfile,
  subscriptionTier: SubscriptionTier,
  onEditUser: () -> Unit,
  onToggleBiometrics: (Boolean) -> Unit,
  onToggleEmergencyMedicalId: (Boolean) -> Unit,
  onTierChange: (SubscriptionTier) -> Unit,
  onNavigateToPrivacy: () -> Unit,
  onNavigateToSubscription: () -> Unit = {},
  onExportData: () -> Unit,
  onLogOut: () -> Unit
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Account Owner Profile Card
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(VitalNavySurface),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = userProfile.fullName.take(1),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
              )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = userProfile.fullName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = userProfile.email,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = userProfile.phone,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            IconButton(
              onClick = onEditUser,
              modifier = Modifier.testTag("edit_user_account_button")
            ) {
              Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Account", tint = VitalTealPrimary)
            }
          }

          Spacer(modifier = Modifier.height(14.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("Primary Emergency Contact", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text("${userProfile.emergencyContactName} • ${userProfile.emergencyContactPhone}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
          }
        }
      }
    }

    // Subscription & Plan Status
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("Vitalith Membership Tier", fontSize = 15.sp, fontWeight = FontWeight.Bold)
              Text("Clinical-grade AI models, unlimited FHIR links", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = VitalTealContainer
            ) {
              Text(
                text = subscriptionTier.name,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = VitalOnTealContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            SubscriptionTier.values().forEach { tier ->
              val isSelected = tier == subscriptionTier
              FilterChip(
                selected = isSelected,
                onClick = { onTierChange(tier) },
                label = { Text(tier.name, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = VitalTealPrimary,
                  selectedLabelColor = Color.White
                ),
                modifier = Modifier.testTag("tier_chip_${tier.name}")
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          OutlinedButton(
            onClick = onNavigateToSubscription,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("profile_manage_subscription_button"),
            shape = RoundedCornerShape(10.dp)
          ) {
            Icon(
              imageVector = Icons.Default.WorkspacePremium,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
              tint = VitalTealPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Manage Membership & Invoices",
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }
    }

    // Security & Cryptographic Enclave
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text("Account Security & Cryptography", fontSize = 15.sp, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(12.dp))

          // Biometric Lock Toggle
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Biometric App Lock", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
              Text("Require Fingerprint or FaceID to open Vitalith", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
              checked = userProfile.biometricLockEnabled,
              onCheckedChange = onToggleBiometrics,
              modifier = Modifier.testTag("biometric_lock_toggle")
            )
          }

          Spacer(modifier = Modifier.height(10.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
          Spacer(modifier = Modifier.height(10.dp))

          // Emergency Medical ID Unlocked Toggle
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Emergency Medical ID Access", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
              Text("Allow first responders to view Blood Type & Allergies without passcode", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
              checked = userProfile.emergencyMedicalIdUnlocked,
              onCheckedChange = onToggleEmergencyMedicalId,
              modifier = Modifier.testTag("emergency_id_toggle")
            )
          }

          Spacer(modifier = Modifier.height(10.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
          Spacer(modifier = Modifier.height(10.dp))

          // Hardware Enclave Info
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = VitalEmeraldSuccess, modifier = Modifier.size(18.dp))
            Column {
              Text("Hardware-Backed Storage", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
              Text("Local AES-256 keys generated in Android Hardware Keystore", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }
    }

    // Legal Compliance & Health Data Actions
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text("Health Governance & Export", fontSize = 15.sp, fontWeight = FontWeight.Bold)

          Surface(
            shape = RoundedCornerShape(8.dp),
            color = VitalEmeraldContainer.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(imageVector = Icons.Default.VerifiedUser, contentDescription = null, tint = VitalEmeraldSuccess, modifier = Modifier.size(20.dp))
              Column {
                Text("HIPAA Authorization Active", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VitalOnEmeraldContainer)
                Text("Patient-directed exchange consent: ${userProfile.hipaaConsentDate}", fontSize = 11.sp, color = VitalOnEmeraldContainer)
              }
            }
          }

          OutlinedButton(
            onClick = onExportData,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("export_health_data_button")
          ) {
            Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Export Full FHIR Health Archive (JSON & PDF)")
          }

          OutlinedButton(
            onClick = onNavigateToPrivacy,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("view_security_audit_button")
          ) {
            Icon(imageVector = Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("View Cryptographic Audit Log & Access History")
          }
        }
      }
    }

    // Active Session & Security Lockout Card
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, VitalAlertRed.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text("Active Session & Enclave Security", fontSize = 15.sp, fontWeight = FontWeight.Bold)
          Text(
            text = "Terminate your authenticated vault session and lock the hardware keystore. You will need to authenticate again with your password, passkey, or biometrics.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp
          )
          Button(
            onClick = onLogOut,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VitalAlertRed),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("profile_logout_button")
          ) {
            Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Log Out & Lock Vault", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PatientProfileFormDialog(
  title: String,
  initialProfile: PatientProfile?,
  onDismiss: () -> Unit,
  onConfirm: (PatientProfile) -> Unit
) {
  var name by remember { mutableStateOf(initialProfile?.name ?: "") }
  var relationship by remember { mutableStateOf(initialProfile?.relationship ?: "Family Dependent") }

  // Extract clean date string YYYY-MM-DD
  var dobDateOnly by remember {
    mutableStateOf(
      PatientAgeCalculator.extractDateOnly(initialProfile?.dob ?: "1995-05-15")
    )
  }

  // Automatically calculate patient age in years
  val calculatedAgeYears by remember(dobDateOnly) {
    derivedStateOf { PatientAgeCalculator.calculateAgeYears(dobDateOnly) }
  }

  var showDatePicker by remember { mutableStateOf(false) }

  val initialDateMillis = remember(dobDateOnly) {
    PatientAgeCalculator.parseDateToUtcMillis(dobDateOnly) ?: System.currentTimeMillis()
  }

  val datePickerState = rememberDatePickerState(
    initialSelectedDateMillis = initialDateMillis
  )

  var bloodType by remember { mutableStateOf(initialProfile?.bloodType ?: "O+") }
  var mrn by remember { mutableStateOf(initialProfile?.mrn ?: "VTL-${(1000..9999).random()}-${name.take(2).uppercase().ifEmpty { "PT" }}") }
  var primaryPhysician by remember { mutableStateOf(initialProfile?.primaryPhysician ?: "Dr. Sarah Jenkins, MD") }
  var allergiesText by remember { mutableStateOf(initialProfile?.allergies?.joinToString(", ") ?: "") }
  var emergencyContact by remember { mutableStateOf(initialProfile?.emergencyContact ?: "Doug Adarkwah • +1 (555) 234-8901") }
  var notes by remember { mutableStateOf(initialProfile?.notes ?: "") }
  var gender by remember { mutableStateOf(initialProfile?.gender ?: "Not Specified") }

  val relationshipOptions = listOf("Child", "Elderly Parent", "Spouse", "Dependent", "Other Family")
  val bloodTypeOptions = listOf("O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-")

  if (showDatePicker) {
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        Button(
          onClick = {
            datePickerState.selectedDateMillis?.let { millis ->
              dobDateOnly = PatientAgeCalculator.formatUtcMillisToDateString(millis)
            }
            showDatePicker = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary),
          modifier = Modifier.testTag("patient_dob_picker_confirm")
        ) {
          Text("Select Date", color = Color.White, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = { showDatePicker = false },
          modifier = Modifier.testTag("patient_dob_picker_cancel")
        ) {
          Text("Cancel")
        }
      }
    ) {
      DatePicker(
        state = datePickerState,
        title = {
          Text(
            text = "Select Patient Date of Birth",
            modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
          )
        }
      )
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title, fontWeight = FontWeight.Bold) },
    text = {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 450.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        item {
          OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name *") },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("patient_name_input")
          )
        }

        item {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Relationship", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            FlowRow(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              relationshipOptions.forEach { rel ->
                FilterChip(
                  selected = relationship.contains(rel, ignoreCase = true),
                  onClick = { relationship = rel },
                  label = { Text(rel, fontSize = 11.sp) }
                )
              }
            }
          }
        }

        // Date of Birth with Material 3 Date Picker & Real-Time Automatic Age Calculation
        item {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Date of Birth *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
              FilledTonalButton(
                onClick = { showDatePicker = true },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("patient_dob_picker_button")
              ) {
                Icon(
                  imageVector = Icons.Default.CalendarMonth,
                  contentDescription = "Open Calendar Date Picker",
                  tint = VitalTealPrimary,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Pick Date", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = VitalTealPrimary)
              }
            }

            OutlinedTextField(
              value = dobDateOnly,
              onValueChange = { input -> dobDateOnly = input },
              label = { Text("Date of Birth (YYYY-MM-DD)") },
              placeholder = { Text("YYYY-MM-DD") },
              singleLine = true,
              trailingIcon = {
                IconButton(
                  onClick = { showDatePicker = true },
                  modifier = Modifier.testTag("patient_dob_calendar_icon")
                ) {
                  Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Select Date of Birth",
                    tint = VitalTealPrimary
                  )
                }
              },
              supportingText = {
                Text("Pick date via calendar button or type YYYY-MM-DD")
              },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("patient_dob_input")
            )

            // Automatically Calculated Years Card
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (calculatedAgeYears != null) VitalTealContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              border = if (calculatedAgeYears != null) androidx.compose.foundation.BorderStroke(1.dp, VitalTealPrimary.copy(alpha = 0.4f)) else null,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("patient_calculated_age_card")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (calculatedAgeYears != null) VitalTealPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.Cake,
                    contentDescription = null,
                    tint = if (calculatedAgeYears != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                  )
                }
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "AUTOMATICALLY CALCULATED AGE",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (calculatedAgeYears != null) VitalTealPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                  )
                  Text(
                    text = if (calculatedAgeYears != null) {
                      PatientAgeCalculator.formatAgeLabel(calculatedAgeYears)
                    } else {
                      "Pick or enter a valid date above to compute patient years"
                    },
                    fontSize = 13.sp,
                    fontWeight = if (calculatedAgeYears != null) FontWeight.Bold else FontWeight.Normal,
                    color = if (calculatedAgeYears != null) VitalOnTealContainer else MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
                if (calculatedAgeYears != null) {
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = VitalTealPrimary
                  ) {
                    Text(
                      text = "$calculatedAgeYears YRS",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.ExtraBold,
                      color = Color.White,
                      modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                        .testTag("patient_calculated_age_badge")
                    )
                  }
                }
              }
            }
          }
        }

        item {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Blood Type", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            FlowRow(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              bloodTypeOptions.forEach { bt ->
                FilterChip(
                  selected = bloodType == bt,
                  onClick = { bloodType = bt },
                  label = { Text(bt, fontSize = 11.sp) }
                )
              }
            }
          }
        }

        item {
          OutlinedTextField(
            value = mrn,
            onValueChange = { mrn = it },
            label = { Text("Medical Record Number (MRN)") },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("patient_mrn_input")
          )
        }

        item {
          OutlinedTextField(
            value = primaryPhysician,
            onValueChange = { primaryPhysician = it },
            label = { Text("Primary Care Physician") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }

        item {
          OutlinedTextField(
            value = allergiesText,
            onValueChange = { allergiesText = it },
            label = { Text("Known Allergies (comma separated)") },
            placeholder = { Text("e.g. Penicillin, Peanuts") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }

        item {
          OutlinedTextField(
            value = emergencyContact,
            onValueChange = { emergencyContact = it },
            label = { Text("Emergency Contact") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }

        item {
          OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Clinical Notes / Pre-existing conditions") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isNotBlank()) {
            val allergiesList = allergiesText.split(",")
              .map { it.trim() }
              .filter { it.isNotBlank() }

            val formattedDob = PatientAgeCalculator.formatDobWithAge(dobDateOnly, calculatedAgeYears)

            val profile = PatientProfile(
              id = initialProfile?.id ?: "profile_${UUID.randomUUID().toString().take(8)}",
              name = name.trim(),
              relationship = relationship,
              dob = formattedDob,
              bloodType = bloodType,
              mrn = mrn.trim(),
              isPrimary = initialProfile?.isPrimary ?: false,
              activeConditionsCount = initialProfile?.activeConditionsCount ?: 0,
              activeMedicationsCount = initialProfile?.activeMedicationsCount ?: 0,
              pendingTasksCount = initialProfile?.pendingTasksCount ?: 0,
              primaryPhysician = primaryPhysician.trim(),
              allergies = allergiesList,
              emergencyContact = emergencyContact.trim(),
              notes = notes.trim(),
              gender = gender
            )
            onConfirm(profile)
          }
        },
        enabled = name.isNotBlank(),
        colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary),
        modifier = Modifier.testTag("save_patient_profile_button")
      ) {
        Text("Save Profile")
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

@Composable
private fun EditUserAccountDialog(
  userProfile: UserProfile,
  onDismiss: () -> Unit,
  onConfirm: (UserProfile) -> Unit
) {
  var fullName by remember { mutableStateOf(userProfile.fullName) }
  var email by remember { mutableStateOf(userProfile.email) }
  var phone by remember { mutableStateOf(userProfile.phone) }
  var emergencyContactName by remember { mutableStateOf(userProfile.emergencyContactName) }
  var emergencyContactPhone by remember { mutableStateOf(userProfile.emergencyContactPhone) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Edit Account Information", fontWeight = FontWeight.Bold) },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedTextField(
          value = fullName,
          onValueChange = { fullName = it },
          label = { Text("Account Holder Name") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("user_name_input")
        )
        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text("Email Address") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("user_email_input")
        )
        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = { Text("Phone Number") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = emergencyContactName,
          onValueChange = { emergencyContactName = it },
          label = { Text("Primary Emergency Contact Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = emergencyContactPhone,
          onValueChange = { emergencyContactPhone = it },
          label = { Text("Emergency Contact Phone") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (fullName.isNotBlank() && email.isNotBlank()) {
            onConfirm(
              userProfile.copy(
                fullName = fullName.trim(),
                email = email.trim(),
                phone = phone.trim(),
                emergencyContactName = emergencyContactName.trim(),
                emergencyContactPhone = emergencyContactPhone.trim()
              )
            )
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary),
        modifier = Modifier.testTag("save_user_account_button")
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
