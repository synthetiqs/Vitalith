package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.zIndex
import com.example.data.model.*
import com.example.data.repository.VitalithRepository
import com.example.ui.components.ClinicalAlertBanner
import com.example.ui.components.GeminiHealthSummaryCard
import com.example.ui.components.GlassHeroBanner
import com.example.ui.components.MonthlyHealthTrendsDashboard
import com.example.ui.components.ProvenanceBadge
import com.example.ui.components.QuickActionDock
import com.example.ui.components.RangeBar
import com.example.ui.components.Sparkline
import com.example.ui.theme.*

@Composable
fun HomeScreen(
  repository: VitalithRepository,
  onNavigateToTimeline: () -> Unit,
  onNavigateToAi: () -> Unit,
  onNavigateToMeds: () -> Unit,
  onNavigateToSharing: () -> Unit,
  onNavigateToEmergency: () -> Unit,
  onNavigateToPrep: () -> Unit,
  onNavigateToIngest: () -> Unit,
  onNavigateToPrivacy: () -> Unit,
  onNavigateToProfile: () -> Unit = {},
  onNavigateToSettings: () -> Unit = {},
  onNavigateToAddRecord: () -> Unit = {},
  onNavigateToSubscription: () -> Unit = {},
  onLogOut: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val profiles by repository.profiles.collectAsState()
  val userProfile by repository.userProfile.collectAsState()
  val currentProfileId by repository.currentProfileId.collectAsState()
  val timelineItems by repository.timelineItems.collectAsState()
  val medications by repository.medications.collectAsState()
  val subscriptionTier by repository.subscriptionTier.collectAsState()

  val currentProfile = profiles.firstOrNull { it.id == currentProfileId } ?: profiles.first()
  var showProfileDropdown by remember { mutableStateOf(false) }
  var showUserDropdown by remember { mutableStateOf(false) }
  var showLogOutConfirm by remember { mutableStateOf(false) }
  val activeThemeKey by ThemeManager.currentThemeKey.collectAsState()
  val themeTokens = LocalVitalithThemeTokens.current

  Box(modifier = modifier.fillMaxSize()) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(horizontal = 16.dp),
      contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
    // Header & Profile Switcher
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = "Vitalith",
              fontSize = 22.sp,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.primary
            )
            Surface(
              color = VitalTealContainer,
              shape = RoundedCornerShape(4.dp)
            ) {
              Text(
                text = "SynthetiQs",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = VitalOnTealContainer,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
          Text(
            text = "Your health history, in your hands.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        // Profile Selector & Account Actions
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Box {
            Surface(
              color = MaterialTheme.colorScheme.surface,
              shape = RoundedCornerShape(20.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
              modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable { showProfileDropdown = true }
                .testTag("profile_selector_button")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(VitalTealPrimary),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = currentProfile.name.take(1),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
                Text(
                  text = currentProfile.name.split(" ").first(),
                  fontSize = 13.sp,
                  fontWeight = FontWeight.SemiBold
                )
                Icon(
                  imageVector = Icons.Default.ArrowDropDown,
                  contentDescription = "Switch Profile",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(18.dp)
                )
              }
            }

            DropdownMenu(
              expanded = showProfileDropdown,
              onDismissRequest = { showProfileDropdown = false }
            ) {
              Text(
                text = "Switch Patient Profile",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
              )
              HorizontalDivider()
              profiles.forEach { p ->
                DropdownMenuItem(
                  text = {
                    Column {
                      Text(text = p.name, fontWeight = FontWeight.SemiBold)
                      Text(
                        text = "${p.relationship} • DOB: ${p.dob.take(10)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }
                  },
                  onClick = {
                    repository.switchProfile(p.id)
                    showProfileDropdown = false
                  },
                  leadingIcon = {
                    Icon(
                      imageVector = if (p.isPrimary) Icons.Default.Person else Icons.Default.FamilyRestroom,
                      contentDescription = null,
                      tint = if (p.id == currentProfileId) VitalTealPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                )
              }
              HorizontalDivider()
              DropdownMenuItem(
                text = {
                  Text(
                    text = "Membership & Subscription",
                    fontWeight = FontWeight.Bold,
                    color = VitalTealPrimary
                  )
                },
                onClick = {
                  showProfileDropdown = false
                  onNavigateToSubscription()
                },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = VitalTealPrimary
                  )
                },
                modifier = Modifier.testTag("menu_subscription_item")
              )
              DropdownMenuItem(
                text = {
                  Text(
                    text = "Manage Profiles & Account",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                },
                onClick = {
                  showProfileDropdown = false
                  onNavigateToProfile()
                },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Default.ManageAccounts,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                },
                modifier = Modifier.testTag("manage_profiles_menu_item")
              )
            }
          }

          IconButton(
            onClick = { showUserDropdown = true },
            modifier = Modifier.testTag("header_profile_button")
          ) {
            Box(
              modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(VitalTealContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "User Account & Profiles",
                tint = VitalTealPrimary,
                modifier = Modifier.size(26.dp)
              )
            }
          }
        }
      }
    }

    // Clinical Disclaimer Banner
    item {
      ClinicalAlertBanner()
    }

    // Glassmorphic Hero Banner (Replacing Emergency Banner)
    item {
      GlassHeroBanner(
        profile = currentProfile,
        onOpenEmergency = onNavigateToEmergency
      )
    }

    // Horizontal Quick-Action Pill Dock (Replacing 2x2 Grid)
    item {
      QuickActionDock(
        onConnectFhir = onNavigateToIngest,
        onUploadOcr = onNavigateToIngest,
        onAskAi = onNavigateToAi,
        onShareRecord = onNavigateToSharing
      )
    }

    // Gemini AI Natural Language Health Record Summary & Anomaly Radar
    item {
      GeminiHealthSummaryCard(
        timelineItems = timelineItems,
        patientName = currentProfile?.name ?: userProfile.fullName,
        unitPreferences = userProfile.unitPreferences
      )
    }

    // Appointment Prep Card (Layer B / Coordinator)
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onNavigateToPrep() }
          .testTag("appointment_prep_card")
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Box(
                modifier = Modifier
                  .size(32.dp)
                  .clip(CircleShape)
                  .background(VitalTealContainerDark.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.EventNote,
                  contentDescription = null,
                  tint = VitalTealPrimaryDark,
                  modifier = Modifier.size(18.dp)
                )
              }
              Text(
                text = "Upcoming Appointment Prep",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
              )
            }
            Surface(
              color = VitalEmeraldContainer,
              shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                text = "Ready",
                color = VitalEmeraldSuccess,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
              )
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "Dr. Sarah Chen, MD • Bayfront Endocrinology (Nov 04, 2026)",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Auto-generated 1-page visit briefing: A1c dropped to 6.4%, 4 recommended questions, and current medication reconciliation ready for review.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
          )
          Spacer(modifier = Modifier.height(12.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Text(
              text = "Open Visit Briefing",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = VitalTealPrimary
            )
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = null,
              tint = VitalTealPrimary,
              modifier = Modifier.size(14.dp)
            )
          }
        }
      }
    }

    // Interactive Monthly Health Trends Summary Dashboard (Recharts-style)
    item {
      MonthlyHealthTrendsDashboard(
        unitPreferences = userProfile.unitPreferences,
        onLogReadingClick = onNavigateToAddRecord
      )
    }

    // Health Trends Spotlight with Bezier Sparklines + Range Bars
    item {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "Longitudinal Biomarker Trends",
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )

        // HbA1c Card with Bezier Sparkline and Range Bar
        BiomarkerDetailCard(
          title = "Hemoglobin A1c",
          currentValue = "6.4%",
          changeText = "-0.4% from Jul 2026",
          isPositive = true,
          date = "Oct 14, 2026 • Quest Diagnostics",
          sparklinePoints = listOf(7.2f, 7.0f, 6.8f, 6.6f, 6.4f),
          lineColor = themeTokens.spark1,
          numericValue = 6.4f,
          minVal = 4.0f,
          maxVal = 9.0f,
          lowNormal = 4.0f,
          highNormal = 5.6f,
          unit = "%"
        )

        // eGFR Card with Bezier Sparkline and Range Bar
        BiomarkerDetailCard(
          title = "eGFR (Kidney Function)",
          currentValue = "98 mL/min",
          changeText = "Stable Filtration Rate",
          isPositive = true,
          date = "Aug 15, 2026 • MetroHealth",
          sparklinePoints = listOf(92f, 95f, 94f, 96f, 98f),
          lineColor = themeTokens.spark2,
          numericValue = 98f,
          minVal = 30f,
          maxVal = 120f,
          lowNormal = 60f,
          highNormal = 120f,
          unit = "mL/min"
        )
      }
    }

    // Recent Timeline Records Section
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Longitudinal Timeline",
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold
        )
        TextButton(onClick = onNavigateToTimeline) {
          Text("View All (${timelineItems.size})", color = VitalTealPrimary, fontSize = 12.sp)
        }
      }
    }

    items(timelineItems.take(3)) { item ->
      TimelineSummaryCard(item = item, onClick = onNavigateToTimeline)
    }

    // Patient Rights & Privacy Footer Card
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onNavigateToPrivacy() }
          .testTag("privacy_audit_footer")
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Security",
            tint = VitalEmeraldSuccess,
            modifier = Modifier.size(20.dp)
          )
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Patient-Owned • Zero Data Sale",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "AES-256 encrypted • Granular consent • Download FHIR R4",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }

  // Log Out Confirmation Dialog
  if (showLogOutConfirm) {
    AlertDialog(
      onDismissRequest = { showLogOutConfirm = false },
      icon = {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Logout,
          contentDescription = null,
          tint = VitalAlertRed,
          modifier = Modifier.size(28.dp)
        )
      },
      title = {
        Text(
          text = "Sign Out of Vitalith?",
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "You are signing out of your account (${userProfile.email}).",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "All patient records and clinical provenance remain encrypted in the local SQLite vault protected by your hardware keystore.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showLogOutConfirm = false
            onLogOut()
          },
          colors = ButtonDefaults.buttonColors(containerColor = VitalAlertRed),
          modifier = Modifier.testTag("confirm_logout_button")
        ) {
          Text("Log Out", color = Color.White, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = { showLogOutConfirm = false },
          modifier = Modifier.testTag("cancel_logout_button")
        ) {
          Text("Cancel")
        }
      },
      modifier = Modifier.testTag("logout_confirmation_dialog")
    )
  }

  // =========================================================================
  // Full-Height User Menu Drawer (occupies 100% full height of device screen)
  // =========================================================================
  AnimatedVisibility(
    visible = showUserDropdown,
    enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(180)) + slideInHorizontally(
      initialOffsetX = { fullWidth -> fullWidth },
      animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow)
    ),
    exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(150)) + slideOutHorizontally(
      targetOffsetX = { fullWidth -> fullWidth },
      animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow)
    ),
    modifier = Modifier.zIndex(100f)
  ) {
    BackHandler(enabled = showUserDropdown) { showUserDropdown = false }

    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.55f))
        .clickable { showUserDropdown = false }
    ) {
      Surface(
        modifier = Modifier
          .fillMaxHeight()
          .fillMaxWidth(0.88f)
          .widthIn(min = 320.dp, max = 420.dp)
          .align(Alignment.CenterEnd)
          .clickable(enabled = false) {} // Prevent backdrop dismiss when tapping inside drawer
          .testTag("user_dropdown_menu"),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 24.dp
      ) {
        Column(
          modifier = Modifier
            .fillMaxHeight()
            .navigationBarsPadding()
        ) {
          // Top Header Bar
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(32.dp)
                  .clip(CircleShape)
                  .background(VitalTealContainer),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Shield,
                  contentDescription = null,
                  tint = VitalTealPrimary,
                  modifier = Modifier.size(18.dp)
                )
              }
              Text(
                text = "Account & Profiles",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
            IconButton(
              onClick = { showUserDropdown = false },
              modifier = Modifier.size(36.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close Menu",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          HorizontalDivider()

          // Scrollable Drawer Content
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f)
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            // User Identity Header Card
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(46.dp)
                      .clip(CircleShape)
                      .background(VitalTealPrimary),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = userProfile.fullName.take(1).ifEmpty { "U" },
                      color = Color.White,
                      fontSize = 20.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = userProfile.fullName,
                      fontSize = 16.sp,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                      text = userProfile.email,
                      fontSize = 12.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                      text = userProfile.phone,
                      fontSize = 11.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                  }
                }

                Row(
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Surface(
                    color = VitalTealContainer,
                    shape = RoundedCornerShape(6.dp)
                  ) {
                    Text(
                      text = "Account Owner",
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = VitalOnTealContainer,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                  }
                  Surface(
                    color = VitalEmeraldContainer,
                    shape = RoundedCornerShape(6.dp)
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                      Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(VitalEmeraldSuccess))
                      Text(
                        text = "Hardware Enclave Active",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = VitalEmeraldSuccess
                      )
                    }
                  }
                }
              }
            }

            // Membership & Subscription (Positioned before Switch Patient Profile)
            Surface(
              onClick = {
                showUserDropdown = false
                onNavigateToSubscription()
              },
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.surface,
              border = androidx.compose.foundation.BorderStroke(1.5.dp, VitalTealPrimary.copy(alpha = 0.5f)),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("user_menu_subscription_entry")
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(VitalTealContainer),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = VitalTealPrimary,
                    modifier = Modifier.size(24.dp)
                  )
                }
                Column(modifier = Modifier.weight(1f)) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Text(
                      text = "MEMBERSHIP & PLAN",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      letterSpacing = 1.sp,
                      color = VitalTealPrimary
                    )
                    Surface(
                      color = VitalEmeraldContainer,
                      shape = RoundedCornerShape(4.dp)
                    ) {
                      Text(
                        text = "ACTIVE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = VitalOnEmeraldContainer,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                      )
                    }
                  }
                  Text(
                    text = subscriptionTier.displayName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = "Manage tiers, feature access & invoices",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(16.dp)
                )
              }
            }

            // Section 1: Switch Patient Profile
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "SWITCH PATIENT PROFILE",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.sp,
                  color = VitalTealPrimary
                )
                Text(
                  text = "${profiles.size} Available",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }

              profiles.forEach { p ->
                val isSelected = p.id == currentProfileId
                Surface(
                  onClick = {
                    repository.switchProfile(p.id)
                    showUserDropdown = false
                  },
                  shape = RoundedCornerShape(12.dp),
                  color = if (isSelected) VitalTealContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
                  border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelected) VitalTealPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                  ),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                  ) {
                    Box(
                      modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) VitalTealPrimary else VitalSlateBg),
                      contentAlignment = Alignment.Center
                    ) {
                      Icon(
                        imageVector = if (p.isPrimary) Icons.Default.Person else Icons.Default.FamilyRestroom,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                      )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                      ) {
                        Text(
                          text = p.name,
                          fontWeight = FontWeight.Bold,
                          fontSize = 13.sp,
                          color = MaterialTheme.colorScheme.onSurface
                        )
                        if (p.isPrimary) {
                          Surface(
                            color = VitalTealContainer,
                            shape = RoundedCornerShape(4.dp)
                          ) {
                            Text(
                              text = "Primary",
                              fontSize = 9.sp,
                              fontWeight = FontWeight.Bold,
                              color = VitalOnTealContainer,
                              modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                          }
                        }
                      }
                      Text(
                        text = "${p.relationship} • DOB: ${p.dob.take(10)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }
                    if (isSelected) {
                      Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Active Profile",
                        tint = VitalTealPrimary,
                        modifier = Modifier.size(20.dp)
                      )
                    }
                  }
                }
              }

              // Manage Profiles Action
              Surface(
                onClick = {
                  showUserDropdown = false
                  onNavigateToProfile()
                },
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = androidx.compose.foundation.BorderStroke(1.dp, VitalTealPrimary.copy(alpha = 0.3f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("user_menu_profiles_account")
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.ManageAccounts,
                    contentDescription = null,
                    tint = VitalTealPrimary,
                    modifier = Modifier.size(20.dp)
                  )
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "Manage Profiles & Account",
                      fontWeight = FontWeight.Bold,
                      fontSize = 13.sp,
                      color = VitalTealPrimary
                    )
                    Text(
                      text = "Add family members, edit MRN, notes & personal info",
                      fontSize = 11.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = VitalTealPrimary,
                    modifier = Modifier.size(15.dp)
                  )
                }
              }
            }

            HorizontalDivider()

            // Section 2: Security & Privacy Governance
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = "SECURITY & COMPLIANCE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )

              // Privacy, Security & Audit
              Surface(
                onClick = {
                  showUserDropdown = false
                  onNavigateToPrivacy()
                },
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("user_menu_privacy_audit")
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = VitalTealPrimary,
                    modifier = Modifier.size(20.dp)
                  )
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "Privacy, Security & Audit",
                      fontWeight = FontWeight.SemiBold,
                      fontSize = 13.sp
                    )
                    Text(
                      text = "Zero-knowledge keys, audit logs & FHIR export",
                      fontSize = 11.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                  )
                }
              }

              // Emergency Medical ID
              Surface(
                onClick = {
                  showUserDropdown = false
                  onNavigateToEmergency()
                },
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("user_menu_emergency_id")
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.HealthAndSafety,
                    contentDescription = null,
                    tint = VitalTealPrimary,
                    modifier = Modifier.size(20.dp)
                  )
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "Emergency Medical ID",
                      fontWeight = FontWeight.SemiBold,
                      fontSize = 13.sp
                    )
                    Text(
                      text = "Lockscreen snapshot, blood type & contacts",
                      fontSize = 11.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                  )
                }
              }
            }

            HorizontalDivider()

            // Section 3: Preferences & Enclave
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = "PREFERENCES & ENCLAVE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )

              Surface(
                onClick = {
                  showUserDropdown = false
                  onNavigateToSettings()
                },
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("user_menu_settings")
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = VitalTealPrimary,
                    modifier = Modifier.size(20.dp)
                  )
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "Settings",
                      fontWeight = FontWeight.SemiBold,
                      fontSize = 13.sp
                    )
                    Text(
                      text = "Biometrics, security & preferences dialog",
                      fontSize = 11.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                  )
                }
              }

              // Inline quick toggles
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text("Biometric Vault Lock", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                      Text("Require fingerprint or biometric scan", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                      checked = userProfile.biometricLockEnabled,
                      onCheckedChange = { repository.setBiometricLock(it) },
                      modifier = Modifier.testTag("drawer_biometric_toggle")
                    )
                  }
                  HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text("Emergency Medical ID", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                      Text("First-responder lockscreen visibility", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                      checked = userProfile.emergencyMedicalIdUnlocked,
                      onCheckedChange = { repository.setEmergencyMedicalIdUnlocked(it) },
                      modifier = Modifier.testTag("drawer_emergency_toggle")
                    )
                  }
                }
              }
            }

            HorizontalDivider()

            // Section 3.5: Appearance & Theme
            Column(
              verticalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.testTag("user_menu_appearance_section")
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "APPEARANCE & THEME",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = "${ALL_THEMES.size} Themes",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary
                )
              }

              Text(
                text = "Select from 9 medical palettes. Changes apply instantly across the entire application and persist in local storage.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 15.sp
              )

              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("user_menu_themes_radiogroup"),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                ALL_THEMES.forEach { themeDesc ->
                  val isThemeSelected = activeThemeKey == themeDesc.key
                  Surface(
                    onClick = {
                      ThemeManager.setTheme(themeDesc.key)
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isThemeSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                      1.dp,
                      if (isThemeSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("theme_option_${themeDesc.key}")
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                      // Color Swatch Circle with accent & bg
                      Box(
                        modifier = Modifier
                          .size(24.dp)
                          .clip(CircleShape)
                          .background(themeDesc.swatchBg)
                          .border(
                            width = 1.5.dp,
                            color = themeDesc.swatchAccent.copy(alpha = 0.8f),
                            shape = CircleShape
                          ),
                        contentAlignment = Alignment.Center
                      ) {
                        Box(
                          modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(themeDesc.swatchAccent)
                        )
                      }

                      Column(modifier = Modifier.weight(1f)) {
                        Row(
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                          Text(
                            text = themeDesc.label,
                            fontWeight = if (isThemeSelected) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                          )
                          Surface(
                            color = if (themeDesc.isDark) Color(0x33000000) else Color(0x22000000),
                            shape = RoundedCornerShape(4.dp)
                          ) {
                            Text(
                              text = if (themeDesc.key == "contrast") "AAA" else if (themeDesc.isDark) "Dark" else "Light",
                              fontSize = 9.sp,
                              fontWeight = FontWeight.Bold,
                              color = MaterialTheme.colorScheme.onSurfaceVariant,
                              modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                          }
                        }
                        Text(
                          text = when (themeDesc.key) {
                            "original" -> "Standard clinical obsidian navy & cyan"
                            "plum" -> "Deep plum with rose accent & sage ranges"
                            "forest" -> "Dark pine green with emerald telemetry"
                            "mono" -> "Neutral minimalist slate & pure white"
                            "indigo" -> "Deep royal indigo & vibrant violet"
                            "sandstone" -> "Warm earth tone with clay & amber"
                            "arctic" -> "Crisp high-readability daylight ice"
                            "rose" -> "Soft pastel daytime clinical aesthetic"
                            "contrast" -> "WCAG AAA black background & luminous yellow"
                            else -> "Design token palette"
                          },
                          fontSize = 10.sp,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                          maxLines = 1
                        )
                      }

                      if (isThemeSelected) {
                        Surface(
                          color = MaterialTheme.colorScheme.primary,
                          shape = CircleShape,
                          modifier = Modifier.size(20.dp)
                        ) {
                          Box(contentAlignment = Alignment.Center) {
                            Icon(
                              imageVector = Icons.Default.Check,
                              contentDescription = "Selected",
                              tint = MaterialTheme.colorScheme.onPrimary,
                              modifier = Modifier.size(14.dp)
                            )
                          }
                        }
                      }
                    }
                  }
                }
              }
            }

            HorizontalDivider()

            // Section 4: Log Out & Session Management
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = "SESSION & VAULT ACCESS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )

              Surface(
                onClick = {
                  showUserDropdown = false
                  showLogOutConfirm = true
                },
                shape = RoundedCornerShape(10.dp),
                color = VitalAlertContainer.copy(alpha = 0.3f),
                border = androidx.compose.foundation.BorderStroke(1.dp, VitalAlertRed.copy(alpha = 0.3f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("user_menu_logout")
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = VitalAlertRed,
                    modifier = Modifier.size(20.dp)
                  )
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "Log Out",
                      fontWeight = FontWeight.Bold,
                      fontSize = 13.sp,
                      color = VitalAlertRed
                    )
                    Text(
                      text = "Sign out and lock hardware keystore vault",
                      fontSize = 11.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
}

@Composable
fun ActionCard(
  title: String,
  subtitle: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  color: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(12.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
      .testTag("action_${title.lowercase().replace(" ", "_")}")
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(20.dp))
      }
      Spacer(modifier = Modifier.height(8.dp))
      Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold)
      Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

