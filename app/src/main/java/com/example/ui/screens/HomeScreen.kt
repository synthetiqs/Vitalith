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
import com.example.data.model.*
import com.example.data.repository.VitalithRepository
import com.example.ui.components.ClinicalAlertBanner
import com.example.ui.components.ProvenanceBadge
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
  modifier: Modifier = Modifier
) {
  val profiles by repository.profiles.collectAsState()
  val currentProfileId by repository.currentProfileId.collectAsState()
  val timelineItems by repository.timelineItems.collectAsState()
  val medications by repository.medications.collectAsState()
  val subscriptionTier by repository.subscriptionTier.collectAsState()

  val currentProfile = profiles.firstOrNull { it.id == currentProfileId } ?: profiles.first()
  var showProfileDropdown by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = modifier
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

        // Profile Selector Button
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
            Divider()
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
          }
        }
      }
    }

    // Clinical Disclaimer Banner
    item {
      ClinicalAlertBanner()
    }

    // Emergency Snapshot Card
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = VitalNavy),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onNavigateToEmergency() }
          .testTag("emergency_snapshot_banner")
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
          ) {
            Box(
              modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(VitalAlertRed.copy(alpha = 0.2f))
                .border(1.dp, VitalAlertRed, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.HealthAndSafety,
                contentDescription = "Emergency",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
              )
            }
            Column {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                  text = "Emergency Snapshot",
                  color = Color.White,
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold
                )
                Surface(
                  color = VitalAlertRed,
                  shape = RoundedCornerShape(4.dp)
                ) {
                  Text(
                    text = "O+ • Penicillin Alert",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                  )
                }
              }
              Text(
                text = "Instant responder view: Allergies, Meds, Contacts",
                color = Color(0xFFCBD5E1),
                fontSize = 11.sp
              )
            }
          }
          Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Open",
            tint = Color.White
          )
        }
      }
    }

    // Quick Actions Grid (4 primary actions)
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        ActionCard(
          title = "Connect FHIR",
          subtitle = "Epic / Cerner",
          icon = Icons.Default.Sync,
          color = VitalTealPrimary,
          onClick = onNavigateToIngest,
          modifier = Modifier.weight(1f)
        )
        ActionCard(
          title = "Upload OCR",
          subtitle = "Lab PDFs & Scans",
          icon = Icons.Default.UploadFile,
          color = VitalEmeraldSuccess,
          onClick = onNavigateToIngest,
          modifier = Modifier.weight(1f)
        )
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        ActionCard(
          title = "Ask AI",
          subtitle = "Grounded Chat",
          icon = Icons.Default.Psychology,
          color = VitalGoldAccent,
          onClick = onNavigateToAi,
          modifier = Modifier.weight(1f)
        )
        ActionCard(
          title = "Share Record",
          subtitle = "Time-Limited Link",
          icon = Icons.Default.Share,
          color = MaterialTheme.colorScheme.secondary,
          onClick = onNavigateToSharing,
          modifier = Modifier.weight(1f)
        )
      }
    }

    // Appointment Prep Card (Layer B / Coordinator)
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onNavigateToPrep() }
          .testTag("appointment_prep_card")
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(
                imageVector = Icons.Default.EventNote,
                contentDescription = null,
                tint = VitalTealPrimary,
                modifier = Modifier.size(20.dp)
              )
              Text(
                text = "Upcoming Appointment Prep",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
              )
            }
            Surface(
              color = VitalEmeraldContainer,
              shape = RoundedCornerShape(4.dp)
            ) {
              Text(
                text = "Ready",
                color = VitalEmeraldSuccess,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Dr. Sarah Chen, MD • Bayfront Endocrinology (Nov 04, 2026)",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Auto-generated 1-page visit briefing: A1c dropped to 6.4%, 4 recommended questions, and current medication reconciliation ready for review.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp
          )
          Spacer(modifier = Modifier.height(10.dp))
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
              imageVector = Icons.Default.ArrowForward,
              contentDescription = null,
              tint = VitalTealPrimary,
              modifier = Modifier.size(14.dp)
            )
          }
        }
      }
    }

    // Health Trends Spotlight (Layer B)
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(
            text = "Longitudinal Biomarker Trends",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            TrendTile(
              label = "HbA1c",
              value = "6.4%",
              change = "-0.4% (Improving)",
              isPositive = true,
              date = "Oct 14, 2026",
              modifier = Modifier.weight(1f)
            )
            TrendTile(
              label = "eGFR (Kidney)",
              value = "98 mL/min",
              change = "Optimal Function",
              isPositive = true,
              date = "Aug 15, 2026",
              modifier = Modifier.weight(1f)
            )
          }
        }
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
