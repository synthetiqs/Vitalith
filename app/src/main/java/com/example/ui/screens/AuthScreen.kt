package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.auth.AuthService
import com.example.data.auth.AuthUiState
import com.example.data.auth.AuthUser
import com.example.data.model.PatientProfile
import com.example.data.repository.VitalithRepository
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.UUID

enum class AuthMode {
  LOGIN,
  SIGN_UP
}

@Composable
fun AuthScreen(
  authService: AuthService,
  repository: VitalithRepository,
  onAuthSuccess: (AuthUser) -> Unit = {},
  onNavigateToOnboarding: () -> Unit = {},
  initialMode: AuthMode = AuthMode.LOGIN
) {
  val coroutineScope = rememberCoroutineScope()
  val focusManager = LocalFocusManager.current
  val uiState by authService.uiState.collectAsState()

  var currentMode by remember { mutableStateOf(initialMode) }
  var localErrorMessage by remember { mutableStateOf<String?>(null) }
  var isSubmitting by remember { mutableStateOf(false) }

  // Form Fields
  var fullName by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var confirmPassword by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var confirmPasswordVisible by remember { mutableStateOf(false) }
  var agreeToPrivacyTerms by remember { mutableStateOf(true) }

  val scrollState = rememberScrollState()

  // Password Strength Calculation
  val passwordStrength = remember(password) {
    var score = 0
    if (password.length >= 8) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { it.isUpperCase() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++
    score
  }

  val passwordStrengthLabel = when (passwordStrength) {
    0, 1 -> "Weak"
    2 -> "Fair"
    3 -> "Strong"
    else -> "Very Strong"
  }

  val passwordStrengthColor = when (passwordStrength) {
    0, 1 -> VitalAlertRed
    2 -> VitalWarningOrange
    3 -> VitalCyanAccent
    else -> VitalEmeraldSuccess
  }

  Box(
    modifier = Modifier
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
      .imePadding()
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(horizontal = 24.dp, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Top Navigation / Revisit Tour
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
              .size(32.dp)
              .clip(CircleShape)
              .background(VitalTealContainerDark.copy(alpha = 0.5f))
              .border(1.dp, VitalTealPrimaryDark, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Shield,
              contentDescription = null,
              tint = VitalCyanAccent,
              modifier = Modifier.size(18.dp)
            )
          }
          Text(
            text = "VITALITH VAULT",
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            color = Color.White
          )
        }

        TextButton(
          onClick = onNavigateToOnboarding,
          modifier = Modifier.testTag("auth_view_tour_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
            contentDescription = null,
            tint = VitalTealPrimaryDark,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("App Tour", fontSize = 12.sp, color = VitalTealPrimaryDark, fontWeight = FontWeight.SemiBold)
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Shield Logo & Brand Banner
      Box(
        modifier = Modifier
          .size(70.dp)
          .clip(CircleShape)
          .background(VitalTealContainerDark.copy(alpha = 0.5f))
          .border(2.dp, VitalTealPrimaryDark, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Shield,
          contentDescription = "Vitalith Vault Logo",
          tint = VitalCyanAccent,
          modifier = Modifier.size(38.dp)
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = if (currentMode == AuthMode.LOGIN) "Unlock Health Vault" else "Create Patient Vault",
        fontSize = 22.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Color.White
      )

      Text(
        text = if (currentMode == AuthMode.LOGIN)
          "Hardware-encrypted clinical timeline & lab provenance"
        else
          "Register a sovereign enclave for your medical history",
        fontSize = 12.5.sp,
        fontWeight = FontWeight.Medium,
        color = VitalTealPrimaryDark,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(18.dp))

      // Mode Switcher Tabs (Log In vs Sign Up)
      Surface(
        color = CardSurfaceDark,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassWhiteBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          // Log In Tab
          Surface(
            onClick = {
              currentMode = AuthMode.LOGIN
              localErrorMessage = null
            },
            color = if (currentMode == AuthMode.LOGIN) VitalTealPrimary else Color.Transparent,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("auth_tab_login")
          ) {
            Text(
              text = "Log In",
              color = if (currentMode == AuthMode.LOGIN) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
              fontWeight = if (currentMode == AuthMode.LOGIN) FontWeight.Bold else FontWeight.Medium,
              fontSize = 13.5.sp,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(vertical = 10.dp)
            )
          }

          // Sign Up Tab
          Surface(
            onClick = {
              currentMode = AuthMode.SIGN_UP
              localErrorMessage = null
            },
            color = if (currentMode == AuthMode.SIGN_UP) VitalTealPrimary else Color.Transparent,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("auth_tab_signup")
          ) {
            Text(
              text = "Register / Sign Up",
              color = if (currentMode == AuthMode.SIGN_UP) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
              fontWeight = if (currentMode == AuthMode.SIGN_UP) FontWeight.Bold else FontWeight.Medium,
              fontSize = 13.5.sp,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(vertical = 10.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Error Banner
      val displayError = localErrorMessage ?: (uiState as? AuthUiState.Error)?.message
      if (displayError != null) {
        Surface(
          color = VitalAlertContainer,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("auth_error_banner")
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.ErrorOutline,
              contentDescription = null,
              tint = VitalAlertRed,
              modifier = Modifier.size(20.dp)
            )
            Text(
              text = displayError,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = VitalOnAlertContainer,
              modifier = Modifier.weight(1f)
            )
            IconButton(
              onClick = { localErrorMessage = null },
              modifier = Modifier.size(24.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dismiss",
                tint = VitalAlertRed,
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(14.dp))
      }

      // Main Form Card
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = CardSurfaceDark,
        tonalElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassWhiteBorder)
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          if (currentMode == AuthMode.SIGN_UP) {
            // Full Name Input
            OutlinedTextField(
              value = fullName,
              onValueChange = { fullName = it },
              label = { Text("Full Legal / Preferred Name", color = Color(0xFFCBD5E1)) },
              leadingIcon = {
                Icon(Icons.Default.Person, contentDescription = null, tint = VitalCyanAccent)
              },
              singleLine = true,
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
              keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = VitalCyanAccent,
                unfocusedBorderColor = GlassWhiteBorder
              ),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_fullname_input")
            )
          }

          // Email Address Input
          OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Clinical Email Address", color = Color(0xFFCBD5E1)) },
            leadingIcon = {
              Icon(Icons.Default.Email, contentDescription = null, tint = VitalCyanAccent)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
              keyboardType = KeyboardType.Email,
              imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White,
              focusedBorderColor = VitalCyanAccent,
              unfocusedBorderColor = GlassWhiteBorder
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("auth_email_input")
          )

          // Password Input
          OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Vault Passcode / Password", color = Color(0xFFCBD5E1)) },
            leadingIcon = {
              Icon(Icons.Default.Lock, contentDescription = null, tint = VitalCyanAccent)
            },
            trailingIcon = {
              IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                  imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                  contentDescription = if (passwordVisible) "Hide password" else "Show password",
                  tint = Color(0xFF94A3B8)
                )
              }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
              keyboardType = KeyboardType.Password,
              imeAction = if (currentMode == AuthMode.SIGN_UP) ImeAction.Next else ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
              onNext = { focusManager.moveFocus(FocusDirection.Down) },
              onDone = { focusManager.clearFocus() }
            ),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White,
              focusedBorderColor = VitalCyanAccent,
              unfocusedBorderColor = GlassWhiteBorder
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("auth_password_input")
          )

          // In Sign Up mode: Password strength & confirm password
          if (currentMode == AuthMode.SIGN_UP) {
            // Live Password Strength Meter
            if (password.isNotEmpty()) {
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(text = "Security Strength: $passwordStrengthLabel", fontSize = 11.sp, color = passwordStrengthColor, fontWeight = FontWeight.Bold)
                  Text(text = "Min. 8 characters", fontSize = 11.sp, color = Color(0xFF94A3B8))
                }
                LinearProgressIndicator(
                  progress = { (passwordStrength.coerceIn(1, 4) / 4f) },
                  color = passwordStrengthColor,
                  trackColor = Color.White.copy(alpha = 0.1f),
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                )
              }
            }

            // Confirm Password Input
            OutlinedTextField(
              value = confirmPassword,
              onValueChange = { confirmPassword = it },
              label = { Text("Confirm Password", color = Color(0xFFCBD5E1)) },
              leadingIcon = {
                Icon(Icons.Default.LockClock, contentDescription = null, tint = VitalCyanAccent)
              },
              trailingIcon = {
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                  Icon(
                    imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8)
                  )
                }
              },
              visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
              singleLine = true,
              keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
              ),
              keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = if (confirmPassword.isNotEmpty() && confirmPassword != password) VitalAlertRed else VitalCyanAccent,
                unfocusedBorderColor = GlassWhiteBorder
              ),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_confirm_password_input")
            )

            // Password match warning
            if (confirmPassword.isNotEmpty() && confirmPassword != password) {
              Text(
                text = "Passwords do not match",
                color = VitalAlertRed,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
              )
            }

            // HIPAA Privacy Agreement Checkbox
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .clickable { agreeToPrivacyTerms = !agreeToPrivacyTerms }
            ) {
              Checkbox(
                checked = agreeToPrivacyTerms,
                onCheckedChange = { agreeToPrivacyTerms = it },
                colors = CheckboxDefaults.colors(checkedColor = VitalTealPrimary)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "I consent to hardware-encrypted on-device storage under HIPAA patient-sovereign rules.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 15.sp
              )
            }
          }

          Spacer(modifier = Modifier.height(4.dp))

          // Primary Submission Button
          Button(
            onClick = {
              localErrorMessage = null
              focusManager.clearFocus()

              if (currentMode == AuthMode.SIGN_UP) {
                // Validation for registration
                if (fullName.trim().length < 2) {
                  localErrorMessage = "Please enter your full legal or preferred name."
                  return@Button
                }
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                  localErrorMessage = "Please enter a valid clinical email address."
                  return@Button
                }
                if (password.length < 8) {
                  localErrorMessage = "Password must be at least 8 characters long."
                  return@Button
                }
                if (password != confirmPassword) {
                  localErrorMessage = "Password and confirmation do not match."
                  return@Button
                }
                if (!agreeToPrivacyTerms) {
                  localErrorMessage = "Please agree to the patient enclave terms."
                  return@Button
                }

                isSubmitting = true
                val regResult = authService.registerUser(fullName, email, password)
                isSubmitting = false
                regResult.onSuccess { user ->
                  // Also create PatientProfile and UserProfile
                  val updatedUser = repository.userProfile.value.copy(
                    fullName = user.displayName ?: fullName,
                    email = user.email ?: email
                  )
                  repository.updateUserProfile(updatedUser)
                  repository.addPatientProfile(
                    PatientProfile(
                      id = "profile_${UUID.randomUUID().toString().take(6)}",
                      name = user.displayName ?: fullName,
                      relationship = "Primary Account (Self)",
                      dob = "1990-01-01",
                      bloodType = "O+",
                      mrn = "VTL-${(1000..9999).random()}",
                      isPrimary = true
                    )
                  )
                  repository.recordAudit("USER_REGISTERED", "Created hardware-encrypted vault account for ${user.email}")
                  onAuthSuccess(user)
                }.onFailure { ex ->
                  localErrorMessage = ex.localizedMessage ?: "Registration failed."
                }
              } else {
                // Validation for login
                if (email.isBlank() || password.isBlank()) {
                  localErrorMessage = "Please enter both email and password."
                  return@Button
                }

                isSubmitting = true
                val loginResult = authService.signInWithEmailPassword(email, password)
                isSubmitting = false
                loginResult.onSuccess { user ->
                  repository.updateUserProfile(
                    repository.userProfile.value.copy(
                      fullName = user.displayName ?: "Verified Patient",
                      email = user.email ?: email
                    )
                  )
                  repository.recordAudit("USER_LOGIN", "Authenticated session unlocked for ${user.email}")
                  onAuthSuccess(user)
                }.onFailure { ex ->
                  localErrorMessage = ex.localizedMessage ?: "Login failed. Check your credentials."
                }
              }
            },
            enabled = !isSubmitting,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary),
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("auth_submit_button")
          ) {
            if (isSubmitting) {
              CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  imageVector = if (currentMode == AuthMode.SIGN_UP) Icons.Default.LockOpen else Icons.Default.Key,
                  contentDescription = null,
                  tint = Color.White
                )
                Text(
                  text = if (currentMode == AuthMode.SIGN_UP) "Create Encrypted Vault" else "Unlock Health Vault",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
              }
            }
          }

          // Interactive Google Sign-In Option
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = GlassWhiteBorder)
            Text(
              text = "OR SIGN IN WITH",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = GlassWhiteBorder)
          }

          OutlinedButton(
            onClick = {
              localErrorMessage = null
              isSubmitting = true
              coroutineScope.launch {
                val res = authService.signInWithGoogle()
                isSubmitting = false
                res.onSuccess { user ->
                  repository.updateUserProfile(
                    repository.userProfile.value.copy(
                      fullName = user.displayName ?: "Verified Patient",
                      email = user.email ?: ""
                    )
                  )
                  repository.recordAudit("GOOGLE_SIGN_IN", "Authenticated with Google ID for ${user.email}")
                  onAuthSuccess(user)
                }.onFailure { ex ->
                  if (ex !is androidx.credentials.exceptions.GetCredentialCancellationException) {
                    localErrorMessage = ex.localizedMessage ?: "Google Sign-In failed."
                  }
                }
              }
            },
            enabled = !isSubmitting,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassWhiteBorder),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("google_signin_button")
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Google Sign In",
                tint = VitalCyanAccent,
                modifier = Modifier.size(20.dp)
              )
              Text("Google Sign-In (CredentialManager)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
          }

          // Quick Clinical Demo Accounts Section
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = GlassWhiteBorder)
            Text(
              text = "CLINICAL EVALUATOR DEMO",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = GlassWhiteBorder)
          }

          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Surface(
              onClick = {
                val res = authService.signInDemoUser("abydouglas@gmail.com")
                res.onSuccess { user ->
                  repository.updateUserProfile(
                    repository.userProfile.value.copy(
                      fullName = "Doug Adarkwah",
                      email = "abydouglas@gmail.com"
                    )
                  )
                  repository.recordAudit("DEMO_SIGN_IN", "Demo evaluator unlocked Doug Adarkwah profile")
                  onAuthSuccess(user)
                }
              },
              shape = RoundedCornerShape(10.dp),
              color = VitalTealContainerDark.copy(alpha = 0.35f),
              border = androidx.compose.foundation.BorderStroke(1.dp, VitalTealPrimaryDark.copy(alpha = 0.4f)),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_demo_doug")
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(VitalTealPrimary),
                  contentAlignment = Alignment.Center
                ) {
                  Text("DA", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f)) {
                  Text("Doug Adarkwah (Primary Patient)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                  Text("abydouglas@gmail.com • Type 2 Pre-Diabetes & Cardiology", fontSize = 10.sp, color = VitalTealPrimaryDark)
                }
                Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, tint = VitalTealPrimaryDark, modifier = Modifier.size(16.dp))
              }
            }

            Surface(
              onClick = {
                val res = authService.signInDemoUser("sarah.jenkins@familyhealth.org")
                res.onSuccess { user ->
                  repository.updateUserProfile(
                    repository.userProfile.value.copy(
                      fullName = "Sarah Jenkins",
                      email = "sarah.jenkins@familyhealth.org"
                    )
                  )
                  repository.recordAudit("DEMO_SIGN_IN", "Demo evaluator unlocked Sarah Jenkins profile")
                  onAuthSuccess(user)
                }
              },
              shape = RoundedCornerShape(10.dp),
              color = VitalNavySurface.copy(alpha = 0.4f),
              border = androidx.compose.foundation.BorderStroke(1.dp, GlassWhiteBorder),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_demo_sarah")
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF6366F1)),
                  contentAlignment = Alignment.Center
                ) {
                  Text("SJ", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f)) {
                  Text("Sarah Jenkins (Family Caregiver)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                  Text("sarah.jenkins@familyhealth.org • Pediatric Proxy Access", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(16.dp))
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Bottom Security Disclaimer
      Text(
        text = "Vitalith Vault encrypts all records with on-device hardware keys. Your clinical records remain strictly in your custody under zero-knowledge architecture.",
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        textAlign = TextAlign.Center,
        lineHeight = 16.sp,
        modifier = Modifier.padding(horizontal = 12.dp)
      )

      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}
