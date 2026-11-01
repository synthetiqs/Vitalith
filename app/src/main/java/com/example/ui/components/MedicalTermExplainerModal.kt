package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.util.GeminiMedicalExplainer
import com.example.util.MedicalTermExplanation
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalTermExplainerModal(
  term: String,
  clinicalContext: String = "",
  onDismiss: () -> Unit,
  onAskFollowUpInChat: ((String) -> Unit)? = null
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val coroutineScope = rememberCoroutineScope()

  var explanation by remember { mutableStateOf<MedicalTermExplanation?>(null) }
  var isLoading by remember { mutableStateOf(true) }
  var errorMsg by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(term) {
    isLoading = true
    errorMsg = null
    try {
      val result = GeminiMedicalExplainer.explainTerm(term, clinicalContext)
      explanation = result
    } catch (e: Exception) {
      errorMsg = "Unable to fetch explanation: ${e.message}"
    } finally {
      isLoading = false
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    modifier = Modifier.testTag("medical_term_explainer_modal")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 12.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Header: Gemini Badge & Term Title
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = VitalTealContainer,
            border = androidx.compose.foundation.BorderStroke(1.dp, VitalTealPrimary.copy(alpha = 0.4f))
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = VitalTealPrimary,
                modifier = Modifier.size(13.dp)
              )
              Text(
                text = "VITALITH AI MEDICAL INTERPRETER",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = VitalOnTealContainer,
                letterSpacing = 0.5.sp
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = term,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        IconButton(
          onClick = onDismiss,
          modifier = Modifier.testTag("close_explainer_modal")
        ) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }

      if (isLoading) {
        // Shimmer / Loading State
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            CircularProgressIndicator(
              color = VitalTealPrimary,
              modifier = Modifier.size(32.dp),
              strokeWidth = 3.dp
            )
            Text(
              text = "Consulting Vitalith AI Clinical Knowledge...",
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold,
              color = VitalTealPrimary
            )
            Text(
              text = "Synthesizing patient-friendly medical explanation for '$term'",
              fontSize = 11.5.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      } else if (errorMsg != null) {
        Card(
          colors = CardDefaults.cardColors(containerColor = VitalAlertContainer),
          shape = RoundedCornerShape(12.dp)
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = VitalAlertRed)
            Text(text = errorMsg ?: "Error", fontSize = 12.sp, color = VitalOnAlertContainer)
          }
        }
      } else if (explanation != null) {
        val exp = explanation!!

        // Section 1: In Plain English
        Card(
          colors = CardDefaults.cardColors(containerColor = VitalTealContainer.copy(alpha = 0.4f)),
          shape = RoundedCornerShape(14.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, VitalTealPrimary.copy(alpha = 0.3f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Default.Lightbulb, contentDescription = null, tint = VitalTealPrimary, modifier = Modifier.size(18.dp))
              Text(
                text = "In Plain English",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = VitalOnTealContainer
              )
            }
            Text(
              text = exp.simpleDefinition,
              fontSize = 13.5.sp,
              lineHeight = 19.sp,
              color = VitalOnTealContainer
            )
          }
        }

        // Section 2: Clinical Significance
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = RoundedCornerShape(14.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Default.MedicalServices, contentDescription = null, tint = VitalTealPrimary, modifier = Modifier.size(18.dp))
              Text(
                text = "Why Your Doctor Ordered / Evaluated This",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
              )
            }
            Text(
              text = exp.clinicalSignificance,
              fontSize = 12.5.sp,
              lineHeight = 18.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Section 3: Reference Range Guidance
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = RoundedCornerShape(14.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Default.Speed, contentDescription = null, tint = VitalWarningOrange, modifier = Modifier.size(18.dp))
              Text(
                text = "Understanding Normal vs. Abnormal Results",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
              )
            }
            Text(
              text = exp.referenceRangeGuidance,
              fontSize = 12.5.sp,
              lineHeight = 18.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Section 4: Questions To Ask Your Doctor
        if (exp.questionsToAskDoctor.isNotEmpty()) {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.QuestionAnswer, contentDescription = null, tint = VitalTealPrimary, modifier = Modifier.size(18.dp))
                Text(
                  text = "Suggested Questions for Your Next Visit",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold
                )
              }

              exp.questionsToAskDoctor.forEachIndexed { idx, q ->
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.Top
                ) {
                  Text(
                    text = "${idx + 1}.",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = VitalTealPrimary
                  )
                  Text(
                    text = q,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                  )
                }
              }
            }
          }
        }

        // Model & Provenance Tag
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = VitalEmeraldSuccess, modifier = Modifier.size(14.dp))
            Text(
              text = "Grounded via ${exp.modelUsed}",
              fontSize = 10.5.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          TextButton(
            onClick = {
              val fullCopy = "${exp.term}\n\nDefinition:\n${exp.simpleDefinition}\n\nClinical Significance:\n${exp.clinicalSignificance}\n\nReference Ranges:\n${exp.referenceRangeGuidance}\n\nQuestions for Doctor:\n" +
                exp.questionsToAskDoctor.mapIndexed { i, q -> "${i + 1}. $q" }.joinToString("\n")
              clipboardManager.setText(AnnotatedString(fullCopy))
              Toast.makeText(context, "Explanation copied to clipboard!", Toast.LENGTH_SHORT).show()
            }
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Copy", fontSize = 11.sp)
          }
        }

        // Action Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          if (onAskFollowUpInChat != null) {
            OutlinedButton(
              onClick = {
                onDismiss()
                onAskFollowUpInChat("Can you explain how $term relates to my other health records?")
              },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Ask Follow-Up", fontSize = 12.sp)
            }
          }

          Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Text("Got It", fontWeight = FontWeight.Bold)
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}
