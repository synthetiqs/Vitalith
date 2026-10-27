package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.example.data.model.ChatMessage
import com.example.data.repository.VitalithRepository
import com.example.ui.components.CitationPill
import com.example.ui.components.ClinicalAlertBanner
import com.example.ui.components.EditorialAiCard
import com.example.ui.components.FloatingComposer
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AiInterpreterScreen(
  repository: VitalithRepository,
  onNavigateToPrep: () -> Unit,
  modifier: Modifier = Modifier
) {
  val chatMessages by repository.chatMessages.collectAsState()
  var inputQuery by remember { mutableStateOf("") }
  val listState = rememberLazyListState()
  val coroutineScope = rememberCoroutineScope()

  LaunchedEffect(chatMessages.size) {
    if (chatMessages.isNotEmpty()) {
      listState.animateScrollToItem(chatMessages.size - 1)
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // Top Bar with Grounding Status & Disclaimer
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 2.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
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
                .background(VitalTealContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = null,
                tint = VitalTealPrimary,
                modifier = Modifier.size(20.dp)
              )
            }
            Column {
              Text(
                text = "Intelligent Interpreter",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
              )
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                  modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(VitalEmeraldSuccess)
                )
                Text(
                  text = "Grounded in Doug's Records (6 docs loaded)",
                  fontSize = 11.sp,
                  color = VitalEmeraldSuccess,
                  fontWeight = FontWeight.Medium
                )
              }
            }
          }

          IconButton(onClick = onNavigateToPrep) {
            Icon(
              imageVector = Icons.Default.AssignmentTurnedIn,
              contentDescription = "Appointment Prep",
              tint = VitalTealPrimary
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))
        ClinicalAlertBanner()
      }
    }

    // Chat Message Thread
    LazyColumn(
      state = listState,
      modifier = Modifier
        .weight(1f)
        .padding(horizontal = 16.dp),
      contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      items(chatMessages, key = { it.id }) { msg ->
        EditorialAiCard(message = msg)
      }
    }

    val suggestions = listOf(
      "Summarize my records & flag anomalies",
      "How is my A1c trending?",
      "Review my active medications & interactions",
      "Prepare summary for my next appointment",
      "Test emergency symptom triage (chest pain)"
    )

    // Floating Composer Bar with animated suggestion chips
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 76.dp) // Clearance for floating pill bottom bar
    ) {
      FloatingComposer(
        query = inputQuery,
        onQueryChange = { inputQuery = it },
        onSend = {
          if (inputQuery.isNotBlank()) {
            val q = inputQuery
            inputQuery = ""
            repository.sendUserMessage(q)
          }
        },
        suggestions = suggestions,
        onSelectSuggestion = { suggestion ->
          repository.sendUserMessage(suggestion)
        }
      )
    }
  }
}

@Composable
fun ChatBubble(msg: ChatMessage) {
  val isUser = msg.sender == "user"
  val isSystem = msg.sender == "system"

  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
  ) {
    if (msg.isRedFlag) {
      ClinicalAlertBanner(isRedFlag = true, customText = msg.redFlagEscalationText)
      Spacer(modifier = Modifier.height(6.dp))
    }

    Surface(
      color = when {
        isUser -> VitalTealPrimary
        isSystem -> MaterialTheme.colorScheme.surfaceVariant
        msg.isRedFlag -> VitalAlertContainer
        else -> MaterialTheme.colorScheme.surface
      },
      shape = RoundedCornerShape(
        topStart = 14.dp,
        topEnd = 14.dp,
        bottomStart = if (isUser) 14.dp else 2.dp,
        bottomEnd = if (isUser) 2.dp else 14.dp
      ),
      border = if (!isUser && !isSystem) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
      modifier = Modifier
        .widthIn(max = 320.dp)
        .testTag("chat_bubble_${msg.sender}")
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        if (!isUser) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = if (isSystem) Icons.Default.Info else Icons.Default.Verified,
              contentDescription = null,
              tint = if (isSystem) MaterialTheme.colorScheme.onSurfaceVariant else VitalTealPrimary,
              modifier = Modifier.size(14.dp)
            )
            Text(
              text = if (isSystem) "Vitalith Engine" else "Grounded Explainer",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = if (isSystem) MaterialTheme.colorScheme.onSurfaceVariant else VitalTealPrimary
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
        }

        Text(
          text = msg.text,
          fontSize = 13.sp,
          color = when {
            isUser -> Color.White
            msg.isRedFlag -> VitalOnAlertContainer
            else -> MaterialTheme.colorScheme.onSurface
          },
          lineHeight = 18.sp
        )

        // Citations List
        if (msg.citations.isNotEmpty()) {
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "Sources from your record (${msg.citations.size}):",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(4.dp))
          msg.citations.forEach { cit ->
            CitationPill(citation = cit, modifier = Modifier.padding(vertical = 2.dp))
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = msg.timestamp,
      fontSize = 9.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 4.dp)
    )
  }
}
