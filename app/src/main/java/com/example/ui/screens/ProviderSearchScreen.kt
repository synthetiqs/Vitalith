package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.HealthcareProvider
import com.example.data.model.HealthcareSpecialty
import com.example.data.model.UserLocationInfo
import com.example.data.repository.VitalithRepository
import com.example.ui.components.PatientProfileDropdownSelector
import com.example.ui.theme.*
import com.example.util.GoogleProviderSearchService

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProviderSearchScreen(
  repository: VitalithRepository,
  onNavigateBack: () -> Unit,
  onNavigateToPrep: (doctorName: String) -> Unit = {},
  initialQuery: String = "",
  initialSpecialty: HealthcareSpecialty = HealthcareSpecialty.ALL,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val profiles by repository.profiles.collectAsState()
  val currentProfileId by repository.currentProfileId.collectAsState()
  val currentProfile = profiles.firstOrNull { it.id == currentProfileId } ?: profiles.first()

  var searchQuery by remember { mutableStateOf(initialQuery) }
  var selectedSpecialty by remember { mutableStateOf(initialSpecialty) }

  // Location state
  var userLocation by remember {
    mutableStateOf(GoogleProviderSearchService.resolveDeviceLocation(context))
  }
  var showLocationDialog by remember { mutableStateOf(false) }
  var customLocationInput by remember { mutableStateOf("") }
  var selectedRadiusMiles by remember { mutableIntStateOf(15) }

  // Permission launcher for Location
  val locationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      userLocation = GoogleProviderSearchService.resolveDeviceLocation(context)
    }
  }

  // Filtered providers
  val providers = remember(searchQuery, selectedSpecialty, userLocation, currentProfile) {
    GoogleProviderSearchService.searchProviders(
      query = searchQuery,
      specialty = selectedSpecialty,
      location = userLocation,
      profile = currentProfile
    )
  }

  // Patient suggestions
  val patientSuggestions = remember(currentProfile) {
    GoogleProviderSearchService.getPatientSuggestions(currentProfile)
  }

  Scaffold(
    topBar = {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.testTag("provider_search_back_button")
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = "Back"
                )
              }
              Column {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Text(
                    text = "Find Care & Specialists",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                  )
                  Surface(
                    color = Color(0xFF4285F4).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF1A73E8),
                        modifier = Modifier.size(11.dp)
                      )
                      Text(
                        text = "Google Search",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A73E8)
                      )
                    }
                  }
                }
                Text(
                  text = "Nearby doctors, clinics & diagnostic centers",
                  fontSize = 11.5.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            PatientProfileDropdownSelector(
              profiles = profiles,
              currentProfileId = currentProfileId,
              onSwitchProfile = { repository.switchProfile(it) },
              testTagPrefix = "provider_search_profile_selector"
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Location Banner & GPS Detection
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(
                  imageVector = Icons.Default.LocationOn,
                  contentDescription = "Location",
                  tint = if (userLocation.isGpsAccurate) VitalEmeraldSuccess else VitalTealPrimary,
                  modifier = Modifier.size(18.dp)
                )
                Column {
                  Text(
                    text = userLocation.displayName,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = if (userLocation.isGpsAccurate) "GPS Active • Within ${selectedRadiusMiles} mi" else "Location Area • Within ${selectedRadiusMiles} mi",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilledTonalButton(
                  onClick = {
                    val hasPerm = ContextCompat.checkSelfPermission(
                      context,
                      Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasPerm) {
                      userLocation = GoogleProviderSearchService.resolveDeviceLocation(context)
                    } else {
                      locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                  },
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                  modifier = Modifier
                    .height(32.dp)
                    .testTag("provider_search_detect_location_button")
                ) {
                  Icon(imageVector = Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("GPS", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                  onClick = {
                    customLocationInput = userLocation.displayName
                    showLocationDialog = true
                  },
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                  modifier = Modifier
                    .height(32.dp)
                    .testTag("provider_search_change_location_button")
                ) {
                  Text("Edit", fontSize = 11.sp)
                }
              }
            }
          }
        }
      }
    },
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Search Bar Row
      item {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search doctor, clinic, condition or lab...", fontSize = 13.sp) },
          leadingIcon = {
            Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = VitalTealPrimary)
          },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
              }
            }
          },
          shape = RoundedCornerShape(12.dp),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("provider_search_text_input")
        )
      }

      // Specialty Filter Chips
      item {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "CARE SPECIALTY",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            items(HealthcareSpecialty.values()) { spec ->
              FilterChip(
                selected = selectedSpecialty == spec,
                onClick = { selectedSpecialty = spec },
                label = { Text(spec.chipLabel, fontSize = 12.sp) },
                leadingIcon = if (selectedSpecialty == spec) {
                  { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                } else null,
                modifier = Modifier.testTag("provider_filter_chip_${spec.name}")
              )
            }
          }
        }
      }

      // Patient-Specific Recommended Searches Card
      item {
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = VitalTealPrimary,
                modifier = Modifier.size(18.dp)
              )
              Text(
                text = "Care Needs for ${currentProfile.name}",
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp
              )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              patientSuggestions.forEach { suggestion ->
                Surface(
                  onClick = {
                    searchQuery = suggestion.query
                    selectedSpecialty = suggestion.specialty
                  },
                  shape = RoundedCornerShape(10.dp),
                  color = MaterialTheme.colorScheme.surface,
                  border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                      ) {
                        Text(
                          text = suggestion.title,
                          fontWeight = FontWeight.SemiBold,
                          fontSize = 12.5.sp
                        )
                        Surface(
                          color = VitalTealContainer.copy(alpha = 0.6f),
                          shape = RoundedCornerShape(4.dp)
                        ) {
                          Text(
                            text = suggestion.reasonBadge,
                            color = VitalOnTealContainer,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                          )
                        }
                      }
                      Text(
                        text = suggestion.subtitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }

                    Icon(
                      imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                      contentDescription = null,
                      tint = VitalTealPrimary,
                      modifier = Modifier.size(16.dp)
                    )
                  }
                }
              }
            }
          }
        }
      }

      // Direct Live Google Search CTA
      item {
        val activeSearchTerm = if (searchQuery.isNotBlank()) searchQuery else selectedSpecialty.searchQuery
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.TravelExplore,
                  contentDescription = null,
                  tint = Color(0xFF38BDF8),
                  modifier = Modifier.size(18.dp)
                )
                Text(
                  text = "Live Google Search",
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.5.sp
                )
              }
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "Search \"$activeSearchTerm\" in ${userLocation.displayName} on Google",
                color = Color(0xFF94A3B8),
                fontSize = 11.5.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
              )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Button(
              onClick = {
                GoogleProviderSearchService.launchGoogleSearch(
                  context = context,
                  query = activeSearchTerm,
                  location = userLocation.displayName
                )
              },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.testTag("provider_search_live_google_button")
            ) {
              Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Search", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      // Provider Results Header
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "MATCHED PROVIDERS & CLINICS (${providers.size})",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          TextButton(
            onClick = {
              val mapQuery = if (searchQuery.isNotBlank()) searchQuery else selectedSpecialty.searchQuery
              GoogleProviderSearchService.launchGoogleMaps(
                context = context,
                query = mapQuery,
                location = userLocation.displayName,
                lat = userLocation.latitude,
                lng = userLocation.longitude
              )
            },
            modifier = Modifier.testTag("provider_search_open_maps_button")
          ) {
            Icon(imageVector = Icons.Default.Map, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Google Maps", fontSize = 11.5.sp)
          }
        }
      }

      // Provider Cards List
      items(providers, key = { it.id }) { provider ->
        ProviderDetailCard(
          provider = provider,
          locationDisplay = userLocation.displayName,
          userLat = userLocation.latitude,
          userLng = userLocation.longitude,
          onNavigateToPrep = onNavigateToPrep
        )
      }

      // Bottom Spacer
      item {
        Spacer(modifier = Modifier.height(32.dp))
      }
    }
  }

  // Location Edit Dialog
  if (showLocationDialog) {
    AlertDialog(
      onDismissRequest = { showLocationDialog = false },
      title = { Text("Set Search Location") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(
            text = "Enter a city, state, or postal ZIP code to find specialists in that area:",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          OutlinedTextField(
            value = customLocationInput,
            onValueChange = { customLocationInput = it },
            placeholder = { Text("e.g. Seattle, WA or 98101") },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("location_dialog_input")
          )

          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Search Radius", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              listOf(5, 15, 25, 50).forEach { radius ->
                FilterChip(
                  selected = selectedRadiusMiles == radius,
                  onClick = { selectedRadiusMiles = radius },
                  label = { Text("${radius} mi", fontSize = 11.sp) }
                )
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (customLocationInput.isNotBlank()) {
              userLocation = UserLocationInfo(
                displayName = customLocationInput.trim(),
                city = customLocationInput.split(",").firstOrNull()?.trim() ?: customLocationInput.trim(),
                state = customLocationInput.split(",").getOrNull(1)?.trim() ?: "",
                isGpsAccurate = false
              )
            }
            showLocationDialog = false
          },
          modifier = Modifier.testTag("location_dialog_save_button")
        ) {
          Text("Apply")
        }
      },
      dismissButton = {
        TextButton(onClick = { showLocationDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProviderDetailCard(
  provider: HealthcareProvider,
  locationDisplay: String,
  userLat: Double?,
  userLng: Double?,
  onNavigateToPrep: (doctorName: String) -> Unit
) {
  val context = LocalContext.current

  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("provider_card_${provider.id}")
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      // Header: Name, Credentials, Distance & Rating
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = "${provider.name}, ${provider.title}",
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp
            )
            if (provider.verifiedBadge) {
              Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = "Verified Practice",
                tint = VitalTealPrimary,
                modifier = Modifier.size(16.dp)
              )
            }
          }
          Text(
            text = provider.specialty,
            color = VitalTealPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.5.sp
          )
          if (provider.subSpecialty.isNotBlank()) {
            Text(
              text = provider.subSpecialty,
              fontSize = 11.5.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Rating & Distance badge
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
          shape = RoundedCornerShape(8.dp)
        ) {
          Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.End
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = VitalGoldAccent,
                modifier = Modifier.size(13.dp)
              )
              Text(
                text = "${provider.rating}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "(${provider.reviewCount})",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Text(
              text = "${provider.distanceMiles} mi away",
              fontSize = 10.sp,
              color = VitalTealPrimary,
              fontWeight = FontWeight.Medium
            )
          }
        }
      }

      // Facility & Address
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Business,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
          )
          Column {
            Text(
              text = provider.facilityName,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = "${provider.address}, ${provider.city}, ${provider.state} ${provider.zip}",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      // Clinical Bio / Overview
      if (provider.bio.isNotBlank()) {
        Text(
          text = provider.bio,
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
          lineHeight = 16.sp
        )
      }

      // Capability Badges
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        if (provider.acceptingNewPatients) {
          Surface(
            color = VitalEmeraldContainer.copy(alpha = 0.7f),
            shape = RoundedCornerShape(4.dp)
          ) {
            Text(
              text = "✓ Accepting New Patients",
              color = VitalOnEmeraldContainer,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
        if (provider.telehealthAvailable) {
          Surface(
            color = VitalTealContainer.copy(alpha = 0.7f),
            shape = RoundedCornerShape(4.dp)
          ) {
            Text(
              text = "Telehealth / Virtual Visits",
              color = VitalOnTealContainer,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
        if (provider.hospitalAffiliation.isNotBlank()) {
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(4.dp)
          ) {
            Text(
              text = provider.hospitalAffiliation,
              fontSize = 10.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
      }

      // Insurance Coverage
      if (provider.insuranceAccepted.isNotEmpty()) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(
            text = "Insurances:",
            fontSize = 10.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
          )
          Text(
            text = provider.insuranceAccepted.joinToString(", "),
            fontSize = 10.5.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

      // Direct Action Buttons: Google Search, Google Maps, Phone Dial, Prep
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        // Google Search Button
        FilledTonalButton(
          onClick = {
            GoogleProviderSearchService.launchGoogleSearch(
              context = context,
              query = provider.googleSearchQuery,
              location = locationDisplay
            )
          },
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
          colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = Color(0xFF4285F4).copy(alpha = 0.12f),
            contentColor = Color(0xFF1A73E8)
          ),
          modifier = Modifier.height(34.dp)
        ) {
          Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(13.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Google Search", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
        }

        // Google Maps Button
        OutlinedButton(
          onClick = {
            GoogleProviderSearchService.launchGoogleMaps(
              context = context,
              query = provider.googleMapsQuery,
              location = locationDisplay,
              lat = userLat,
              lng = userLng
            )
          },
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
          modifier = Modifier.height(34.dp)
        ) {
          Icon(imageVector = Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(13.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Maps", fontSize = 11.5.sp)
        }

        // Call Office Button
        OutlinedButton(
          onClick = {
            GoogleProviderSearchService.dialProvider(context, provider.phoneNumber)
          },
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
          modifier = Modifier.height(34.dp)
        ) {
          Icon(imageVector = Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(13.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Call", fontSize = 11.5.sp)
        }

        // Prep Appointment with this Provider
        Button(
          onClick = { onNavigateToPrep(provider.name) },
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
          colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary),
          modifier = Modifier.height(34.dp)
        ) {
          Icon(imageVector = Icons.AutoMirrored.Filled.EventNote, contentDescription = null, modifier = Modifier.size(13.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Prep Visit", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
        }
      }
    }
  }
}