@Composable
fun BiomarkerDetailCard(
  title: String,
  currentValue: String,
  changeText: String,
  isPositive: Boolean,
  date: String,
  sparklinePoints: List<Float>,
  lineColor: Color,
  numericValue: Float,
  minVal: Float,
  maxVal: Float,
  lowNormal: Float,
  highNormal: Float,
  unit: String,
  modifier: Modifier = Modifier
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(18.dp),
    shadowElevation = 2.dp,
    tonalElevation = 2.dp,
    modifier = modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column {
          Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = currentValue,
            style = MetricDisplayLarge,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Surface(
          color = if (isPositive) VitalEmeraldContainer else VitalAlertContainer,
          shape = RoundedCornerShape(6.dp)
        ) {
          Text(
            text = changeText,
            color = if (isPositive) VitalEmeraldSuccess else VitalAlertRed,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Sparkline Canvas with Bezier curve & gradient fill
      Sparkline(
        points = sparklinePoints,
        lineColor = lineColor,
        modifier = Modifier
          .fillMaxWidth()
          .height(44.dp)
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Clinical 3-zone range bar with glowing indicator dot
      RangeBar(
        currentValue = numericValue,
        minVal = minVal,
        maxVal = maxVal,
        lowNormal = lowNormal,
        highNormal = highNormal,
        unit = unit
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = date,
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
      )
    }
  }
}

@Composable
fun TrendTile(
  label: String,
  value: String,
  change: String,
  isPositive: Boolean,
  date: String,
  modifier: Modifier = Modifier
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    shape = RoundedCornerShape(8.dp),
    modifier = modifier
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Spacer(modifier = Modifier.height(2.dp))
      Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = change,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (isPositive) VitalEmeraldSuccess else VitalAlertRed
      )
      Text(text = date, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

@Composable
fun TimelineSummaryCard(
  item: HealthTimelineItem,
  onClick: () -> Unit
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(10.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          CategoryChip(category = item.category)
          Text(text = item.date, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        StatusBadge(status = item.status)
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(text = item.title, fontSize = 14.sp, fontWeight = FontWeight.Bold)
      Text(text = item.subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Spacer(modifier = Modifier.height(8.dp))
      ProvenanceBadge(provenance = item.provenance)
    }
  }
}

@Composable
fun CategoryChip(category: TimelineCategory) {
  Surface(
    color = when (category) {
      TimelineCategory.LAB -> VitalTealContainer
      TimelineCategory.MEDICATION -> VitalGoldContainer
      TimelineCategory.CONDITION -> VitalAlertContainer
      TimelineCategory.ALLERGY -> VitalAlertContainer
      TimelineCategory.ENCOUNTER -> Color(0xFFE0E7FF)
      TimelineCategory.IMMUNIZATION -> VitalEmeraldContainer
      else -> Color(0xFFF1F5F9)
    },
    shape = RoundedCornerShape(4.dp)
  ) {
    Text(
      text = category.name,
      fontSize = 9.sp,
      fontWeight = FontWeight.Bold,
      color = when (category) {
        TimelineCategory.LAB -> VitalOnTealContainer
        TimelineCategory.MEDICATION -> VitalGoldAccent
        TimelineCategory.CONDITION -> VitalOnAlertContainer
        TimelineCategory.ALLERGY -> VitalOnAlertContainer
        TimelineCategory.ENCOUNTER -> Color(0xFF3730A3)
        TimelineCategory.IMMUNIZATION -> VitalOnEmeraldContainer
        else -> Color(0xFF334155)
      },
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
    )
  }
}

@Composable
fun StatusBadge(status: ItemStatus) {
  Surface(
    color = when (status) {
      ItemStatus.NORMAL, ItemStatus.ACTIVE, ItemStatus.RESOLVED -> VitalEmeraldContainer
      ItemStatus.ELEVATED -> VitalWarningContainer
      ItemStatus.CRITICAL -> VitalAlertContainer
      ItemStatus.DISCONTINUED -> Color(0xFFF1F5F9)
    },
    shape = RoundedCornerShape(4.dp)
  ) {
    Text(
      text = status.name,
      fontSize = 9.sp,
      fontWeight = FontWeight.Bold,
      color = when (status) {
        ItemStatus.NORMAL, ItemStatus.ACTIVE, ItemStatus.RESOLVED -> VitalEmeraldSuccess
        ItemStatus.ELEVATED -> VitalWarningOrange
        ItemStatus.CRITICAL -> VitalAlertRed
        ItemStatus.DISCONTINUED -> Color(0xFF64748B)
      },
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
    )
  }
}
