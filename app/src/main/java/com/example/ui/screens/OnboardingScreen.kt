package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.launch

data class OnboardingSlideData(
  val title: String,
  val subtitle: String,
  val description: String,
  val icon: ImageVector,
  val badgeText: String,
  val featureBadges: List<Pair<ImageVector, String>>,
  val gradientColors: List<Color>
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
  onFinishOnboarding: (startRegisterMode: Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  val coroutineScope = rememberCoroutineScope()
  val slides = remember {
    listOf(
      OnboardingSlideData(
        title = "Unified Health Vault",
        subtitle = "Consolidated SMART-on-FHIR Records",
        description = "Seamlessly ingest your complete longitudinal medical history. Connect EHR portals from Epic, Cerner, and LabCorp, or scan lab PDFs with clinical-grade OCR extraction.",
        icon = Icons.Default.FolderShared,
        badgeText = "INTEROPERABLE HL7 FHIR",
        featureBadges = listOf(
          Pair(Icons.Default.CloudSync, "SMART-on-FHIR Direct Ingest"),
          Pair(Icons.Default.DocumentScanner, "Clinical OCR Lab Parser"),
          Pair(Icons.Default.Lock, "Zero-Knowledge Encryption")
        ),
        gradientColors = listOf(VitalTealPrimaryDark, VitalCyanAccent)
      ),
      OnboardingSlideData(
        title = "Clinical AI Intelligence",
        subtitle = "Grounded Biomarker Radar & Visit Prep",
        description = "Empowered by Vitalith AI medical synthesis. Detect subtle glycemic and lipid trajectory anomalies, and auto-generate 1-page visit briefings for doctor consultations.",
        icon = Icons.Default.Psychology,
        badgeText = "REAL-TIME CLINICAL RADAR",
        featureBadges = listOf(
          Pair(Icons.AutoMirrored.Filled.ShowChart, "Longitudinal Biomarker Trends"),
          Pair(Icons.AutoMirrored.Filled.EventNote, "Automated Doctor Visit Prep"),
          Pair(Icons.Default.GppGood, "Grounded Evidence Citations")
        ),
        gradientColors = listOf(VitalCyanAccent, VitalEmeraldSuccess)
      ),
      OnboardingSlideData(
        title = "Hardware-Grade Security",
        subtitle = "You Own Your Vault. Truly.",
        description = "Guarded with Android KeyStore enclave encryption and biometrics. Grant time-limited, revocable clinical access to family or physicians with immutable audit logging.",
        icon = Icons.Default.Security,
        badgeText = "PATIENT-SOVEREIGN ENCLAVE",
        featureBadges = listOf(
          Pair(Icons.Default.Fingerprint, "Biometric & Passkey Unlock"),
          Pair(Icons.Default.Share, "Time-Limited Care Grants"),
          Pair(Icons.AutoMirrored.Filled.FactCheck, "Immutable Cryptographic Audit")
        ),
        gradientColors = listOf(VitalEmeraldSuccess, VitalCyanAccent)
      )
    )
  }

  val pagerState = rememberPagerState(pageCount = { slides.size })

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            ObsidianNavyDark,
            VitalNavy,
            VitalNavyDark
          )
        )
      )
      .statusBarsPadding()
      .navigationBarsPadding()
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 24.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Top Navigation: Logo & Skip
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(VitalTealContainerDark.copy(alpha = 0.6f))
              .border(1.dp, VitalTealPrimaryDark.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Shield,
              contentDescription = null,
              tint = VitalCyanAccent,
              modifier = Modifier.size(20.dp)
            )
          }
          Text(
            text = "VITALITH",
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            color = Color.White
          )
        }

        TextButton(
          onClick = { onFinishOnboarding(false) },
          modifier = Modifier.testTag("onboarding_skip_button")
        ) {
          Text(
            text = "Skip Tour",
            color = VitalTealPrimaryDark,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Pager for slides
      HorizontalPager(
        state = pagerState,
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .testTag("onboarding_pager")
      ) { page ->
        val slide = slides[page]
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          // Hero Icon Badge with Animated Gradient Glow
          Box(
            modifier = Modifier
              .size(110.dp)
              .clip(CircleShape)
              .background(
                Brush.radialGradient(
                  colors = listOf(
                    VitalTealContainerDark.copy(alpha = 0.8f),
                    VitalNavySurface.copy(alpha = 0.4f)
                  )
                )
              )
              .border(2.dp, Brush.linearGradient(slide.gradientColors), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = slide.icon,
              contentDescription = slide.title,
              tint = slide.gradientColors.first(),
              modifier = Modifier.size(54.dp)
            )
          }

          Spacer(modifier = Modifier.height(24.dp))

          // Category Pill
          Surface(
            color = VitalTealContainerDark.copy(alpha = 0.4f),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, VitalTealPrimaryDark.copy(alpha = 0.3f))
          ) {
            Text(
              text = slide.badgeText,
              color = VitalCyanAccent,
              fontSize = 10.sp,
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = 1.sp,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Headline
          Text(
            text = slide.title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(6.dp))

          // Subtitle
          Text(
            text = slide.subtitle,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = VitalTealPrimaryDark,
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(14.dp))

          // Body Description
          Text(
            text = slide.description,
            fontSize = 13.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
          )

          Spacer(modifier = Modifier.height(24.dp))

          // Feature Badges Card
          Surface(
            color = CardSurfaceDark.copy(alpha = 0.7f),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassWhiteBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              slide.featureBadges.forEach { (featIcon, featText) ->
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(28.dp)
                      .clip(CircleShape)
                      .background(VitalTealContainerDark.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = featIcon,
                      contentDescription = null,
                      tint = VitalCyanAccent,
                      modifier = Modifier.size(16.dp)
                    )
                  }
                  Text(
                    text = featText,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFE2E8F0)
                  )
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Dots Indicator
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        repeat(slides.size) { index ->
          val isSelected = pagerState.currentPage == index
          val width by animateDpAsState(
            targetValue = if (isSelected) 24.dp else 8.dp,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
            label = "dot_width"
          )
          Box(
            modifier = Modifier
              .height(8.dp)
              .width(width)
              .clip(CircleShape)
              .background(if (isSelected) VitalCyanAccent else Color.White.copy(alpha = 0.25f))
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Action Buttons
      val isLastSlide = pagerState.currentPage == slides.size - 1

      if (isLastSlide) {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Primary: Create Vault Account
          Button(
            onClick = { onFinishOnboarding(true) },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary),
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("onboarding_signup_button")
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White)
              Text("Create Encrypted Account", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
          }

          // Secondary: Log In to Existing Vault
          OutlinedButton(
            onClick = { onFinishOnboarding(false) },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = VitalCyanAccent),
            border = androidx.compose.foundation.BorderStroke(1.dp, VitalCyanAccent.copy(alpha = 0.6f)),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("onboarding_login_button")
          ) {
            Text("Already have a vault? Log In", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
          }
        }
      } else {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (pagerState.currentPage > 0) {
            IconButton(
              onClick = {
                coroutineScope.launch {
                  pagerState.animateScrollToPage(pagerState.currentPage - 1)
                }
              },
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(CardSurfaceDark)
                .testTag("onboarding_prev_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Previous",
                tint = Color.White
              )
            }
          } else {
            Spacer(modifier = Modifier.size(48.dp))
          }

          Button(
            onClick = {
              coroutineScope.launch {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
              }
            },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            modifier = Modifier
              .height(48.dp)
              .testTag("onboarding_next_button")
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text("Continue", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}
