package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Verified
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
import com.example.ui.theme.*

/**
 * Editorial response card for AI Interpreter with expandable source drawers.
 */
@Composable
fun EditorialAiCard(
  message: ChatMessage,
  modifier: Modifier = Modifier
) {
  var isSourceDrawerExpanded by remember { mutableStateOf(false) }
  val isUser = message.sender == "user"

  if (isUser) {
    // User query bubble styled cleanly
    Box(
      modifier = modifier
        .fillMaxWidth()
        .padding(start = 48.dp, bottom = 4.dp),
      contentAlignment = Alignment.CenterEnd
    ) {
      Surface(
        color = VitalTealPrimary,
        shape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp),
        shadowElevation = 1.dp,
        modifier = Modifier.testTag("chat_bubble_user")
      ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
          Text(
            text = message.text,
            color = Color.White,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = message.timestamp,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 9.sp,
            modifier = Modifier.align(Alignment.End)
          )
        }
      }
    }
  } else {
    // Editorial AI Copilot Card
    Column(
      modifier = modifier
        .fillMaxWidth()
        .padding(end = 16.dp, bottom = 8.dp)
    ) {
      if (message.isRedFlag) {
        ClinicalAlertBanner(isRedFlag = true, customText = message.redFlagEscalationText)
        Spacer(modifier = Modifier.height(8.dp))
      }

      Surface(
        color = if (message.isRedFlag) VitalAlertContainer else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 2.dp,
        tonalElevation = 2.dp,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("chat_bubble_${message.sender}")
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          // Editorial Header
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
                  .size(28.dp)
                  .clip(CircleShape)
                  .background(VitalTealContainerDark.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Psychology,
                  contentDescription = null,
                  tint = VitalTealPrimaryDark,
                  modifier = Modifier.size(16.dp)
                )
              }

              Column {
                Text(
                  text = "Vitalith Copilot",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Grounded Synthesis",
                  fontSize = 10.sp,
                  color = TealNode,
                  fontWeight = FontWeight.Medium
                )
              }
            }

            Surface(
              color = VitalEmeraldContainer.copy(alpha = 0.4f),
              shape = RoundedCornerShape(6.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Verified,
                  contentDescription = null,
                  tint = VitalEmeraldSuccess,
                  modifier = Modifier.size(11.dp)
                )
                Text(
                  text = "Grounded",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  color = VitalEmeraldSuccess
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Body text with generous typography
          Text(
            text = message.text,
            fontSize = 14.sp,
            color = if (message.isRedFlag) VitalOnAlertContainer else MaterialTheme.colorScheme.onSurface,
            lineHeight = 22.sp
          )

          // Expandable Source Drawer
          if (message.citations.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { isSourceDrawerExpanded = !isSourceDrawerExpanded }
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "Source Clinical Records (${message.citations.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                  )
                  Icon(
                    imageVector = if (isSourceDrawerExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                  )
                }

                AnimatedVisibility(
                  visible = isSourceDrawerExpanded,
                  enter = fadeIn() + expandVertically(),
                  exit = fadeOut() + shrinkVertically()
                ) {
                  Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    message.citations.forEach { citation ->
                      Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                      ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                          Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                          ) {
                            Text(
                              text = citation.title,
                              fontSize = 11.sp,
                              fontWeight = FontWeight.Bold,
                              color = TealNode
                            )
                            Text(
                              text = citation.date,
                              fontSize = 10.sp,
                              color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                          }
                          Spacer(modifier = Modifier.height(2.dp))
                          Text(
                            text = "\"${citation.excerpt}\"",
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
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

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = message.timestamp,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.End)
          )
        }
      }
    }
  }
}
