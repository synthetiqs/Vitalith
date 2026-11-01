package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CareGrant
import com.example.data.model.ItemStatus
import com.example.data.model.SubscriptionTier
import com.example.data.repository.VitalithRepository
import com.example.ui.components.PatientProfileDropdownSelector
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharingScreen(
  repository: VitalithRepository,
  modifier: Modifier = Modifier,
  onNavigateToSubscription: () -> Unit = {}
) {
  val context = LocalContext.current
  val careGrants by repository.careGrants.collectAsState()
  val timelineItems by repository.timelineItems.collectAsState()
  val medications by repository.medications.collectAsState()
  val profiles by repository.profiles.collectAsState()
  val subscriptionTier by repository.subscriptionTier.collectAsState()
  val currentProfileId by repository.currentProfileId.collectAsState()
  val currentProfile = remember(profiles, currentProfileId) {
    profiles.firstOrNull { it.id == currentProfileId } ?: profiles.firstOrNull()
  }

  val profileCareGrants = remember(careGrants, currentProfileId) {
    careGrants.filter { it.profileId == currentProfileId }
  }
  val activeGrants = remember(profileCareGrants) {
    profileCareGrants.filter { it.isActive }
  }
  val profileTimelineItems = remember(timelineItems, currentProfileId) {
    timelineItems.filter { it.profileId == currentProfileId }
  }
  val profileMedications = remember(medications, currentProfileId) {
    medications.filter { it.profileId == currentProfileId }
  }

  var showCreateGrantDialog by remember { mutableStateOf(false) }
  var showUpgradeModal by remember { mutableStateOf(false) }
  var newlyCreatedGrant by remember { mutableStateOf<CareGrant?>(null) }
  var previewGrantForDoctorView by remember { mutableStateOf<CareGrant?>(null) }
  var showQrCodeGrant by remember { mutableStateOf<CareGrant?>(null) }

  fun copyToClipboard(link: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Vitalith Secure Care Link", link)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
  }

  fun shareViaSystemIntent(grant: CareGrant) {
    val link = "https://vitalith.health/vault/grant/${grant.tokenKey}"
    val shareText = "Vitalith Secure Patient-Authorized Care Link\n\n" +
        "Recipient: ${grant.recipientName} (${grant.recipientRole})\n" +
        "Scope: ${grant.scope}\n" +
        "Expiration: ${grant.expiresAt}\n\n" +
        "Access Link: $link\n\n" +
        "This link grants time-limited, encrypted access directly to patient-owned health records. Patient can revoke at any time."

    val intent = Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_SUBJECT, "Secure Health Vault Access: ${grant.recipientName}")
      putExtra(Intent.EXTRA_TEXT, shareText)
    }
    val chooser = Intent.createChooser(intent, "Share Care Link via").apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // Header
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 2.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Care-Team Sharing",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Time-limited, revocable links • Granular scopes",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          PatientProfileDropdownSelector(
            profiles = profiles,
            currentProfileId = currentProfileId,
            onSwitchProfile = { repository.switchProfile(it) },
            onNavigateToSubscription = onNavigateToSubscription,
            testTagPrefix = "sharing_profile_selector"
          )
          FilledTonalIconButton(
            onClick = {
              if (subscriptionTier == SubscriptionTier.FREE && activeGrants.isNotEmpty()) {
                showUpgradeModal = true
              } else {
                showCreateGrantDialog = true
              }
            },
            colors = IconButtonDefaults.filledTonalIconButtonColors(
              containerColor = VitalTealContainer,
              contentColor = VitalTealPrimary
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .size(44.dp)
              .testTag("create_share_link_button")
          ) {
            Icon(
              imageVector = Icons.Default.AddLink,
              contentDescription = "Create Share Link",
              modifier = Modifier.size(22.dp)
            )
          }
        }
      }
    }

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
      contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Security Protocol Notice
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = VitalTealContainer.copy(alpha = 0.45f)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = VitalTealPrimary, modifier = Modifier.size(28.dp))
            Column {
              Text(
                text = "Patient-Controlled Tokenized Access",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = VitalOnTealContainer
              )
              Text(
                text = "Shared links use cryptographically signed tokens. You can copy the link, launch the system share sheet, or revoke instantly at any second.",
                fontSize = 11.5.sp,
                color = VitalOnTealContainer,
                lineHeight = 16.sp
              )
            }
          }
        }
      }

      if (subscriptionTier == SubscriptionTier.FREE && activeGrants.isNotEmpty()) {
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = VitalWarningContainer.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = VitalWarningOrange)
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Basic Tier: 1 Active Care Grant Limit",
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  color = Color(0xFF7C2D12)
                )
                Text(
                  text = "You have reached the maximum active share grant for the Free tier. Upgrade to Premium for unlimited links.",
                  fontSize = 11.5.sp,
                  color = Color(0xFF7C2D12)
                )
              }
              Button(
                onClick = onNavigateToSubscription,
                colors = ButtonDefaults.buttonColors(containerColor = VitalWarningOrange),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
              ) {
                Text("Upgrade", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
              }
            }
          }
        }
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Active Access Grants (${profileCareGrants.count { it.isActive }})",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Total Grants: ${profileCareGrants.size}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      items(profileCareGrants, key = { it.id }) { grant ->
        CareGrantCard(
          grant = grant,
          onCopyLink = { copyToClipboard("https://vitalith.health/vault/grant/${grant.tokenKey}") },
          onShareLink = { shareViaSystemIntent(grant) },
          onPreviewDoctorView = { previewGrantForDoctorView = grant },
          onShowQr = { showQrCodeGrant = grant },
          onRevoke = { repository.revokeCareGrant(grant.id) }
        )
      }
    }
  }

  // Upgrade Modal for Tier Gating
  if (showUpgradeModal) {
    AlertDialog(
      onDismissRequest = { showUpgradeModal = false },
      icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = VitalWarningOrange) },
      title = { Text("Care-Team Sharing Limit") },
      text = {
        Text("The Basic (Free) tier includes 1 active care-team share grant at a time. Upgrade to Premium or Concierge for unlimited links, granular timeframes, and instant revocations.")
      },
      confirmButton = {
        Button(
          onClick = {
            showUpgradeModal = false
            onNavigateToSubscription()
          },
          colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary)
        ) {
          Text("Upgrade Plan")
        }
      },
      dismissButton = {
        TextButton(onClick = { showUpgradeModal = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // 1. Create Grant Dialog
  if (showCreateGrantDialog) {
    var recipientName by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Specialist / Clinician") }
    var selectedScope by remember { mutableStateOf("Full Longitudinal Record") }
    var selectedDuration by remember { mutableStateOf("7 days") }

    AlertDialog(
      onDismissRequest = { showCreateGrantDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Default.VpnKey, contentDescription = null, tint = VitalTealPrimary)
          Text("Generate Secure Care Link", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }
      },
      text = {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Text(
            text = "Specify who is receiving access and what clinical data they are permitted to see.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          OutlinedTextField(
            value = recipientName,
            onValueChange = { recipientName = it },
            label = { Text("Clinician or Facility Name") },
            placeholder = { Text("e.g. Dr. Jennifer Davis") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = role,
            onValueChange = { role = it },
            label = { Text("Role / Specialty") },
            placeholder = { Text("e.g. Cardiology, ER, Nutrition") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          Text("Access Scope", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
          val scopes = listOf(
            "Full Longitudinal Record" to "All labs, encounters, vitals & meds",
            "Labs & Medications Only" to "Diagnostic reports & active prescriptions",
            "Emergency Snapshot Only" to "Blood type, allergies & primary contact"
          )
          scopes.forEach { (scope, desc) ->
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (selectedScope == scope) VitalTealContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
              border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (selectedScope == scope) VitalTealPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
              ),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { selectedScope = scope }
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(10.dp)
              ) {
                RadioButton(
                  selected = selectedScope == scope,
                  onClick = { selectedScope = scope }
                )
                Column(modifier = Modifier.padding(start = 4.dp)) {
                  Text(text = scope, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                  Text(text = desc, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
            }
          }

          Text("Link Expiration", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
          val durations = listOf("24 hours", "7 days", "30 days")
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            durations.forEach { dur ->
              FilterChip(
                selected = selectedDuration == dur,
                onClick = { selectedDuration = dur },
                label = { Text(dur, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = VitalTealPrimary,
                  selectedLabelColor = Color.White
                )
              )
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (recipientName.isNotBlank()) {
              val created = repository.createCareGrant(
                recipientName = recipientName,
                role = role,
                scope = selectedScope,
                durationLabel = selectedDuration
              )
              showCreateGrantDialog = false
              newlyCreatedGrant = created
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("Generate Link")
        }
      },
      dismissButton = {
        TextButton(onClick = { showCreateGrantDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // 2. Newly Created Grant Success Modal
  newlyCreatedGrant?.let { grant ->
    val shareUrl = "https://vitalith.health/vault/grant/${grant.tokenKey}"
    AlertDialog(
      onDismissRequest = { newlyCreatedGrant = null },
      icon = {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VitalEmeraldSuccess, modifier = Modifier.size(36.dp))
      },
      title = {
        Text("Care Link Ready", fontWeight = FontWeight.Bold, fontSize = 18.sp)
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(
            text = "Link created for ${grant.recipientName} (${grant.recipientRole}). You can send this link via SMS, Email, or WhatsApp, or have them scan the QR code.",
            fontSize = 12.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(text = "SHAREABLE URL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = VitalTealPrimary)
              Text(
                text = shareUrl,
                fontSize = 11.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Scope: ${grant.scope} • Expires: ${grant.expiresAt}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = { copyToClipboard(shareUrl) },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(8.dp)
            ) {
              Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Copy Link", fontSize = 12.sp)
            }

            Button(
              onClick = { shareViaSystemIntent(grant) },
              colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary),
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(8.dp)
            ) {
              Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Share Sheet", fontSize = 12.sp)
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { newlyCreatedGrant = null }) {
          Text("Done")
        }
      }
    )
  }

  // 3. Clinician View Live Simulation Modal
  previewGrantForDoctorView?.let { grant ->
    ModalBottomSheet(
      onDismissRequest = { previewGrantForDoctorView = null },
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
      containerColor = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp)
          .padding(bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Clinician View Banner
        Surface(
          color = VitalNavy,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(Icons.Default.Visibility, contentDescription = null, tint = VitalTealPrimary, modifier = Modifier.size(24.dp))
            Column {
              Text(
                text = "CLINICIAN PORTAL PREVIEW",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = VitalTealPrimary
              )
              Text(
                text = "Showing exact screen viewed by ${grant.recipientName}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
              Text(
                text = "Enforced Scope: ${grant.scope} • Token: ${grant.tokenKey.take(12)}...",
                fontSize = 10.sp,
                color = Color(0xFFCBD5E1)
              )
            }
          }
        }

        // Patient Banner
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(text = currentProfile?.name ?: "Doug Adarkwah", fontWeight = FontWeight.Bold, fontSize = 15.sp)
              Text(text = "MRN: ${currentProfile?.mrn ?: "VTL-8921-DA"} • DOB: ${currentProfile?.dob ?: "1988-04-12"}", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(
              color = VitalEmeraldContainer,
              shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                text = "AUTHENTICATED",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = VitalEmeraldSuccess,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
        }

        // Scope-filtered data
        if (grant.scope.contains("Emergency") || grant.scope.contains("Full")) {
          Text(text = "Emergency Essentials", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VitalTealPrimary)
          Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(text = "Blood Group: ${currentProfile?.bloodType ?: "O+"}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
              Text(text = "Allergies: ${currentProfile?.allergies?.joinToString(", ") ?: "Penicillin"}", fontSize = 12.sp, color = VitalAlertRed, fontWeight = FontWeight.Bold)
              Text(text = "Emergency Contact: ${currentProfile?.emergencyContact ?: "Elena Adarkwah • +1 (555) 345-6789"}", fontSize = 11.5.sp)
            }
          }
        }

        if (grant.scope.contains("Medications") || grant.scope.contains("Full")) {
          Text(text = "Active Medications (${profileMedications.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VitalTealPrimary)
          profileMedications.forEach { med ->
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text(text = "${med.name} • ${med.dosage}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(text = "Sig: ${med.frequency} • Prescriber: ${med.prescriber}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }

        if (grant.scope.contains("Full") || grant.scope.contains("Labs")) {
          Text(text = "Clinical Timeline Records", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VitalTealPrimary)
          val filteredTimeline = if (grant.scope.contains("Labs Only")) {
            profileTimelineItems.filter { it.category == com.example.data.model.TimelineCategory.LAB }
          } else {
            profileTimelineItems.take(4)
          }

          filteredTimeline.forEach { item ->
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text(text = item.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                  Text(text = item.date, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(text = item.clinicalNotes, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }

        Button(
          onClick = { previewGrantForDoctorView = null },
          colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Close Preview")
        }
      }
    }
  }

  // 4. QR Code Modal
  showQrCodeGrant?.let { grant ->
    val shareUrl = "https://vitalith.health/vault/grant/${grant.tokenKey}"
    AlertDialog(
      onDismissRequest = { showQrCodeGrant = null },
      title = {
        Text("Care Link QR Code", fontWeight = FontWeight.Bold, fontSize = 17.sp)
      },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          Text(
            text = "Have ${grant.recipientName} or clinic staff scan this code with their camera or tablet.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          // Styled QR Code Graphic representation
          Box(
            modifier = Modifier
              .size(180.dp)
              .background(Color.White, RoundedCornerShape(12.dp))
              .border(2.dp, VitalTealPrimary, RoundedCornerShape(12.dp))
              .padding(16.dp),
            contentAlignment = Alignment.Center
          ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
              val w = size.width
              val h = size.height
              val step = w / 9f

              // Draw corner alignment squares
              drawRect(Color.Black, Offset(0f, 0f), Size(step * 3, step * 3))
              drawRect(Color.White, Offset(step, step), Size(step, step))

              drawRect(Color.Black, Offset(w - step * 3, 0f), Size(step * 3, step * 3))
              drawRect(Color.White, Offset(w - step * 2, step), Size(step, step))

              drawRect(Color.Black, Offset(0f, h - step * 3), Size(step * 3, step * 3))
              drawRect(Color.White, Offset(step, h - step * 2), Size(step, step))

              // Center data dots pattern
              drawRect(Color(0xFF0D9488), Offset(step * 4, step * 4), Size(step, step))
              drawRect(Color.Black, Offset(step * 3, step * 4), Size(step, step))
              drawRect(Color.Black, Offset(step * 5, step * 3), Size(step, step))
              drawRect(Color.Black, Offset(step * 4, step * 2), Size(step, step))
              drawRect(Color.Black, Offset(step * 6, step * 5), Size(step, step))
              drawRect(Color(0xFF0D9488), Offset(step * 2, step * 5), Size(step, step))
              drawRect(Color.Black, Offset(step * 4, step * 6), Size(step, step))
            }
          }

          Text(
            text = shareUrl,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = VitalTealPrimary
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            copyToClipboard(shareUrl)
            showQrCodeGrant = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary)
        ) {
          Text("Copy Link & Close")
        }
      },
      dismissButton = {
        TextButton(onClick = { showQrCodeGrant = null }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
fun CareGrantCard(
  grant: CareGrant,
  onCopyLink: () -> Unit,
  onShareLink: () -> Unit,
  onPreviewDoctorView: () -> Unit,
  onShowQr: () -> Unit,
  onRevoke: () -> Unit
) {
  val shareUrl = "https://vitalith.health/vault/grant/${grant.tokenKey}"

  Card(
    colors = CardDefaults.cardColors(
      containerColor = if (grant.isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ),
    shape = RoundedCornerShape(14.dp),
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      if (grant.isActive) MaterialTheme.colorScheme.outline.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    ),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = grant.recipientName,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (grant.isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = grant.recipientRole,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Surface(
          color = if (grant.isActive) VitalEmeraldContainer else VitalAlertContainer,
          shape = RoundedCornerShape(6.dp)
        ) {
          Text(
            text = if (grant.isActive) "ACTIVE" else "REVOKED",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (grant.isActive) VitalEmeraldSuccess else VitalAlertRed,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
          )
        }
      }

      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(text = "Scope: ${grant.scope}", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
            Text(text = grant.expiresAt, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
          }

          // Real HTTPS link display
          Text(
            text = shareUrl,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = VitalTealPrimary,
            maxLines = 1
          )

          Text(
            text = "Token: ${grant.tokenKey.take(16)}... • Accessed ${grant.accessCount} times",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      if (grant.isActive) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Copy button
          OutlinedButton(
            onClick = onCopyLink,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Copy", fontSize = 11.5.sp)
          }

          // Share Sheet button
          Button(
            onClick = onShareLink,
            colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Share", fontSize = 11.5.sp)
          }

          // QR Code button
          IconButton(
            onClick = onShowQr,
            modifier = Modifier.size(36.dp)
          ) {
            Icon(Icons.Default.QrCode, contentDescription = "QR Code", tint = VitalTealPrimary)
          }

          // Doctor Preview button
          IconButton(
            onClick = onPreviewDoctorView,
            modifier = Modifier.size(36.dp)
          ) {
            Icon(Icons.Default.Visibility, contentDescription = "Preview", tint = VitalNavy)
          }
        }

        // Revoke Button
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(
            onClick = onRevoke,
            colors = ButtonDefaults.textButtonColors(contentColor = VitalAlertRed),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
          ) {
            Icon(imageVector = Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Revoke Immediately", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
