package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.VitalithRepository
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SubscriptionScreen(
  repository: VitalithRepository,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val currentTier by repository.subscriptionTier.collectAsState()
  val subscriptionDetails by repository.subscriptionDetails.collectAsState()
  val paymentHistory by repository.paymentHistory.collectAsState()

  var selectedBillingCycle by remember { mutableStateOf(BillingCycle.ANNUAL) }
  var pendingCheckoutTier by remember { mutableStateOf<SubscriptionTier?>(null) }
  var completedTransaction by remember { mutableStateOf<PaymentTransaction?>(null) }
  var showCancelAutoRenewConfirm by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Membership & Subscription",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Clinical AI, family enclaves & encrypted vault features",
              fontSize = 11.5.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("subscription_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back to Settings",
              tint = MaterialTheme.colorScheme.onSurface
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    modifier = modifier.fillMaxSize()
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .background(MaterialTheme.colorScheme.background),
      contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // 1. Current Active Plan Hero Card
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = androidx.compose.foundation.BorderStroke(1.5.dp, VitalTealPrimary.copy(alpha = 0.6f)),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("subscription_active_plan_card")
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                      Brush.linearGradient(
                        listOf(VitalTealPrimary, VitalCyanAccent)
                      )
                    ),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                  )
                }
                Column {
                  Text(
                    text = "CURRENT ACTIVE MEMBERSHIP",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = VitalTealPrimary,
                    letterSpacing = 0.5.sp
                  )
                  Text(
                    text = currentTier.displayName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }
              }

              Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (subscriptionDetails.status == "ACTIVE") VitalEmeraldContainer else VitalAlertContainer
              ) {
                Text(
                  text = if (subscriptionDetails.status == "ACTIVE") "ACTIVE" else "CANCELED",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = if (subscriptionDetails.status == "ACTIVE") VitalOnEmeraldContainer else VitalAlertRed,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
              }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Plan details row
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(text = "Billing Interval", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = subscriptionDetails.billingCycle.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
              }
              Column(horizontalAlignment = Alignment.End) {
                Text(text = "Next Renewal", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = subscriptionDetails.renewalDate, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
              }
            }

            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  imageVector = if (subscriptionDetails.lastPaymentMethod.contains("Google Pay", ignoreCase = true)) {
                    Icons.Default.AccountBalanceWallet
                  } else {
                    Icons.Default.CreditCard
                  },
                  contentDescription = null,
                  tint = VitalTealPrimary,
                  modifier = Modifier.size(16.dp)
                )
                Column {
                  Text(text = "Default Payment Method", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Text(text = subscriptionDetails.lastPaymentMethod, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                }
              }
            }

            // Auto-renew management
            if (currentTier != SubscriptionTier.FREE) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                  .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Icon(
                    imageVector = if (subscriptionDetails.autoRenew) Icons.Default.Autorenew else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (subscriptionDetails.autoRenew) VitalEmeraldSuccess else VitalAlertRed,
                    modifier = Modifier.size(16.dp)
                  )
                  Text(
                    text = if (subscriptionDetails.autoRenew) "Auto-Renew is Enabled" else "Cancels at end of billing period",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                  )
                }

                if (subscriptionDetails.autoRenew) {
                  TextButton(
                    onClick = { showCancelAutoRenewConfirm = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.testTag("subscription_cancel_autorenew_button")
                  ) {
                    Text("Turn Off", fontSize = 11.5.sp, color = VitalAlertRed, fontWeight = FontWeight.Bold)
                  }
                } else {
                  Button(
                    onClick = { repository.resumeSubscriptionAutoRenew() },
                    colors = ButtonDefaults.buttonColors(containerColor = VitalEmeraldSuccess),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.testTag("subscription_resume_autorenew_button")
                  ) {
                    Text("Resume Auto-Renew", fontSize = 11.5.sp, color = Color.White, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }
        }
      }

      // 2. Billing Cycle Toggle
      item {
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = "CHOOSE BILLING CADENCE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.5.sp
          )

          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
          ) {
            Row(
              modifier = Modifier.padding(4.dp),
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              FilterChip(
                selected = selectedBillingCycle == BillingCycle.MONTHLY,
                onClick = { selectedBillingCycle = BillingCycle.MONTHLY },
                label = { Text("Monthly Billing", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = VitalTealPrimary,
                  selectedLabelColor = Color.White
                ),
                modifier = Modifier.testTag("billing_cycle_monthly_button")
              )

              FilterChip(
                selected = selectedBillingCycle == BillingCycle.ANNUAL,
                onClick = { selectedBillingCycle = BillingCycle.ANNUAL },
                label = {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Annual (Save 25%)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Surface(
                      color = VitalEmeraldSuccess,
                      shape = RoundedCornerShape(4.dp)
                    ) {
                      Text(
                        text = "SAVE 25%",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                      )
                    }
                  }
                },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = VitalTealPrimary,
                  selectedLabelColor = Color.White
                ),
                modifier = Modifier.testTag("billing_cycle_annual_button")
              )
            }
          }
        }
      }

      // 3. Plan Tiers Comparison
      items(SubscriptionTier.values()) { tier ->
        val isCurrent = tier == currentTier
        val isAnnual = selectedBillingCycle == BillingCycle.ANNUAL
        val priceDisplay = if (tier == SubscriptionTier.FREE) {
          "$0 / month"
        } else if (isAnnual) {
          "$${tier.annualPricePerMonth} / month"
        } else {
          "$${tier.monthlyPrice} / month"
        }

        val billedFrequencyNote = when {
          tier == SubscriptionTier.FREE -> "Free forever • Standard offline vault"
          isAnnual -> "Billed annually as $${String.format(Locale.US, "%.2f", tier.annualPricePerMonth * 12)}/year"
          else -> "Billed monthly • Cancel anytime"
        }

        val isRecommended = tier == SubscriptionTier.PREMIUM

        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) VitalTealContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
          ),
          border = androidx.compose.foundation.BorderStroke(
            if (isCurrent) 2.dp else if (isRecommended) 1.5.dp else 1.dp,
            if (isCurrent) VitalTealPrimary else if (isRecommended) VitalTealPrimary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
          ),
          elevation = CardDefaults.cardElevation(defaultElevation = if (isRecommended || isCurrent) 3.dp else 1.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("subscription_tier_card_${tier.name.lowercase()}")
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Text(
                    text = tier.displayName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  if (isRecommended) {
                    Surface(
                      color = VitalTealPrimary,
                      shape = RoundedCornerShape(4.dp)
                    ) {
                      Text(
                        text = "MOST POPULAR",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                      )
                    }
                  } else if (tier == SubscriptionTier.PREMIUM_PLUS) {
                    Surface(
                      color = VitalGoldAccent,
                      shape = RoundedCornerShape(4.dp)
                    ) {
                      Text(
                        text = "MAXIMUM ENCLAVE",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                      )
                    }
                  }
                }
                Text(
                  text = tier.subtitle,
                  fontSize = 11.5.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            // Price Banner
            Row(
              verticalAlignment = Alignment.Bottom,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = priceDisplay,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = VitalTealPrimary
              )
              Text(
                text = billedFrequencyNote,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 3.dp)
              )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

            // Feature Checklist
            val features = when (tier) {
              SubscriptionTier.FREE -> listOf(
                "1 Primary Patient Profile",
                "Encrypted On-Device Room SQLite Vault",
                "5 Vitalith Clinical Intelligence Queries / day",
                "2 Document OCR Extractions / month",
                "Emergency Medical ID lock screen snapshot",
                "Standard PDF & CSV Data Export"
              )
              SubscriptionTier.PREMIUM -> listOf(
                "Up to 5 Family Patient Profiles (Kids, Spouse, Elderly)",
                "Unlimited Vitalith AI Clinical Consultations",
                "Unlimited Multimodal OCR Document Auto-Fill",
                "SMART on FHIR Automated Health Portal Sync",
                "All 9 High-Contrast & M3 Clinical Theme Palettes",
                "Unlimited Medication Reminders & Refill Alerts",
                "5 Cryptographic Care-Team Link Grants"
              )
              SubscriptionTier.PREMIUM_PLUS -> listOf(
                "Unlimited Family & Dependent Patient Profiles",
                "Priority Vitalith AI Multimodal OCR Queue",
                "Unlimited Cryptographic Care-Team Link Grants",
                "Complete Cryptographic Provenance Audit Trail (Merkle)",
                "Automated Drug-Drug Interaction Telemetry",
                "Comprehensive HL7 FHIR R4 JSON & PDF Bundle Archives",
                "Dedicated Hardware Keystore TEE StrongBox Backup"
              )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              features.forEach { feature ->
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = VitalEmeraldSuccess,
                    modifier = Modifier.size(16.dp)
                  )
                  Text(
                    text = feature,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 16.sp
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action / Upgrade Button
            if (isCurrent) {
              OutlinedButton(
                onClick = {},
                enabled = false,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Your Current Active Plan", fontWeight = FontWeight.Bold)
              }
            } else {
              Button(
                onClick = {
                  if (tier == SubscriptionTier.FREE) {
                    repository.setSubscriptionTier(SubscriptionTier.FREE)
                  } else {
                    pendingCheckoutTier = tier
                  }
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (isRecommended) VitalTealPrimary else MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("subscription_tier_select_${tier.name.lowercase()}")
              ) {
                Icon(
                  imageVector = if (tier == SubscriptionTier.FREE) Icons.Default.ArrowDownward else Icons.Default.ShoppingCartCheckout,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = if (tier == SubscriptionTier.FREE) "Downgrade to Free Core" else "Upgrade to ${tier.displayName}",
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.5.sp
                )
              }
            }
          }
        }
      }

      // 4. Feature Access & Limits Matrix Summary
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              text = "Feature Entitlements Matrix",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Access is enforced dynamically based on your active plan tier.",
              fontSize = 11.5.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

            val entitlements = listOf(
              Triple("Family Patient Profiles", "1 (Self)", "Up to 5 • Unlimited on Plus+"),
              Triple("Vitalith Clinical Ingestion", "5 / day", "Unlimited High-Speed"),
              Triple("Automated OCR Auto-Fill", "2 / month", "Unlimited Full Panels"),
              Triple("SMART on FHIR System Sync", "Manual Only", "Automated Real-Time Sync"),
              Triple("Care-Team Provider Links", "1 Active Link", "5 to Unlimited Links")
            )

            entitlements.forEach { (feature, freeVal, premiumVal) ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(text = feature, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                  Text(text = "Free: $freeVal", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = VitalTealContainer
                ) {
                  Text(
                    text = premiumVal,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = VitalOnTealContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }
              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            }
          }
        }
      }

      // 5. Payment Transaction Invoices & History
      if (paymentHistory.isNotEmpty()) {
        item {
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(text = "Payment & Billing History", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Icon(imageVector = Icons.Default.Receipt, contentDescription = null, tint = VitalTealPrimary, modifier = Modifier.size(18.dp))
              }

              paymentHistory.take(5).forEach { txn ->
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                  border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    // Row 1: Plan Tier & Amount + Status Badge (shifts COMPLETED to next line if space is limited)
                    ResponsivePaymentStatusRow(
                      tierContent = {
                        Text(
                          text = "${txn.tier.displayName} (${txn.billingCycle.label})",
                          fontSize = 13.sp,
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.onSurface
                        )
                      },
                      amountContent = {
                        Text(
                          text = "$${String.format(Locale.US, "%.2f", txn.amountPaid)}",
                          fontSize = 14.sp,
                          fontWeight = FontWeight.ExtraBold,
                          color = MaterialTheme.colorScheme.onSurface
                        )
                      },
                      badgeContent = {
                        Surface(
                          shape = RoundedCornerShape(4.dp),
                          color = VitalEmeraldContainer,
                          modifier = Modifier.testTag("payment_status_badge_${txn.id}")
                        ) {
                          Text(
                            text = txn.status,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = VitalOnEmeraldContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                          )
                        }
                      },
                      horizontalSpacing = 6.dp,
                      verticalSpacing = 2.dp
                    )

                    // Row 2: Date & Invoice ID
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Text(
                        text = txn.date,
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                      Text(
                        text = "Invoice: ${txn.invoiceNumber}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = VitalTealPrimary,
                        fontWeight = FontWeight.Medium
                      )
                    }

                    // Row 3: Shifted to the next line — Formatted Payment Method
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = MaterialTheme.colorScheme.surface,
                      border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                      ) {
                        Icon(
                          imageVector = if (txn.paymentMethod.contains("Google Pay", ignoreCase = true)) {
                            Icons.Default.AccountBalanceWallet
                          } else {
                            Icons.Default.CreditCard
                          },
                          contentDescription = null,
                          tint = VitalTealPrimary,
                          modifier = Modifier.size(15.dp)
                        )
                        Text(
                          text = txn.paymentMethod,
                          fontSize = 11.5.sp,
                          fontWeight = FontWeight.SemiBold,
                          color = MaterialTheme.colorScheme.onSurface
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

      // 6. Security, Encryption & Guarantee Banner
      item {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = VitalNavy,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Shield,
              contentDescription = null,
              tint = VitalTealPrimary,
              modifier = Modifier.size(24.dp)
            )
            Column {
              Text(
                text = "HIPAA Security Rule & 256-Bit Encrypted Payments",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color.White
              )
              Text(
                text = "Payments are tokenized with Android Keystore hardware cryptography. 30-day money-back guarantee. Zero health telemetry is ever linked or shared.",
                fontSize = 10.5.sp,
                color = Color(0xFFCBD5E1),
                lineHeight = 14.sp
              )
            }
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }

  // Interactive Payment Sheet Modal
  pendingCheckoutTier?.let { tierToBuy ->
    SubscriptionCheckoutDialog(
      tier = tierToBuy,
      billingCycle = selectedBillingCycle,
      onDismiss = { pendingCheckoutTier = null },
      onPaymentSuccess = { txn ->
        completedTransaction = txn
        pendingCheckoutTier = null
      },
      repository = repository
    )
  }

  // Payment Confirmation & Receipt Dialog
  completedTransaction?.let { txn ->
    AlertDialog(
      onDismissRequest = { completedTransaction = null },
      icon = {
        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(VitalEmeraldSuccess),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
      },
      title = {
        Text(
          text = "Subscription Activated!",
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Welcome to ${txn.tier.displayName}! All plan entitlements and family health vaults have been unlocked immediately.",
            fontSize = 12.5.sp,
            color = MaterialTheme.colorScheme.onSurface
          )

          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(text = "TRANSACTION RECEIPT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = VitalTealPrimary)
              Text(text = "Amount Paid: $${String.format(Locale.US, "%.2f", txn.amountPaid)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              Text(text = "Method: ${txn.paymentMethod}", fontSize = 11.sp)
              Text(text = "Transaction ID: ${txn.id}", fontSize = 10.5.sp, fontFamily = FontFamily.Monospace)
              Text(text = "Invoice Number: ${txn.invoiceNumber}", fontSize = 10.5.sp, fontFamily = FontFamily.Monospace)
              Text(text = "Date: ${txn.date}", fontSize = 11.sp)
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = { completedTransaction = null },
          colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary),
          modifier = Modifier.testTag("subscription_receipt_done_button")
        ) {
          Text("Done & Explore Features", color = Color.White, fontWeight = FontWeight.Bold)
        }
      }
    )
  }

  // Confirm cancel auto-renew
  if (showCancelAutoRenewConfirm) {
    AlertDialog(
      onDismissRequest = { showCancelAutoRenewConfirm = false },
      title = { Text("Turn Off Auto-Renewal?", fontWeight = FontWeight.Bold) },
      text = {
        Text(
          "Your ${currentTier.displayName} entitlements will stay fully active until ${subscriptionDetails.renewalDate}. You won't be charged again unless you manually renew.",
          fontSize = 13.sp
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            repository.cancelSubscriptionAutoRenew()
            showCancelAutoRenewConfirm = false
          }
        ) {
          Text("Confirm Cancel Auto-Renew", color = VitalAlertRed, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showCancelAutoRenewConfirm = false }) {
          Text("Keep Active")
        }
      }
    )
  }
}

/**
 * Interactive Checkout Dialog with Google Pay, Credit/Debit Card, and Hardware Keystore Tokenization.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubscriptionCheckoutDialog(
  tier: SubscriptionTier,
  billingCycle: BillingCycle,
  onDismiss: () -> Unit,
  onPaymentSuccess: (PaymentTransaction) -> Unit,
  repository: VitalithRepository
) {
  val isAnnual = billingCycle == BillingCycle.ANNUAL
  val amountDue = if (isAnnual) {
    tier.annualPricePerMonth * 12
  } else {
    tier.monthlyPrice
  }

  var selectedPaymentTab by remember { mutableIntStateOf(0) } // 0 = Google Pay, 1 = Card
  var cardholderName by remember { mutableStateOf("Doug Adarkwah") }
  var cardNumber by remember { mutableStateOf("4532 8912 3456 4242") }
  var cardExpiry by remember { mutableStateOf("08/29") }
  var cardCvv by remember { mutableStateOf("891") }
  var cardZip by remember { mutableStateOf("95113") }

  var isProcessing by remember { mutableStateOf(false) }
  var processingStatusText by remember { mutableStateOf("") }

  val coroutineScope = rememberCoroutineScope()

  AlertDialog(
    onDismissRequest = { if (!isProcessing) onDismiss() },
    title = {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.Lock, contentDescription = null, tint = VitalTealPrimary, modifier = Modifier.size(20.dp))
        Text("Secure Vault Checkout", fontWeight = FontWeight.Bold, fontSize = 17.sp)
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Order Summary Box
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = VitalTealContainerDark.copy(alpha = 0.25f),
          border = androidx.compose.foundation.BorderStroke(1.dp, VitalTealPrimary.copy(alpha = 0.3f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(text = "${tier.displayName} (${billingCycle.label})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
              Text(text = "$${String.format(Locale.US, "%.2f", amountDue)}", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = VitalTealPrimary)
            }
            if (isAnnual) {
              Text(
                text = "Includes 25% annual clinical membership discount",
                fontSize = 11.sp,
                color = VitalEmeraldSuccess,
                fontWeight = FontWeight.SemiBold
              )
            }
            Text(text = "Taxes & Vault Encryption: $0.00", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }

        if (isProcessing) {
          // Processing Animation
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              CircularProgressIndicator(color = VitalTealPrimary)
              Text(
                text = processingStatusText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }
        } else {
          // Payment Method Selector Tabs
          TabRow(
            selectedTabIndex = selectedPaymentTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = VitalTealPrimary,
            modifier = Modifier.fillMaxWidth()
          ) {
            Tab(
              selected = selectedPaymentTab == 0,
              onClick = { selectedPaymentTab = 0 },
              text = { Text("Google Pay", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
              selected = selectedPaymentTab == 1,
              onClick = { selectedPaymentTab = 1 },
              text = { Text("Credit / Debit Card", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
          }

          if (selectedPaymentTab == 0) {
            // Google Pay View
            Column(
              verticalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.padding(vertical = 4.dp)
            ) {
              Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(Icons.Default.Payment, contentDescription = null, tint = VitalTealPrimary)
                  Column {
                    Text("Linked Google Account", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("abydouglas@gmail.com • Visa •• 4242", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                }
              }

              Button(
                onClick = {
                  coroutineScope.launch {
                    isProcessing = true
                    processingStatusText = "Connecting to Google Pay API..."
                    delay(450)
                    processingStatusText = "Verifying Android Hardware Keystore token..."
                    delay(500)
                    processingStatusText = "Authorizing $${String.format(Locale.US, "%.2f", amountDue)}..."
                    delay(450)
                    val txn = repository.processPaymentAndActivateSubscription(
                      tier = tier,
                      billingCycle = billingCycle,
                      paymentMethod = "Google Pay (abydouglas@gmail.com)",
                      amount = amountDue
                    )
                    isProcessing = false
                    onPaymentSuccess(txn)
                  }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .testTag("checkout_google_pay_button")
              ) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pay with Google Pay", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
              }
            }
          } else {
            // Credit / Debit Card Form View
            Column(
              verticalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.padding(vertical = 2.dp)
            ) {
              OutlinedTextField(
                value = cardholderName,
                onValueChange = { cardholderName = it },
                label = { Text("Name on Card") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("checkout_card_name_input")
              )

              OutlinedTextField(
                value = cardNumber,
                onValueChange = { cardNumber = it },
                label = { Text("Card Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = {
                  Icon(Icons.Default.CreditCard, contentDescription = null, tint = VitalTealPrimary)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("checkout_card_number_input")
              )

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedTextField(
                  value = cardExpiry,
                  onValueChange = { cardExpiry = it },
                  label = { Text("MM/YY") },
                  singleLine = true,
                  modifier = Modifier.weight(1f).testTag("checkout_card_expiry_input")
                )
                OutlinedTextField(
                  value = cardCvv,
                  onValueChange = { cardCvv = it },
                  label = { Text("CVV") },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                  singleLine = true,
                  modifier = Modifier.weight(1f).testTag("checkout_card_cvv_input")
                )
                OutlinedTextField(
                  value = cardZip,
                  onValueChange = { cardZip = it },
                  label = { Text("ZIP") },
                  singleLine = true,
                  modifier = Modifier.weight(1f).testTag("checkout_card_zip_input")
                )
              }

              Button(
                onClick = {
                  coroutineScope.launch {
                    isProcessing = true
                    processingStatusText = "Encrypting card telemetry with AES-256..."
                    delay(500)
                    processingStatusText = "Verifying transaction with issuing bank..."
                    delay(550)
                    processingStatusText = "Authorizing $${String.format(Locale.US, "%.2f", amountDue)}..."
                    delay(400)
                    val last4 = cardNumber.takeLast(4).ifEmpty { "4242" }
                    val txn = repository.processPaymentAndActivateSubscription(
                      tier = tier,
                      billingCycle = billingCycle,
                      paymentMethod = "Visa ending in $last4",
                      amount = amountDue
                    )
                    isProcessing = false
                    onPaymentSuccess(txn)
                  }
                },
                colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .testTag("checkout_submit_card_button")
              ) {
                Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  "Pay $${String.format(Locale.US, "%.2f", amountDue)} & Subscribe",
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = {
      if (!isProcessing) {
        TextButton(onClick = onDismiss) {
          Text("Cancel")
        }
      }
    }
  )
}

/**
 * Responsive row for Payment & Billing history that places the Plan Tier, Amount, and
 * Status Badge ("COMPLETED") side-by-side if there is sufficient horizontal space;
 * otherwise, gracefully shifts "COMPLETED" to the next line with a very small line gap (2.dp).
 */
@Composable
private fun ResponsivePaymentStatusRow(
  tierContent: @Composable () -> Unit,
  amountContent: @Composable () -> Unit,
  badgeContent: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  horizontalSpacing: androidx.compose.ui.unit.Dp = 6.dp,
  verticalSpacing: androidx.compose.ui.unit.Dp = 2.dp
) {
  Layout(
    content = {
      Box(modifier = Modifier.wrapContentSize()) { tierContent() }
      Box(modifier = Modifier.wrapContentSize()) { amountContent() }
      Box(modifier = Modifier.wrapContentSize()) { badgeContent() }
    },
    modifier = modifier.fillMaxWidth()
  ) { measurables, constraints ->
    val horizontalSpacingPx = horizontalSpacing.roundToPx()
    val verticalSpacingPx = verticalSpacing.roundToPx()
    val minSpacingPx = 8.dp.roundToPx()
    val maxAvailableWidth = constraints.maxWidth

    val badgeMeasurable = measurables[2]
    val badgePlaceable = badgeMeasurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
    val badgeWidth = badgePlaceable.width

    val amountMeasurable = measurables[1]
    val amountPlaceable = amountMeasurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
    val amountWidth = amountPlaceable.width

    val tierMeasurable = measurables[0]
    val tierIntrinsicWidth = tierMeasurable.maxIntrinsicWidth(constraints.maxHeight)

    // Check if Tier + Amount + Badge can all fit side-by-side on a single line
    val canFitAllTogether = (tierIntrinsicWidth + minSpacingPx + amountWidth + horizontalSpacingPx + badgeWidth) <= maxAvailableWidth

    if (canFitAllTogether) {
      val maxTierWidth = (maxAvailableWidth - amountWidth - horizontalSpacingPx - badgeWidth - minSpacingPx).coerceAtLeast(0)
      val tierPlaceable = tierMeasurable.measure(
        constraints.copy(minWidth = 0, maxWidth = maxTierWidth)
      )
      val rowHeight = maxOf(tierPlaceable.height, amountPlaceable.height, badgePlaceable.height)
      layout(maxAvailableWidth, rowHeight) {
        tierPlaceable.placeRelative(0, (rowHeight - tierPlaceable.height) / 2)
        val amountX = maxAvailableWidth - badgeWidth - horizontalSpacingPx - amountWidth
        amountPlaceable.placeRelative(amountX, (rowHeight - amountPlaceable.height) / 2)
        badgePlaceable.placeRelative(maxAvailableWidth - badgeWidth, (rowHeight - badgePlaceable.height) / 2)
      }
    } else {
      // Shift "COMPLETED" to the next line with a very small line gap (2.dp)
      val maxTierWidth = (maxAvailableWidth - amountWidth - minSpacingPx).coerceAtLeast(0)
      val tierPlaceable = tierMeasurable.measure(
        constraints.copy(minWidth = 0, maxWidth = maxTierWidth)
      )
      val line1Height = maxOf(tierPlaceable.height, amountPlaceable.height)
      val totalHeight = line1Height + verticalSpacingPx + badgePlaceable.height

      layout(maxAvailableWidth, totalHeight) {
        // Line 1: Tier on left, Amount on right
        tierPlaceable.placeRelative(0, (line1Height - tierPlaceable.height) / 2)
        amountPlaceable.placeRelative(maxAvailableWidth - amountWidth, (line1Height - amountPlaceable.height) / 2)
        // Line 2: "COMPLETED" badge shifted to next line with small line gap
        badgePlaceable.placeRelative(maxAvailableWidth - badgeWidth, line1Height + verticalSpacingPx)
      }
    }
  }
}

