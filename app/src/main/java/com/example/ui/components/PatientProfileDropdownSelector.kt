package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WorkspacePremium
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
import com.example.ui.theme.*

/**
 * Universal Patient Profile Switcher Dropdown.
 * Mirrors the header implementation for consistent, accessible patient record switching across all screens.
 */
@Composable
fun PatientProfileDropdownSelector(
  profiles: List<PatientProfile>,
  currentProfileId: String,
  onSwitchProfile: (String) -> Unit,
  modifier: Modifier = Modifier,
  isFullWidth: Boolean = false,
  onNavigateToManageProfiles: (() -> Unit)? = null,
  onNavigateToSubscription: (() -> Unit)? = null,
  testTagPrefix: String = "profile_selector"
) {
  var isExpanded by remember { mutableStateOf(false) }
  val currentProfile = profiles.firstOrNull { it.id == currentProfileId } ?: profiles.firstOrNull()

  Box(modifier = modifier) {
    Surface(
      color = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(20.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
      modifier = (if (isFullWidth) Modifier.fillMaxWidth() else Modifier)
        .clip(RoundedCornerShape(20.dp))
        .clickable { isExpanded = true }
        .testTag("${testTagPrefix}_button")
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isFullWidth) Arrangement.SpaceBetween else Arrangement.spacedBy(6.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Box(
            modifier = Modifier
              .size(26.dp)
              .clip(CircleShape)
              .background(VitalTealPrimary),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = currentProfile?.name?.take(1) ?: "P",
              color = Color.White,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }

          Column {
            Text(
              text = if (isFullWidth) (currentProfile?.name ?: "Select Patient") else (currentProfile?.name?.split(" ")?.firstOrNull() ?: "Patient"),
              fontSize = 13.5.sp,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface
            )
            if (isFullWidth && currentProfile != null) {
              Text(
                text = "${currentProfile.relationship} • MRN: ${currentProfile.mrn}",
                fontSize = 10.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        Icon(
          imageVector = Icons.Default.ArrowDropDown,
          contentDescription = "Switch Patient Profile Dropdown",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(20.dp)
        )
      }
    }

    DropdownMenu(
      expanded = isExpanded,
      onDismissRequest = { isExpanded = false },
      modifier = Modifier
        .widthIn(min = 240.dp, max = 340.dp)
        .testTag("${testTagPrefix}_dropdown_menu")
    ) {
      Text(
        text = "Switch Patient Profile",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        color = VitalTealPrimary,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
      )
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

      profiles.forEach { p ->
        val isSelected = p.id == currentProfileId
        DropdownMenuItem(
          text = {
            Column {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Text(
                  text = p.name,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                if (isSelected) {
                  Surface(
                    color = VitalEmeraldContainer,
                    shape = RoundedCornerShape(4.dp)
                  ) {
                    Text(
                      text = "ACTIVE",
                      fontSize = 8.5.sp,
                      fontWeight = FontWeight.ExtraBold,
                      color = VitalOnEmeraldContainer,
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
          },
          onClick = {
            onSwitchProfile(p.id)
            isExpanded = false
          },
          leadingIcon = {
            Icon(
              imageVector = if (p.isPrimary) Icons.Default.Person else Icons.Default.FamilyRestroom,
              contentDescription = null,
              tint = if (isSelected) VitalTealPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
          },
          modifier = Modifier.testTag("${testTagPrefix}_item_${p.id}")
        )
      }

      if (onNavigateToSubscription != null || onNavigateToManageProfiles != null) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        if (onNavigateToSubscription != null) {
          DropdownMenuItem(
            text = {
              Text(
                text = "Membership & Subscription",
                fontWeight = FontWeight.Bold,
                color = VitalTealPrimary
              )
            },
            onClick = {
              isExpanded = false
              onNavigateToSubscription()
            },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = null,
                tint = VitalTealPrimary
              )
            },
            modifier = Modifier.testTag("${testTagPrefix}_menu_subscription")
          )
        }

        if (onNavigateToManageProfiles != null) {
          DropdownMenuItem(
            text = {
              Text(
                text = "Manage Profiles & Account",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
            },
            onClick = {
              isExpanded = false
              onNavigateToManageProfiles()
            },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.ManageAccounts,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
            },
            modifier = Modifier.testTag("${testTagPrefix}_menu_manage_profiles")
          )
        }
      }
    }
  }
}
