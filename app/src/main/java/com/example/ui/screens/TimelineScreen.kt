package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.data.model.HealthTimelineItem
import com.example.data.model.TimelineCategory
import com.example.data.repository.VitalithRepository
import com.example.ui.components.ProvenanceBadge
import com.example.ui.theme.*

@Composable
fun TimelineScreen(
  repository: VitalithRepository,
  onNavigateToIngest: () -> Unit,
  modifier: Modifier = Modifier
) {
  val timelineItems by repository.timelineItems.collectAsState()
  var selectedCategory by remember { mutableStateOf(TimelineCategory.ALL) }
  var searchQuery by remember { mutableStateOf("") }
  var selectedItemForDetail by remember { mutableStateOf<HealthTimelineItem?>(null) }

  val filteredItems = remember(timelineItems, selectedCategory, searchQuery) {
    timelineItems.filter { item ->
      val matchesCategory = (selectedCategory == TimelineCategory.ALL || item.category == selectedCategory)
      val matchesSearch = searchQuery.isBlank() ||
        item.title.contains(searchQuery, ignoreCase = true) ||
        item.subtitle.contains(searchQuery, ignoreCase = true) ||
        item.clinicalNotes.contains(searchQuery, ignoreCase = true) ||
        item.tags.any { it.contains(searchQuery, ignoreCase = true) }
      matchesCategory && matchesSearch
    }
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
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "Longitudinal Timeline",
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Unified patient-owned record across providers & years",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          IconButton(
            onClick = onNavigateToIngest,
            modifier = Modifier.testTag("add_record_button")
          ) {
            Icon(
              imageVector = Icons.Default.AddCircle,
              contentDescription = "Add Record",
              tint = VitalTealPrimary,
              modifier = Modifier.size(28.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search labs, meds, conditions, encounters...", fontSize = 13.sp) },
          leadingIcon = {
            Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
          },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("timeline_search_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Category Filter Chips
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
          val categories = listOf(
            TimelineCategory.ALL,
            TimelineCategory.LAB,
            TimelineCategory.MEDICATION,
            TimelineCategory.ENCOUNTER,
            TimelineCategory.CONDITION,
            TimelineCategory.ALLERGY,
            TimelineCategory.IMMUNIZATION
          )
          items(categories) { cat ->
            FilterChip(
              selected = selectedCategory == cat,
              onClick = { selectedCategory = cat },
              label = {
                Text(
                  text = if (cat == TimelineCategory.ALL) "All (${timelineItems.size})" else cat.name,
                  fontSize = 11.sp,
                  fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = VitalTealPrimary,
                selectedLabelColor = Color.White
              ),
              modifier = Modifier.testTag("filter_${cat.name.lowercase()}")
            )
          }
        }
      }
    }

    // Timeline List
    if (filteredItems.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(32.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "No clinical records found",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
          )
          Text(
            text = "Try adjusting your search query or filter category.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        items(filteredItems, key = { it.id }) { item ->
          TimelineCard(
            item = item,
            onClick = { selectedItemForDetail = item }
          )
        }
      }
    }
  }

  // Detail Modal Dialog
  selectedItemForDetail?.let { item ->
    AlertDialog(
      onDismissRequest = { selectedItemForDetail = null },
      confirmButton = {
        Button(
          onClick = { selectedItemForDetail = null },
          colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary)
        ) {
          Text("Close")
        }
      },
      title = {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            CategoryChip(category = item.category)
            StatusBadge(status = item.status)
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(text = item.title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
          Text(text = item.date, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      },
      text = {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Provider / Facility: ${item.providerOrFacility}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
          )

          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text(text = "Clinical Interpretation & Notes", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              Spacer(modifier = Modifier.height(4.dp))
              Text(text = item.clinicalNotes, fontSize = 12.sp, lineHeight = 16.sp)
            }
          }

          if (item.keyValues.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(text = "Clinical Values & Ranges", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              item.keyValues.forEach { (k, v) ->
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(text = k, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Text(text = v, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
              }
            }
          }

          Divider()

          // Full Provenance
          Text(text = "Provenance & Audit Verification", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          ProvenanceBadge(provenance = item.provenance, showFullDetails = true)
        }
      }
    )
  }
}

@Composable
fun TimelineCard(
  item: HealthTimelineItem,
  onClick: () -> Unit
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(12.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
      .testTag("timeline_item_${item.id}")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          CategoryChip(category = item.category)
          Text(
            text = item.date,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        StatusBadge(status = item.status)
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = item.title,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = item.subtitle,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2
      )

      Spacer(modifier = Modifier.height(10.dp))

      ProvenanceBadge(provenance = item.provenance)
    }
  }
}
