package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TealNode
import com.example.ui.theme.VitalTealPrimary

/**
 * Floating composer bar with suggestion chips and rounded pill input.
 */
@Composable
fun FloatingComposer(
  query: String,
  onQueryChange: (String) -> Unit,
  onSend: () -> Unit,
  suggestions: List<String>,
  onSelectSuggestion: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp)
  ) {
    // Animated suggestion chips
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      contentPadding = PaddingValues(bottom = 8.dp)
    ) {
      items(suggestions) { suggestion ->
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
          shadowElevation = 1.dp,
          modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onSelectSuggestion(suggestion) }
        ) {
          Text(
            text = suggestion,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
          )
        }
      }
    }

    // Floating Composer Pill
    Surface(
      shape = RoundedCornerShape(28.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      shadowElevation = 8.dp,
      modifier = Modifier
        .fillMaxWidth()
        .shadow(8.dp, shape = RoundedCornerShape(28.dp), spotColor = Color.Black.copy(alpha = 0.15f))
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        TextField(
          value = query,
          onValueChange = onQueryChange,
          placeholder = {
            Text(
              text = "Ask about labs, medications, encounters...",
              fontSize = 13.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
          },
          colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
          ),
          singleLine = true,
          modifier = Modifier
            .weight(1f)
            .testTag("ai_chat_input")
        )

        val isEnabled = query.isNotBlank()
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (isEnabled) VitalTealPrimary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = isEnabled, onClick = onSend)
            .testTag("send_ai_query_button"),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = "Send",
            tint = if (isEnabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}
