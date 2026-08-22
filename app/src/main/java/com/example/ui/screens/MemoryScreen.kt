package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.model.BrainMemoryEntity
import com.example.data.model.MemoryType
import com.example.data.model.TaskEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MemoryScreen(
    memories: List<BrainMemoryEntity>,
    summary: String,
    tasks: List<TaskEntity> = emptyList(),
    onDeleteMemory: (BrainMemoryEntity) -> Unit,
    onClearAll: () -> Unit,
    onCompleteTask: (TaskEntity) -> Unit = {},
    onDeleteTask: (TaskEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalAppPalette.current
    val facts = memories.filter { it.memoryType == MemoryType.FACT || it.memoryType == MemoryType.PREFERENCE }
    val patterns = memories.filter { it.memoryType !in listOf(MemoryType.FACT, MemoryType.PREFERENCE, MemoryType.SELF_HEAL) }
    val heals = memories.filter { it.memoryType == MemoryType.SELF_HEAL }

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("memory_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Brain Memory", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = palette.accent)
                    Text("Everything Dhan-OM has learned about you", style = MaterialTheme.typography.bodySmall, color = palette.secondaryText)
                }
                if (memories.isNotEmpty()) {
                    TextButton(onClick = onClearAll) { Text("Clear all", color = MaterialTheme.colorScheme.error) }
                }
            }
        }

        // Graphify-style stats: how many dots (memories) and tasks the brain holds.
        item {
            Surface(shape = RoundedCornerShape(16.dp), color = palette.surface, border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)) {
                Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    StatChip("${memories.size}", "facts / dots")
                    StatChip("${tasks.count { it.isActive }}", "active tasks")
                    StatChip("${summary.lines().size}", "summary lines")
                }
            }
        }

        // Scheduled tasks the brain follows until they expire.
        item { Text("Tasks I'm Following", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.accent) }
        if (tasks.isEmpty()) {
            item {
                Text(
                    "No tasks yet. Say \"remind me to pay rent on the 1st every month\" and I'll keep following it.",
                    style = MaterialTheme.typography.bodySmall, color = palette.secondaryText
                )
            }
        } else {
            items(tasks, key = { "t${it.id}" }) { t -> TaskRow(t, onCompleteTask, onDeleteTask) }
        }

        if (summary.isNotBlank()) {
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = palette.primaryContainer) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Conversation Summary (lifetime, unbounded)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.onPrimaryContainer)
                        Spacer(Modifier.height(6.dp))
                        Text(summary, style = MaterialTheme.typography.bodySmall, color = palette.onPrimaryContainer)
                    }
                }
            }
        }

        if (facts.isNotEmpty()) {
            item { Text("Learned Facts & Preferences", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.accent) }
            items(facts, key = { it.id }) { m -> MemoryRow(m, onDeleteMemory) }
        }

        if (patterns.isNotEmpty()) {
            item { Text("Patterns & Habits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.accent) }
            items(patterns, key = { it.id }) { m -> MemoryRow(m, onDeleteMemory) }
        }

        if (heals.isNotEmpty()) {
            item { Text("Self-Heal History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.accent) }
            items(heals, key = { it.id }) { m -> MemoryRow(m, onDeleteMemory) }
        }

        if (memories.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Text("Nothing learned yet. Chat with Dhan-OM and it will start remembering your facts, preferences and habits.", style = MaterialTheme.typography.bodyMedium, color = palette.secondaryText)
                }
            }
        }
    }
}

@Composable
private fun StatChip(value: String, label: String) {
    val palette = LocalAppPalette.current
    Column {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.accent)
        Text(label, style = MaterialTheme.typography.labelSmall, color = palette.secondaryText)
    }
}

@Composable
private fun TaskRow(task: TaskEntity, onComplete: (TaskEntity) -> Unit, onDelete: (TaskEntity) -> Unit) {
    val palette = LocalAppPalette.current
    val fmt = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(34.dp).clip(androidx.compose.foundation.shape.CircleShape)
                    .background(if (task.isActive) palette.primaryContainer else palette.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (task.isActive) Icons.Default.Event else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (task.isActive) palette.primary else palette.secondaryText,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(task.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = palette.accent)
                Text(
                    buildString {
                        append(task.scheduleLabel())
                        if (task.amount > 0) append(" · ₹%,.0f".format(task.amount))
                        if (task.nextDueDateMillis > 0) append(" · due ${fmt.format(Date(task.nextDueDateMillis))}")
                        if (task.expiresAtMillis > 0) append(" · expires ${fmt.format(Date(task.expiresAtMillis))}")
                        if (task.timesDone > 0) append(" · done ${task.timesDone}×")
                    },
                    style = MaterialTheme.typography.bodySmall, color = palette.secondaryText
                )
            }
            if (task.isActive) {
                IconButton(onClick = { onComplete(task) }) {
                    Icon(Icons.Default.Check, contentDescription = "Mark done", tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                }
            }
            IconButton(onClick = { onDelete(task) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun MemoryRow(memory: BrainMemoryEntity, onDelete: (BrainMemoryEntity) -> Unit) {
    val palette = LocalAppPalette.current
    val fmt = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }
    val typeColor = when (memory.memoryType) {
        MemoryType.FACT -> Color(0xFF0B6BCB)
        MemoryType.PREFERENCE -> Color(0xFF0E9F6E)
        MemoryType.GOAL_STRATEGY -> Color(0xFFB45309)
        else -> Color(0xFF6750A4)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        border = BorderStroke(1.dp, palette.border)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(androidx.compose.foundation.shape.CircleShape).background(typeColor))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(memory.topic, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = palette.accent)
                Text(memory.description, style = MaterialTheme.typography.bodySmall, color = palette.secondaryText)
                Text("${(memory.confidenceScore * 100).toInt()}% conf · ${fmt.format(Date(memory.lastObservedAt))}", style = MaterialTheme.typography.labelSmall, color = palette.secondaryText)
            }
            IconButton(onClick = { onDelete(memory) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            }
        }
    }
}
